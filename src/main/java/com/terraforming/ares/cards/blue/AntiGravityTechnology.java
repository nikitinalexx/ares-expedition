package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@RequiredArgsConstructor
@Getter
public class AntiGravityTechnology implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public AntiGravityTechnology(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Anti-Gravity Technology")
                .description("Requires 5 SCT. When you play a card, gain 2 heat and 2 plants.")
                .cardAction(CardAction.ANTI_GRAVITY_TECH)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public void onProjectBuiltEffect(CardService cardService, MarsGame marsGame, Player player, Card project, Map<Integer, List<Integer>> inputParams) {
        player.setHeat(player.getHeat() + 2);
        player.setPlants(player.getPlants() + 2);
    }

    @Override
    public boolean onBuiltEffectApplicableToOther() {
        return true;
    }

    @Override
    public List<Tag> getTagRequirements() {
        return IntStream.range(0, 5).mapToObj(i -> Tag.SCIENCE).collect(Collectors.toList());
    }

    @Override
    public int getWinningPoints() {
        return 3;
    }

    @Override
    public List<Tag> getTags() {
        return Collections.singletonList(Tag.SCIENCE);
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
    public int getPrice() {
        return 18;
    }
}
