package com.terraforming.ares.cards.corporations;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import lombok.Getter;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 27.04.2022
 */
@Getter
public class MiningGuildCorporation implements CorporationCard {
    private final int id;
    private final CardMetadata cardMetadata;

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.BUILDING, Tag.BUILDING);
    }

    public MiningGuildCorporation(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Mining Guild")
                .description("27 Mc. 1 Steel income. Whenever you play a card that increases Steel income, gain 1 TR.")
                .cardAction(CardAction.MINING_GUILD_CORPORATION)
                .build();
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card project, Map<Integer, List<Integer>> inputParams) {
        CardMetadata metadata = project.getCardMetadata();
        if (metadata == null) {
            return;
        }
        List<Gain> projectIncomes = metadata.getIncomes();
        if (CollectionUtils.isEmpty(projectIncomes)) {
            return;
        }

        final Player player = marsContext.getPlayer();

        if (projectIncomes.stream().anyMatch(gain -> gain.getType() == GainType.STEEL && gain.getValue() > 0)) {
            player.setTerraformingRating(player.getTerraformingRating() + 1);
        }
    }

    @Override
    public boolean onBuiltEffectApplicableToOther() {
        return true;
    }

    @Override
    public boolean isActiveCard() {
        return false;
    }


    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        player.setMc(27);
        player.setSteelIncome(player.getSteelIncome() + 1);
        return null;
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }

    @Override
    public int getPrice() {
        return 27;
    }

}
