package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.DecomposingFungus;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardCollectableResource;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
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
public class DecomposingFungusActionValidator implements ActionValidator<DecomposingFungus> {
    public static final String ERROR_MESSAGE = "Decomposing fungus expects a card to remove an Animal/Microbe from";
    private final CardService cardService;

    @Override
    public Class<DecomposingFungus> getType() {
        return DecomposingFungus.class;
    }

    @Override
    public String validate(MarsGame game, Player player, Map<Integer, List<Integer>> inputParameters) {
        if (CollectionUtils.isEmpty(inputParameters)) {
            return ERROR_MESSAGE;
        }

        final List<Integer> cardToRemoveInput = inputParameters.get(InputFlag.CARD_CHOICE.getId());
        if (CollectionUtils.isEmpty(cardToRemoveInput) || cardToRemoveInput.size() != 1) {
            return ERROR_MESSAGE;
        }

        Integer cardToRemoveFrom = cardToRemoveInput.get(0);

        if (!player.getPlayed().containsCard(cardToRemoveFrom)) {
            return "Player doesn't have the selected project card.";
        }

        Card project = cardService.getCard(cardToRemoveFrom);
        CardCollectableResource collectableResourceType = project.getCollectableResource();

        if (collectableResourceType != CardCollectableResource.MICROBE && collectableResourceType != CardCollectableResource.ANIMAL) {
            return "You may only remove resource from an Animal/Microbe card";
        }

        if (player.getCardResourcesCount().get(project.getClass()) < 1) {
            return "The selected card doesn't have enough resources to perform current action";
        }

        return null;
    }
}
