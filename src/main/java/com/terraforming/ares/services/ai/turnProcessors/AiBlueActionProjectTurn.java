package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Deck;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.StandardProjectType;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.CardValidationService;
import com.terraforming.ares.services.StandardProjectService;
import com.terraforming.ares.services.ai.helpers.AiCardActionHelper;
import com.terraforming.ares.services.ai.helpers.AiPaymentService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Created by oleksii.nikitin
 * Creation date 23.11.2022
 */
@Component
public class AiBlueActionProjectTurn implements AiTurnProcessor {
    private final Random random = new Random();
    private final AiTurnService aiTurnService;
    private final CardService cardService;
    private final CardValidationService cardValidationService;
    private final AiPaymentService aiPaymentHelper;
    private final AiCardActionHelper aiCardActionHelper;
    private final StandardProjectService standardProjectService;

    public AiBlueActionProjectTurn(AiTurnService aiTurnService,
                                   CardService cardService,
                                   CardValidationService cardValidationService,
                                   AiPaymentService aiPaymentHelper, AiCardActionHelper aiCardActionHelper, StandardProjectService standardProjectService) {

        this.aiTurnService = aiTurnService;
        this.cardService = cardService;
        this.cardValidationService = cardValidationService;
        this.aiPaymentHelper = aiPaymentHelper;
        this.aiCardActionHelper = aiCardActionHelper;
        this.standardProjectService = standardProjectService;
    }

    @Override
    public TurnType getType() {
        return TurnType.PERFORM_BLUE_ACTION;
    }

    @Override
    public boolean processTurn(MarsGame game, Player player) {
        Deck activatedBlueCards = player.getActivatedBlueCards();

        List<Card> notUsedBlueCards = player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .filter(Card::isActiveCard)
                .filter(c -> !activatedBlueCards.containsCard(c.getId()))
                .collect(Collectors.toList());

        boolean result = processActionCards(game, player, notUsedBlueCards);
        if (result) {
            return result;
        }

        if (player.getChosenPhase() == 3 && !player.isActivatedBlueActionTwice()) {
            List<Card> actionBlueCards = player.getPlayed().getCards().stream()
                    .map(cardService::getCard)
                    .filter(Card::isActiveCard)
                    .collect(Collectors.toList());

            result = processActionCards(game, player, actionBlueCards);
        }

        if (!result) {
            if (!game.gameEndCondition() && canFinishGame(game, player)) {
                if (player.getHand().size() != 0) {
                    aiTurnService.sellAllCards(player, game, player.getHand().getCards());
                    return true;
                }
                if (game.getPlanet().temperatureLeft() > 0) {
                    aiTurnService.standardProjectTurn(game, player, StandardProjectType.TEMPERATURE);
                    System.out.println("Forcing Temperature");
                    return true;
                }
                if (game.getPlanet().oxygenLeft() > 0) {
                    aiTurnService.standardProjectTurn(game, player, StandardProjectType.FOREST);
                    System.out.println("Forcing Forest");
                    return true;
                }
                if (game.getPlanet().oceansLeft() > 0) {
                    aiTurnService.standardProjectTurn(game, player, StandardProjectType.OCEAN);
                    System.out.println("Forcing Ocean");
                    return true;
                }
            }

        }

        return result;
    }

    private boolean canFinishGame(MarsGame game, Player player) {
        int mc = player.getMc();
        mc += player.getHand().size() * 3;

        mc -= game.getPlanet().oceansLeft() * standardProjectService.getProjectPrice(player, StandardProjectType.OCEAN);

        if (mc < 0) {
            return false;
        }

        mc -= game.getPlanet().temperatureLeft() * standardProjectService.getProjectPrice(player, StandardProjectType.TEMPERATURE);

        if (mc < 0) {
            return false;
        }

        mc -= game.getPlanet().oxygenLeft() * standardProjectService.getProjectPrice(player, StandardProjectType.FOREST);

        return mc >= 0;
    }

    private boolean processActionCards(MarsGame game, Player player, List<Card> cards) {
        while (!cards.isEmpty()) {
            int selectedIndex = random.nextInt(cards.size());
            Card selectedCard = cards.get(selectedIndex);

            if (aiCardActionHelper.validateAction(game, player, selectedCard) == null) {
                aiTurnService.performBlueAction(
                        game,
                        player,
                        selectedCard.getId(),
                        aiCardActionHelper.getActionInputParams(game, player, selectedCard)
                );
                return true;
            } else {
                cards.remove(selectedIndex);
            }
        }
        return false;
    }

}
