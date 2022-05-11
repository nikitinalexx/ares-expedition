package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@RequiredArgsConstructor
@Getter
public class PhysicsComplex implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public PhysicsComplex(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Physics Complex")
                .description("Requires 4 Science tags. When you raise the temperature, add 1 science resource to this card. 1 VP per 2 science res on this card.")
                .cardAction(CardAction.PHYSICS_COMPLEX)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        marsContext.getPlayer().getCardResourcesCount().put(PhysicsComplex.class, 0);
        return null;
    }

    @Override
    public List<Tag> getTagRequirements() {
        return List.of(Tag.SCIENCE, Tag.SCIENCE, Tag.SCIENCE, Tag.SCIENCE);
    }

    @Override
    public CardCollectableResource getCollectableResource() {
        return CardCollectableResource.SCIENCE;
    }

    @Override
    public void onTemperatureChangedEffect(Player player) {
        player.getCardResourcesCount().put(PhysicsComplex.class, player.getCardResourcesCount().get(PhysicsComplex.class) + 1);
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
        return List.of(Tag.SCIENCE, Tag.BUILDING);
    }

    @Override
    public int getPrice() {
        return 5;
    }
}
