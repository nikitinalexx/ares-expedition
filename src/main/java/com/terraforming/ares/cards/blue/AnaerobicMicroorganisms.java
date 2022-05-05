package com.terraforming.ares.cards.blue;

import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.PlayerContext;
import com.terraforming.ares.model.ProjectCard;
import com.terraforming.ares.model.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@RequiredArgsConstructor
@Getter
public class AnaerobicMicroorganisms implements BlueCard {
    private final int id;

    @Override
    public void buildProject(PlayerContext playerContext) {
        playerContext.getCardIdToResourcesCount().put(id, 1);
    }

    @Override
    public String description() {
        return "When you play a card, you may remove 2 microbes from this card to pay 10 MC less for that card.\n"
                + "When you play an Animal, Microbe, or Plant, including this, add a microbe to this card.\n";
    }

    @Override
    public List<Tag> getTags() {
        return Collections.singletonList(Tag.MICROBE);
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }

    @Override
    public void onProjectBuiltEffect(PlayerContext player, ProjectCard project) {
        int affectedTagsCount = (int) project.getTags().stream().filter(tag ->
                tag == Tag.ANIMAL || tag == Tag.MICROBE || tag == Tag.PLANT
        ).count();

        player.getCardIdToResourcesCount().compute(
                id, (key, value) -> {
                    if (value == null) {
                        value = 0;
                    }
                    return value + affectedTagsCount;
                }
        );
    }

    @Override
    public boolean isActiveCard() {
        return false;
    }

    @Override
    public int getPrice() {
        return 10;
    }
}
