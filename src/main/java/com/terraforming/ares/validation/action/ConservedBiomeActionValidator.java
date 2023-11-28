package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.BacterialAggregates;
import com.terraforming.ares.cards.blue.ConservedBiome;
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
 * Creation date 06.05.2022
 */
@Component
@RequiredArgsConstructor
public class ConservedBiomeActionValidator implements ActionValidator<ConservedBiome> {
    public static final String ERROR_MESSAGE = "Conserved biome expects a card to add an Animal/Microbe to";
    private final CardService cardService;
    private final CardResourceService cardResourceService;

    @Override
    public Class<ConservedBiome> getType() {
        return ConservedBiome.class;
    }

    @Override
    public String validate(MarsGame game, Player player, Map<Integer, List<Integer>> inputParameters) {
        if (CollectionUtils.isEmpty(inputParameters)) {
            return ERROR_MESSAGE;
        }

        final List<Integer> cardToAddInput = inputParameters.get(InputFlag.CARD_CHOICE.getId());
        if (CollectionUtils.isEmpty(cardToAddInput) || cardToAddInput.size() != 1) {
            return ERROR_MESSAGE;
        }

        Integer cardToAddTo = cardToAddInput.get(0);

        if (!player.getPlayed().containsCard(cardToAddTo)) {
            return "Player doesn't have the selected project card.";
        }

        Card project = cardService.getCard(cardToAddTo);
        CardCollectableResource collectableResourceType = project.getCollectableResource();

        if (collectableResourceType != CardCollectableResource.MICROBE && collectableResourceType != CardCollectableResource.ANIMAL) {
            return "You may only add resource to an Animal/Microbe card";
        }

        String resourceSubmissionMessage = cardResourceService.resourceSubmissionMessage(project,
                BacterialAggregates.class,
                player.getCardResourcesCount().get(BacterialAggregates.class),
                5);

        if (resourceSubmissionMessage != null) {
            return resourceSubmissionMessage;
        }

        return null;
    }
}
