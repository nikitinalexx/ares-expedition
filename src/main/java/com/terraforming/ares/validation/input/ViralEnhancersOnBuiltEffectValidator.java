package com.terraforming.ares.validation.input;

import com.terraforming.ares.cards.blue.BacterialAggregates;
import com.terraforming.ares.cards.blue.ViralEnhancers;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardResourceService;
import com.terraforming.ares.services.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.terraforming.ares.model.InputFlag.VIRAL_ENHANCERS_TAKE_PLANT;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
@RequiredArgsConstructor
public class ViralEnhancersOnBuiltEffectValidator implements OnBuiltEffectValidator<ViralEnhancers> {
    private final CardService cardService;
    private final CardResourceService cardResourceService;

    @Override
    public Class<ViralEnhancers> getType() {
        return ViralEnhancers.class;
    }

    @Override
    public String validate(Card card, Player player, Map<Integer, List<Integer>> input) {
        long tagsCount = cardService.countCardTags(card, Set.of(Tag.ANIMAL, Tag.MICROBE, Tag.PLANT), input);

        if (tagsCount == 0) {
            return null;
        }

        List<Integer> takePlantsInput = input.getOrDefault(VIRAL_ENHANCERS_TAKE_PLANT.getId(), List.of());
        List<Integer> microbeAnimalsInput = input.getOrDefault(InputFlag.VIRAL_ENHANCERS_PUT_RESOURCE.getId(), List.of());

        int plantsInput = CollectionUtils.isEmpty(takePlantsInput) ? 0 : takePlantsInput.get(0);
        if (plantsInput < 0) {
            return "Invalid number of plants selected";
        }

        int totalInputSize = plantsInput
                + (CollectionUtils.isEmpty(microbeAnimalsInput) ? 0 : microbeAnimalsInput.size());

        if (tagsCount != totalInputSize) {
            return "Input mismatch: when you play Animal/Microbe/Plant tag and have a Viral Enhancers project, " +
                    "you need to decide if you want to take a Plant or put an Animal/Microbe on another card. The number of tags doesn't match number of inputs";
        }

        if (!CollectionUtils.isEmpty(microbeAnimalsInput)) {
            for (Integer cardId : microbeAnimalsInput) {
                if (!player.getPlayed().containsCard(cardId) && card.getId() != cardId) {
                    return "Player doesn't have the selected card to add Animal/Microbe to";
                }

                Card projectCard = cardService.getCard(cardId);
                if (projectCard.getCollectableResource() != CardCollectableResource.ANIMAL
                        && projectCard.getCollectableResource() != CardCollectableResource.MICROBE) {
                    return "Selected card can not collect any animals or resources";
                }

                String resourceSubmissionMessage = cardResourceService.resourceSubmissionMessage(projectCard,player);
                if (resourceSubmissionMessage != null) {
                    return resourceSubmissionMessage;
                }
            }
        }

        return null;
    }
}
