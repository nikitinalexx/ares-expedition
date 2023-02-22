package com.terraforming.ares.cards.corporations;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.cards.green.PoliticalInfluence;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.milestones.Milestone;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 21.02.2023
 */
@Getter
public class AustellarCorporation implements CorporationCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public AustellarCorporation(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Austellar")
                .description("42 Mc. Choose a tag and add to this card. Place a cube of your player color on a Milestone of your choice." +
                        " Effect: When you gain the Milestone with your player cube on it, gain 3 TR.")
                .cardAction(CardAction.AUSTELLAR_CORPORATION)
                .build();
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card project, Map<Integer, List<Integer>> inputParams) {
        List<Integer> tagInput = inputParams.get(InputFlag.TAG_INPUT.getId());

        final Tag tag = Tag.byIndex(tagInput.get(0));

        assert tag != null;

        marsContext.getPlayer().getCardToTag().put(AustellarCorporation.class, List.of(tag));

        int milestone = inputParams.get(InputFlag.AUSTELLAR_CORPORATION_MILESTONE.getId()).get(0);

        marsContext.getPlayer().setAustellarMilestone(milestone);
    }

    @Override
    public void onMilestoneGained(MarsGame game, Player player, Milestone milestone) {
        if (game.getMilestones().get(player.getAustellarMilestone()) == milestone) {
            player.setTerraformingRating(player.getTerraformingRating() + 3);
        }
    }

    @Override
    public boolean onBuiltEffectApplicableToItself() {
        return true;
    }

    @Override
    public boolean isActiveCard() {
        return false;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        player.setMc(42);
        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.DYNAMIC);
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.DISCOVERY;
    }

    @Override
    public int getPrice() {
        return 42;
    }

}
