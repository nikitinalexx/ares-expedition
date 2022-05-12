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
public class MirandaResort implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public MirandaResort(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Miranda Resort")
                .description("During the production phase, this produces 1 MC per Earth tag you have, including this.")
                .cardAction(CardAction.MC_EARTH_INCOME)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public void onProjectBuiltEffect(CardService cardService, MarsGame game, Player player, ProjectCard project, Map<Integer, List<Integer>> inputParams) {
        int earthTags = (int) project.getTags().stream().filter(Tag.EARTH::equals).count();

        player.setMcIncome(player.getMcIncome() + earthTags);
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        int earthTagCount = (int) marsContext.getPlayer()
                .getPlayed()
                .getCards().stream()
                .map(marsContext.getCardService()::getProjectCard)
                .flatMap(card -> card.getTags().stream())
                .filter(Tag.EARTH::equals).count();

        marsContext.getPlayer().setMcIncome(marsContext.getPlayer().getMcIncome() + earthTagCount + 1);

        return null;
    }

    @Override
    public boolean onBuiltEffectApplicableToOther() {
        return true;
    }

    @Override
    public int getWinningPoints() {
        return 1;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SPACE, Tag.EARTH, Tag.JUPITER);
    }

    @Override
    public int getPrice() {
        return 15;
    }
}
