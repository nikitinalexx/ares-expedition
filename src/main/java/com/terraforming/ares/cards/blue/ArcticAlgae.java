package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.parameters.ParameterColor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@RequiredArgsConstructor
@Getter
public class ArcticAlgae implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public ArcticAlgae(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Arctic Algae")
                .description("Requires red temperature or warmer. When you flip an ocean tile, gain 4 plants.")
                .cardAction(CardAction.ARCTIC_ALGAE)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public void onOceanFlippedEffect(Player player) {
        player.setPlants(player.getPlants() + 4);
    }

    @Override
    public int getWinningPoints() {
        return 2;
    }

    @Override
    public List<Tag> getTags() {
        return Collections.singletonList(Tag.PLANT);
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }

    @Override
    public List<ParameterColor> getTemperatureRequirement() {
        return Arrays.asList(ParameterColor.R, ParameterColor.Y, ParameterColor.W);
    }

    @Override
    public boolean isActiveCard() {
        return false;
    }

    @Override
    public int getPrice() {
        return 19;
    }
}
