package com.terraforming.ares.cards.corporations;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.UpgradePhaseHelper;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 21.02.2023
 */
@Getter
public class HyperionSystemsCorporation implements CorporationCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public HyperionSystemsCorporation(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Hyperion Systems")
                .description("30 Mc. Upgrade your phase 3 card. Action: Gain 1 MC. *If you chose the action phase this turn, gain an additional 1 MC.")
                .cardAction(CardAction.HYPERION_SYSTEMS_CORPORATION)
                .build();
    }

    @Override
    public boolean isActiveCard() {
        return true;
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card project, Map<Integer, List<Integer>> inputParams) {
        List<Integer> cardInput = inputParams.get(InputFlag.PHASE_UPGRADE_CARD.getId());

        final MarsGame game = marsContext.getGame();

        UpgradePhaseHelper.upgradePhase(marsContext.getPlayer(), cardInput.get(0));
    }

    @Override
    public boolean onBuiltEffectApplicableToItself() {
        return true;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        player.setMc(30);
        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.EARTH);
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.DISCOVERY;
    }

    @Override
    public int getPrice() {
        return 30;
    }

}
