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
public class Satellites implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public Satellites(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Satellites")
                .description("During the production phase, this produces 1 MC per Space you have, including this.")
                .cardAction(CardAction.MC_SPACE_INCOME)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public void onProjectBuiltEffect(CardService cardService, MarsGame game, Player player, Card project, Map<Integer, List<Integer>> inputParams) {
        int spaceTags = (int) project.getTags().stream().filter(Tag.SPACE::equals).count();

        player.setMcIncome(player.getMcIncome() + spaceTags);
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        int spaceTags = (int) marsContext.getPlayer()
                .getPlayed()
                .getCards().stream()
                .map(marsContext.getCardService()::getCard)
                .flatMap(card -> card.getTags().stream())
                .filter(Tag.SPACE::equals).count();

        marsContext.getPlayer().setMcIncome(marsContext.getPlayer().getMcIncome() + spaceTags + 1);

        return null;
    }

    @Override
    public boolean onBuiltEffectApplicableToOther() {
        return true;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SPACE);
    }

    @Override
    public int getPrice() {
        return 14;
    }
}
