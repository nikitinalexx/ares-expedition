package com.terraforming.ares.cards.blue;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@RequiredArgsConstructor
@Getter
public class AnaerobicMicroorganisms implements BlueCard {
    private final int id;

    @Override
    public CardCollectableResource getCollectableResource() {
        return CardCollectableResource.MICROBE;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        marsContext.getPlayer().getCardResourcesCount().put(AnaerobicMicroorganisms.class, 1);
        return null;
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
    public void onOtherProjectBuiltEffect(CardService cardService, MarsGame marsGame, Player player, ProjectCard project, Map<Integer, List<Integer>> inputParams) {
        int affectedTagsCount = (int) project.getTags().stream().filter(tag ->
                tag == Tag.ANIMAL || tag == Tag.MICROBE || tag == Tag.PLANT
        ).count();

        player.getCardResourcesCount().compute(
                AnaerobicMicroorganisms.class, (key, value) -> {
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
