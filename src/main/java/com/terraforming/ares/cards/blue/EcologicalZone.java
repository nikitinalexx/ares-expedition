package com.terraforming.ares.cards.blue;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@RequiredArgsConstructor
@Getter
public class EcologicalZone implements BlueCard {
    private final int id;

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        marsContext.getPlayer().getCardResourcesCount().put(EcologicalZone.class, 2);
        return null;
    }

    @Override
    public CardCollectableResource getCollectableResource() {
        return CardCollectableResource.ANIMAL;
    }

    @Override
    public String description() {
        return "1 VP per 2 animals on this card. When you play a Animal or Plant, including these, add an animal to this card.";
    }

    @Override
    public void onOtherProjectBuiltEffect(CardService cardService, MarsGame game, Player player, ProjectCard card, Map<Integer, List<Integer>> inputParams) {
        long animalsToAddCount = card.getTags().stream().filter(tag -> tag == Tag.ANIMAL || tag == Tag.PLANT).count();

        if (animalsToAddCount == 0) {
            return;
        }

        player.getCardResourcesCount().put(EcologicalZone.class, player.getCardResourcesCount().get(EcologicalZone.class) + (int) animalsToAddCount);
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
        return List.of(Tag.PLANT, Tag.ANIMAL);
    }

    @Override
    public int getPrice() {
        return 11;
    }
}
