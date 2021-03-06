package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class AtmosphericInsulators implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public AtmosphericInsulators(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Atmospheric Insulators")
                .description("During the production phase, this produces 1 heat per Earth you have, including this.")
                .cardAction(CardAction.HEAT_EARTH_INCOME)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public void postProjectBuiltEffect(CardService cardService, MarsGame game, Player player, Card project, Map<Integer, List<Integer>> inputParams) {
        int earthTags = (int) project.getTags().stream().filter(Tag.EARTH::equals).count();

        player.setHeatIncome(player.getHeatIncome() + earthTags);
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        int earthTagCount = (int) marsContext.getPlayer()
                .getPlayed()
                .getCards().stream()
                .map(marsContext.getCardService()::getCard)
                .flatMap(card -> card.getTags().stream())
                .filter(Tag.EARTH::equals).count();

        marsContext.getPlayer().setHeatIncome(marsContext.getPlayer().getHeatIncome() + earthTagCount + 1);

        return null;
    }

    @Override
    public boolean onBuiltEffectApplicableToOther() {
        return true;
    }

    @Override
    public void revertPlayedTags(CardService cardService, List<Tag> tags, Player player) {
        int earthTagCount = (int) tags.stream().filter(Tag.EARTH::equals).count();
        player.setHeatIncome(player.getHeatIncome() - earthTagCount);
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SPACE, Tag.EARTH);
    }

    @Override
    public int getPrice() {
        return 10;
    }
}
