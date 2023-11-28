package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.BacterialAggregates;
import com.terraforming.ares.cards.blue.SymbioticFungus;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardCollectableResource;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardResourceService;
import com.terraforming.ares.services.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
@RequiredArgsConstructor
public class SymbioticFungudActionValidator implements ActionValidator<SymbioticFungus> {
    public static final String ERROR_MESSAGE = "SymbioticFungud requires a card to put a microbe on";
    private final CardService cardService;
    private final CardResourceService cardResourceService;

    @Override
    public Class<SymbioticFungus> getType() {
        return SymbioticFungus.class;
    }

    @Override
    public String validate(MarsGame game, Player player, Map<Integer, List<Integer>> inputParameters) {
        if (CollectionUtils.isEmpty(inputParameters)) {
            return ERROR_MESSAGE;
        }

        final List<Integer> selectedCardInput = inputParameters.get(InputFlag.CARD_CHOICE.getId());
        if (CollectionUtils.isEmpty(selectedCardInput)) {
            return ERROR_MESSAGE;
        }

        Integer selectedCardId = selectedCardInput.get(0);

        if (!player.getPlayed().containsCard(selectedCardId)) {
            return "SymbioticFungud - player doesn't have the selected card";
        }

        Card card = cardService.getCard(selectedCardId);
        if (card.getCollectableResource() != CardCollectableResource.MICROBE) {
            return "SymbioticFungud may only place a microbe on a microbe collecting card";
        }

        String resourceSubmissionMessage = cardResourceService.resourceSubmissionMessage(card,
                BacterialAggregates.class,
                player.getCardResourcesCount().get(BacterialAggregates.class),
                5);

        if (resourceSubmissionMessage != null) {
            return resourceSubmissionMessage;
        }

        return null;
    }
}
