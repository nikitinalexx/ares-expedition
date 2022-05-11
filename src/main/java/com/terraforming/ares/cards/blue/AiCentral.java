package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@RequiredArgsConstructor
@Getter
public class AiCentral implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public AiCentral(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Ai Central")
                .description("Requires 5 SCT. Action: Draw 2 cards.")
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public List<Tag> getTags() {
        return Arrays.asList(Tag.SCIENCE, Tag.BUILDING);
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }

    @Override
    public boolean isActiveCard() {
        return true;
    }

    @Override
    public int getPrice() {
        return 22;
    }

    @Override
    public List<Tag> getTagRequirements() {
        return IntStream.range(0, 5).mapToObj(i -> Tag.SCIENCE).collect(Collectors.toList());
    }

    @Override
    public int getWinningPoints() {
        return 2;
    }
}
