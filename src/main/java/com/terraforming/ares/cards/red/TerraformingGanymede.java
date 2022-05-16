package com.terraforming.ares.cards.red;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class TerraformingGanymede implements BaseExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public TerraformingGanymede(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Terraforming Ganymede")
                .description("Raise your TR 1 step per Jupiter tag you have, including this.")
                .cardAction(CardAction.TERRAFORMING_GANYMEDE)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        int jupiterTags = (int) player.getPlayed()
                .getCards()
                .stream()
                .map(marsContext.getCardService()::getCard)
                .flatMap(projectCard -> projectCard.getTags().stream())
                .filter(Tag.JUPITER::equals).count();

        player.setTerraformingRating(player.getTerraformingRating() + jupiterTags + 1);

        return null;
    }

    @Override
    public int getWinningPoints() {
        return 2;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SPACE, Tag.JUPITER, Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 28;
    }

}
