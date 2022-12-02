package com.terraforming.ares.services.ai;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.CardValidationService;
import com.terraforming.ares.services.ai.turnProcessors.AiBuildProjectService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 25.11.2022
 */
@Service
public class CardValueService {
    private final DeepNetwork deepNetwork;
    private final AiBuildProjectService aiBuildProjectService;
    private final CardService cardService;
    private final CardValidationService cardValidationService;

    public CardValueService(DeepNetwork deepNetwork, AiBuildProjectService aiBuildProjectService, CardService cardService, CardValidationService cardValidationService) {
        this.deepNetwork = deepNetwork;
        this.aiBuildProjectService = aiBuildProjectService;
        this.cardService = cardService;
        this.cardValidationService = cardValidationService;
    }

    public List<Integer> getCardsToDiscard(MarsGame game, Player player, List<Integer> cardsToDiscardFrom, int count) {
        game = new MarsGame(game);
        player = game.getPlayerByUuid(player.getUuid());

        List<Integer> cardsToDiscard = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int worstCardIndex = 0;
            float worstChance = 1.0f;

            for (int j = 0; j < cardsToDiscardFrom.size(); j++) {
                player.getHand().removeCard(cardsToDiscardFrom.get(j));
                float currentChance = deepNetwork.testState(game, player);

                if (currentChance >= worstChance) {
                    player.getHand().addCard(cardsToDiscardFrom.get(j));
                    Card card = cardService.getCard(cardsToDiscardFrom.get(j));
                    MarsGame projectedState = aiBuildProjectService.projectBuildCard(
                            game, player, card, ProjectionStrategy.FROM_PICK_PHASE
                    );

                    if (projectedState == null) {
                        if (cardValidationService.validateGlobalParameters(player, game, card.getId()) != null) {
                            worstCardIndex = j;
                            worstChance = 0;
                        }
                        continue;
                    }

                    currentChance = deepNetwork.testState(projectedState, projectedState.getPlayerByUuid(player.getUuid()));
                    if (currentChance < worstChance) {
                        worstCardIndex = j;
                        worstChance = currentChance;
                    }

                } else {
                    if (currentChance < worstChance) {
                        worstCardIndex = j;
                        worstChance = currentChance;
                    }
                    player.getHand().addCard(cardsToDiscardFrom.get(j));
                }
            }

            player.getHand().removeCard(cardsToDiscardFrom.get(worstCardIndex));
            cardsToDiscard.add(cardsToDiscardFrom.get(worstCardIndex));
            cardsToDiscardFrom.remove(worstCardIndex);
        }

        return cardsToDiscard;
    }

}
