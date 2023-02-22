package com.terraforming.ares.cards.corporations;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.UpgradePhaseHelper;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.terraforming.ares.model.SpecialEffect.NEBU_LABS_CORPORATION;

/**
 * Created by oleksii.nikitin
 * Creation date 21.02.2023
 */
@Getter
public class NebuLabsCorporation implements CorporationCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public NebuLabsCorporation(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Nebu Labs")
                .description("25 Mc. Update a phase card. Effect: When you reveal an upgraded phase card, gain 2 MC.")
                .cardAction(CardAction.NEBU_LABS_CORPORATION)
                .build();
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card project, Map<Integer, List<Integer>> inputParams) {
        List<Integer> cardInput = inputParams.get(InputFlag.PHASE_UPGRADE_CARD.getId());

        final MarsGame game = marsContext.getGame();
        final Player player = marsContext.getPlayer();

        UpgradePhaseHelper.upgradePhase(marsContext.getCardService(), game, player, cardInput.get(0));
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
        player.setMc(25);
        return null;
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.DISCOVERY;
    }

    @Override
    public Set<SpecialEffect> getSpecialEffects() {
        return Set.of(NEBU_LABS_CORPORATION);
    }

    @Override
    public int getPrice() {
        return 25;
    }

}
