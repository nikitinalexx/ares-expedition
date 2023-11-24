package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.request.ChooseCorporationRequest;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.AiDiscoveryDecisionService;
import com.terraforming.ares.services.ai.AiPickCardProjectionService;
import com.terraforming.ares.services.ai.DeepNetwork;
import com.terraforming.ares.services.ai.TestAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Created by oleksii.nikitin
 * Creation date 23.11.2022
 */
@Component
@RequiredArgsConstructor
public class AiPickCorporationTurn implements AiTurnProcessor {
    private final Random random = new Random();
    private final AiTurnService aiTurnService;
    private final CardService cardService;
    private final AiDiscoveryDecisionService aiDiscoveryDecisionService;
    private final TestAiService testAiService;
    private final DeepNetwork deepNetwork;
    private final AiPickCardProjectionService aiPickCardProjectionService;


    @Override
    public TurnType getType() {
        return TurnType.PICK_CORPORATION;
    }

    @Override
    public boolean processTurn(MarsGame game, Player player) {
        List<Integer> corporations = player.getCorporations().getCards();
        int selectedCorporationId = corporations.get(0);

        switch (player.getDifficulty().EXPERIMENTAL_TURN) {
            case REGULAR:
                selectedCorporationId = corporations.get(random.nextInt(corporations.size()));
                break;
            case EXPERIMENT:
                MarsGame gameAfterOpponentPlay = testAiService.projectOpponentCorporationBuildExperiment(game, player);

                List<Integer> playerCards = player.getHand().getCards();

                float bestTotalExtra = 0;

                Map<String, Float> corporationToExtraChance = new HashMap<>();

                for (Integer corporation : player.getCorporations().getCards()) {
                    MarsGame projectionAfterCorporation = testAiService.projectPlayerBuildCorporationExperiment(gameAfterOpponentPlay, player, corporation);

                    float initialChance = deepNetwork.testState(projectionAfterCorporation, projectionAfterCorporation.getPlayerByUuid(player.getUuid()));

                    float totalExtra = 0;

                    for (int i = 0; i < playerCards.size(); i++) {
                        MarsGame gameCopy = new MarsGame(projectionAfterCorporation);
                        totalExtra += aiPickCardProjectionService.cardExtraChanceIfBuilt(gameCopy, gameCopy.getPlayerByUuid(player.getUuid()), playerCards.get(i), initialChance);
                    }

                    corporationToExtraChance.put(cardService.getCard(corporation).getClass().getSimpleName(), (totalExtra / playerCards.size()) + initialChance);

                    if (totalExtra > bestTotalExtra) {
                        bestTotalExtra = totalExtra;
                        selectedCorporationId = corporation;
                    }


                }

                break;
        }


        Card corporationCard = cardService.getCard(selectedCorporationId);
        CardAction corporationCardAction = corporationCard.getCardMetadata().getCardAction();

        aiTurnService.chooseCorporationTurn(game, ChooseCorporationRequest.builder()
                .playerUuid(player.getUuid())
                .corporationId(selectedCorporationId)
                .inputParams(aiDiscoveryDecisionService.getCorporationInput(game, player, corporationCardAction))
                .build());

        return true;
    }



}
