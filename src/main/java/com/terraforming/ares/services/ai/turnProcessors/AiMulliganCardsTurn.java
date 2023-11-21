package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.processors.turn.PickCorporationProcessor;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.MarsContextProvider;
import com.terraforming.ares.services.ai.AiBalanceService;
import com.terraforming.ares.services.ai.AiDiscoveryDecisionService;
import com.terraforming.ares.services.ai.AiPickCardProjectionService;
import com.terraforming.ares.services.ai.ICardValueService;
import com.terraforming.ares.services.ai.dto.CardValueResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Created by oleksii.nikitin
 * Creation date 23.11.2022
 */
@Component
@RequiredArgsConstructor
public class AiMulliganCardsTurn implements AiTurnProcessor {
    private final AiTurnService aiTurnService;
    private final ICardValueService cardValueService;
    private final AiBalanceService aiBalanceService;
    private final PickCorporationProcessor pickCorporationProcessor;
    private final AiPickCardProjectionService aiPickCardProjectionService;
    private final CardService cardService;
    private final AiDiscoveryDecisionService aiDiscoveryDecisionService;
    private final MarsContextProvider marsContextProvider;

    private final Random random = new Random();


    @Override
    public TurnType getType() {
        return TurnType.MULLIGAN;
    }

    @Override
    public boolean processTurn(MarsGame game, Player player) {
        List<Integer> cardsToDiscardSmart = getCardsToDiscardSmart(game, player, player.getHand().getCards().size());
        aiTurnService.mulliganCards(game, player, cardsToDiscardSmart);
        return true;
    }

    public List<Integer> getCardsToDiscardSmart(MarsGame game, Player player, int max) {
        List<Integer> cards = new ArrayList<>(player.getHand().getCards());

        List<Integer> cardsToDiscard = new ArrayList<>();

        switch (player.getDifficulty().CARDS_PICK) {
            case RANDOM:
                int toDiscard = random.nextInt(cards.size());
                cardsToDiscard.addAll(cards.subList(0, toDiscard));
                break;
            case FILE_VALUE:
                while (cardsToDiscard.size() != max) {
                    if (cards.isEmpty()) {
                        break;
                    }
                    CardValueResponse cardValueResponse = cardValueService.getWorstCard(game, player, cards, game.getTurns());

                    if (aiBalanceService.isCardWorthToDiscard(player, cardValueResponse.getWorth())) {
                        cardsToDiscard.add(cardValueResponse.getCardId());
                        cards.remove(cardValueResponse.getCardId());
                    } else {
                        break;
                    }

                }
                break;
            case NETWORK_PROJECTION:
                MarsGame projectCorporationBuild = projectCorporationBuild(game);

                Map<Integer, Float> cardToChance = new HashMap<>();

                for (int i = 0; i < cards.size(); i++) {
                    MarsGame gameCopy = new MarsGame(projectCorporationBuild);
                    cardToChance.put(cards.get(i), aiPickCardProjectionService.cardExtraChanceIfBuilt(gameCopy, gameCopy.getPlayerByUuid(player.getUuid()), cards.get(i)));
                }

                List<Integer> cardsByChance = new ArrayList<>(cards);
                cardsByChance.sort(Comparator.comparingDouble(cardToChance::get));

                for (int i = 0; i < cardsByChance.size(); i++) {
                    float chance = cardToChance.get(cardsByChance.get(i));

                    if (chance < 0.01f) {
                        cardsToDiscard.add(cardsByChance.get(i));
                    } else {
                        break;
                    }
                }
                break;
        }

        return cardsToDiscard;
    }

    private MarsGame projectCorporationBuild(MarsGame game) {
        game = new MarsGame(game);

        for (Player player : game.getPlayerUuidToPlayer().values()) {
            projectPlayerBuildCorporation(game, player);
        }

        return game;
    }

    private void projectPlayerBuildCorporation(MarsGame game, Player player) {
        LinkedList<Integer> corporations = player.getCorporations().getCards();
        int selectedCorporationId = corporations.stream().min(Comparator.comparingInt(Constants.CORPORATION_PRIORITY::indexOf)).orElseThrow();

        player.setSelectedCorporationCard(selectedCorporationId);
        player.setMulligan(false);
        player.getPlayed().addCard(selectedCorporationId);

        Card card = cardService.getCard(selectedCorporationId);
        final MarsContext marsContext = marsContextProvider.provide(game, player);
        card.buildProject(marsContext);
        if (card.onBuiltEffectApplicableToItself()) {
            card.postProjectBuiltEffect(marsContext, card, aiDiscoveryDecisionService.getCorporationInput(game, player, card.getCardMetadata().getCardAction()));
        }
    }

}
