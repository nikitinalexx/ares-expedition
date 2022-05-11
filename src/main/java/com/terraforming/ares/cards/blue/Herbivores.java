package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.function.LongPredicate;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@RequiredArgsConstructor
@Getter
public class Herbivores implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public Herbivores(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Herbivores")
                .description("Requires 5 oceans to be flipped. When you raise oxygen, flip an ocean tile, or raise temperature, add 1 animal to this card. 1 VP per 2 animals on this card.")
                .build();
    }

    @Override
    public int getWinningPoints() {
        //TODO
        return 0;
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }

    @Override
    public LongPredicate getOceanRequirement() {
        return numberOfFlippedOceans -> numberOfFlippedOceans >= 5;
    }

    @Override
    public boolean isActiveCard() {
        return false;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        marsContext.getPlayer().getCardResourcesCount().put(Herbivores.class, 0);
        return null;
    }

    @Override
    public CardCollectableResource getCollectableResource() {
        return CardCollectableResource.ANIMAL;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.ANIMAL);
    }

    @Override
    public void onOxygenChangedEffect(Player player) {
        addAnimal(player);
    }

    @Override
    public void onTemperatureChangedEffect(Player player) {
        addAnimal(player);
    }

    @Override
    public void onOceanFlippedEffect(Player player) {
        addAnimal(player);
    }

    private void addAnimal(Player player) {
        player.getCardResourcesCount().put(Herbivores.class, player.getCardResourcesCount().get(Herbivores.class) + 1);
    }

    @Override
    public int getPrice() {
        return 25;
    }
}
