package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.cards.green.DiscoveryExpansionBlueCard;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardResourceService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@RequiredArgsConstructor
@Getter
public class BacterialAggregates implements DiscoveryExpansionBlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public BacterialAggregates(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Bacterial Aggregates")
                .description("When you play an Earth tag, put a microbe on this card (max 5). Draw an additional card in 5th phase for each microbe on this card.")
                .cardAction(CardAction.BACTERIAL_AGGREGATES)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        marsContext.getPlayer().initResources(this);

        return null;
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card project, Map<Integer, List<Integer>> inputParams) {
        int tagsCount = marsContext.getCardService().countCardTags(project, Set.of(Tag.EARTH), inputParams);

        if (tagsCount == 0) {
            return;
        }

        final Player player = marsContext.getPlayer();

//        player.getCardResourcesCount().put(this.getClass(), Math.min(player.getCardResourcesCount().get(this.getClass()) + tagsCount, 5));
        marsContext.getCardResourceService().addResources(player, this, Math.min(player.getCardResourcesCount().get(this.getClass()) + tagsCount, 5));
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
    public Set<SpecialEffect> getSpecialEffects() {
        return Set.of(SpecialEffect.BACTERIAL_AGGREGATES);
    }

    @Override
    public int getWinningPoints() {
        return 1;
    }

    @Override
    public CardCollectableResource getCollectableResource() {
        return CardCollectableResource.MICROBE;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.MICROBE, Tag.EARTH);
    }

    @Override
    public int getPrice() {
        return 7;
    }


}
