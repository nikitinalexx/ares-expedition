package com.terraforming.ares.services.dataset;

import com.terraforming.ares.cards.blue.*;
import com.terraforming.ares.cards.buffedCorporations.BuffedArclightCorporation;
import com.terraforming.ares.cards.corporations.ArclightCorporation;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.CardValidationService;
import com.terraforming.ares.services.DraftCardsService;
import com.terraforming.ares.services.WinPointsService;
import com.terraforming.ares.services.ai.helpers.AiCardBuildParamsHelper;
import com.terraforming.ares.services.ai.helpers.AiPaymentService;
import com.terraforming.ares.services.ai.turnProcessors.AiBuildProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by oleksii.nikitin
 * Creation date 30.11.2022
 */
@Service
@RequiredArgsConstructor
public class DatasetCollectService {
    private final WinPointsService winPointsService;
    private final DraftCardsService draftCardsService;
    private final CardService cardService;
    private final CardValidationService cardValidationService;
    private final AiPaymentService aiPaymentService;
    private final AiCardBuildParamsHelper aiCardBuildParamsHelper;

    private static Map<Class<?>, Integer> RESOURCE_VP_COUNT;
    private static Map<Class<?>, Integer> RESOURCE_INTRINSIC_VP_COUNT;

    static {
        RESOURCE_VP_COUNT = new HashMap<>();
        //microbes
        RESOURCE_VP_COUNT.put(Tardigrades.class, 3);             // 1/3 vp
        //animals
        RESOURCE_VP_COUNT.put(FilterFeeders.class, 3);// 1/3vp
        RESOURCE_VP_COUNT.put(ArclightCorporation.class, 2);// 1/2vp
        RESOURCE_VP_COUNT.put(BuffedArclightCorporation.class, 2);// 1/2vp
        RESOURCE_VP_COUNT.put(EcologicalZone.class, 2);// 1/2vp
        RESOURCE_VP_COUNT.put(SmallAnimals.class, 2);// 1/2vp
        RESOURCE_VP_COUNT.put(Herbivores.class, 2);// 1/2vp
        RESOURCE_VP_COUNT.put(PhysicsComplex.class, 2);// 1/2vp

        RESOURCE_INTRINSIC_VP_COUNT = new HashMap<>();
        RESOURCE_INTRINSIC_VP_COUNT.put(Decomposers.class, 1);              // 1/3 vp
        RESOURCE_INTRINSIC_VP_COUNT.put(GhgProductionBacteria.class, 1);// 1/3 vp
        RESOURCE_INTRINSIC_VP_COUNT.put(NitriteReductingBacteria.class, 1);// 1/3 vp
        RESOURCE_INTRINSIC_VP_COUNT.put(RegolithEaters.class, 1);// 1/3 vp
        RESOURCE_INTRINSIC_VP_COUNT.put(AnaerobicMicroorganisms.class, 2);// 1/2 vp
        RESOURCE_INTRINSIC_VP_COUNT.put(DecomposingFungus.class, 2); // 1/2 vp
        RESOURCE_INTRINSIC_VP_COUNT.put(SelfReplicatingBacteria.class, 2);// 1/2 vp
    }


    public void collect(MarsGame marsGame, MarsGameDataset marsGameDataset) {
        for (int i = 0; i < 2; i++) {
            Player currentPlayer = marsGame.getPlayerByUuid(marsGameDataset.getPlayers().get(i));
            Player anotherPlayer = marsGame.getPlayerByUuid(marsGameDataset.getPlayers().get(i == 0 ? 1 : 0));
            marsGameDataset.getPlayerToRows().get(currentPlayer.getUuid()).add(
                    collectPlayerDataWithoutTags(marsGame, currentPlayer, anotherPlayer)
            );
        }
    }

    public MarsGameRow collectPlayerDataWithoutTags(MarsGame game, Player currentPlayer, Player anotherPlayer) {
        return MarsGameRow.builder()
                .turn(game.getTurns())
                .winPoints(winPointsService.countWinPoints(currentPlayer, game))
                .mcIncome(currentPlayer.getMcIncome())
                .mc(currentPlayer.getMc())
                .steelIncome(currentPlayer.getSteelIncome())
                .titaniumIncome(currentPlayer.getTitaniumIncome())
                .plantsIncome(currentPlayer.getPlantsIncome())
                .plants(currentPlayer.getPlants())
                .heatIncome(currentPlayer.getHeatIncome())
                .heat(currentPlayer.getHeat())
                .cardsIncome(currentPlayer.getCardIncome())
                .cardsInHand(currentPlayer.getHand().size())
                .cardsBuilt(currentPlayer.getPlayed().size() - 1)
                .oxygenLevel(game.getPlanet().getOxygenValue())
                .temperatureLevel(game.getPlanet().getTemperatureValue())
                .oceansLevel(Constants.MAX_OCEANS - game.getPlanet().oceansLeft())
                .opponentWinPoints(winPointsService.countWinPoints(anotherPlayer, game))
                .opponentMcIncome(anotherPlayer.getMcIncome())
                .opponentMc(anotherPlayer.getMc())
                .opponentSteelIncome(anotherPlayer.getSteelIncome())
                .opponentTitaniumIncome(anotherPlayer.getTitaniumIncome())
                .opponentPlantsIncome(anotherPlayer.getPlantsIncome())
                .opponentPlants(anotherPlayer.getPlants())
                .opponentHeatIncome(anotherPlayer.getHeatIncome())
                .opponentHeat(anotherPlayer.getHeat())
                .opponentCardsIncome(anotherPlayer.getCardIncome())
                .opponentCardsBuilt(anotherPlayer.getPlayed().size() - 1)
                .extraCardsToTake(draftCardsService.countExtraCardsToTake(currentPlayer))
                .extraCardsToSee(draftCardsService.countExtraCardsToDraft(currentPlayer))
                .resourceCount(getTotalResourceCount(currentPlayer.getCardResourcesCount()))
                .cards(collectSpecificBlueCards(currentPlayer))
                .build();
    }


    private float getTotalResourceCount(Map<Class<?>, Integer> cardResourcesCount) {
        int halves = 0;
        int thirds = 0;

        for (Map.Entry<Class<?>, Integer> entry : cardResourcesCount.entrySet()) {
            if (entry.getValue() > 0) {
                Integer div = RESOURCE_VP_COUNT.getOrDefault(entry.getKey(), 0);
                if (div != 0 && entry.getValue() % div != 0) {
                    if (div == 2) {
                        halves += entry.getValue() % div;
                    } else {
                        thirds += entry.getValue() % div;
                    }
                } else if (div == 0) {
                    div = RESOURCE_INTRINSIC_VP_COUNT.get(entry.getKey());
                    if (div != null) {
                        if (div == 2) {
                            halves += div * entry.getValue();
                        } else {
                            thirds += div * entry.getValue();
                        }
                    }
                }
            }
        }

        return (float) halves / 2 + (float) thirds / 3;
    }

    private static Map<Tag, Long> getPlayerTagsCount(CardService cardService, Player player) {
        return player.getPlayed().getCards()
                .stream()
                .map(cardService::getCard)
                .flatMap(card -> card.getTags().stream())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    private static final List<Integer> cardsToLog = List.of(
            1, 2, 5, 6, 8, 17, 19, 23, 24, 25, 30, 33, 37, 39, 40, 42, 44, 45, 46, 48, 51, 52, 53, 55, 61, 217
    );

    private static List<Integer> collectSpecificBlueCards(Player player) {
        return cardsToLog.stream()
                .flatMap(cardId -> Stream.of(
                        player.getHand().containsCard(cardId) ? 1 : 0,
                        player.getPlayed().containsCard(cardId) ? 1 : 0
                ))
                .collect(Collectors.toList());
    }
}
