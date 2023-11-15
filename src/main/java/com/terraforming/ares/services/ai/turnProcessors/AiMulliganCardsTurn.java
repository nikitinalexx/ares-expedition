package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.dataset.CardsAiService;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.ai.AiCardsChoice;
import com.terraforming.ares.model.ai.AiTurnChoice;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.processors.turn.PickCorporationProcessor;
import com.terraforming.ares.services.ai.AiBalanceService;
import com.terraforming.ares.services.ai.ICardValueService;
import com.terraforming.ares.services.ai.dto.CardValueResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
    private final CardsAiService cardsAiService;
    private final PickCorporationProcessor pickCorporationProcessor;
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

        if (player.isFirstBot() && Constants.CARDS_PICK_PLAYER_1 == AiCardsChoice.RANDOM || player.isSecondBot() && Constants.CARDS_PICK_PLAYER_2 == AiCardsChoice.RANDOM) {
            int toDiscard = random.nextInt(cards.size());
            cardsToDiscard.addAll(cards.subList(0, toDiscard));
        } else {
            if (player.isFirstBot() && Constants.CARDS_PICK_PLAYER_1 == AiCardsChoice.NETWORK || player.isSecondBot() && Constants.CARDS_PICK_PLAYER_2 == AiCardsChoice.NETWORK) {

                float average = 0;
                for (Integer card : cards) {
                    average += cardsAiService.getCardChance(game, player.getUuid(), card);
                }
                average /= 8;
                average *= 1.04f;

                while (cardsToDiscard.size() != max) {
                    if (cards.isEmpty()) {
                        break;
                    }

                    Integer cardToRemove = cardsAiService.getWorstCard(game, player.getUuid(), cards, average);//TODO if the worst card is still good, maybe don't discard it?
                    if (cardToRemove != null) {
                        cardsToDiscard.add(cardToRemove);
                        cards.remove(cardToRemove);
                    } else {
                        break;
                    }
                }
            }
            if (player.isFirstBot() && Constants.CARDS_PICK_PLAYER_1 == AiCardsChoice.FILE_VALUE || player.isSecondBot() && Constants.CARDS_PICK_PLAYER_2 == AiCardsChoice.FILE_VALUE) {
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
            }

        }


        return cardsToDiscard;
    }

}
