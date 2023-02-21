package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.terraforming.ares.model.InputFlag.VIRAL_ENHANCERS_TAKE_PLANT;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@RequiredArgsConstructor
@Getter
public class ViralEnhancers implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public ViralEnhancers(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Viral Enhancers")
                .description("When you play a Plant, Microbe, or Animal tags, including these, gain 1 plant or add  1 animal or microbe to ANOTHER* card.")
                .cardAction(CardAction.VIRAL_ENHANCERS)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card project, Map<Integer, List<Integer>> inputParams) {
        long tagsCount = marsContext.getCardService().countCardTags(project, Set.of(Tag.ANIMAL, Tag.MICROBE, Tag.PLANT), inputParams);

        if (tagsCount == 0) {
            return;
        }

        List<Integer> takePlantsInput = inputParams.getOrDefault(VIRAL_ENHANCERS_TAKE_PLANT.getId(), List.of());
        List<Integer> microbeAnimalsInput = inputParams.getOrDefault(InputFlag.VIRAL_ENHANCERS_PUT_RESOURCE.getId(), List.of());

        final Player player = marsContext.getPlayer();

        if (!CollectionUtils.isEmpty(takePlantsInput)) {
            player.setPlants(player.getPlants() + takePlantsInput.get(0));
        }

        if (!CollectionUtils.isEmpty(microbeAnimalsInput)) {
            for (Integer cardId : microbeAnimalsInput) {
                Card projectCard = marsContext.getCardService().getCard(cardId);
                player.addResources(projectCard, 1);
            }
        }
    }

    @Override
    public boolean onBuiltEffectApplicableToOther() {
        return true;
    }

    @Override
    public boolean onBuiltEffectApplicableToItself() {
        return true;
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }

    @Override
    public boolean isActiveCard() {
        return false;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.MICROBE, Tag.PLANT);
    }

    @Override
    public int getPrice() {
        return 8;
    }
}
