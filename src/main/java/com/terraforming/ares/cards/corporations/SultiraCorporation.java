package com.terraforming.ares.cards.corporations;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.UpgradePhaseHelper;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 21.02.2023
 */
@Getter
public class SultiraCorporation implements CorporationCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public SultiraCorporation(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Sultira")
                .description("38 Mc. Upgrade your phase 1 card. When you play an Energy tag, get 2 Heat.")
                .cardAction(CardAction.SULTIRA_CORPORATION)
                .build();
    }

    @Override
    public boolean isActiveCard() {
        return false;
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card project, Map<Integer, List<Integer>> inputParams) {
        final int energyTagsCount = marsContext.getCardService().countCardTags(project, Set.of(Tag.ENERGY), inputParams);

        final Player player = marsContext.getPlayer();

        player.setHeat(player.getHeat() + energyTagsCount * 2);

        if (project.getClass() == SultiraCorporation.class) {
            List<Integer> cardInput = inputParams.get(InputFlag.PHASE_UPGRADE_CARD.getId());

            final MarsGame game = marsContext.getGame();

            UpgradePhaseHelper.upgradePhase(player, cardInput.get(0));
        }
    }

    @Override
    public boolean onBuiltEffectApplicableToItself() {
        return true;
    }

    @Override
    public boolean onBuiltEffectApplicableToOther() {
        return true;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        player.setMc(38);
        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.ENERGY);
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.DISCOVERY;
    }

    @Override
    public int getPrice() {
        return 38;
    }

}
