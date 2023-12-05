package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import com.terraforming.ares.services.UpgradePhaseHelper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 04.12.2023
 */
@RequiredArgsConstructor
@Getter
public class JezeroCraterHospital implements InfrastructureExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public JezeroCraterHospital(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Jezero Crater Hospital")
                .description("Raise infrastructure 1 step. Upgrade a phase card.")
                .bonuses(List.of(Gain.of(GainType.INFRASTRUCTURE, 1)))
                .cardAction(CardAction.UPDATE_PHASE_CARD)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        marsContext.getTerraformingService().increaseInfrastructure(marsContext);
        return null;
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card project, Map<Integer, List<Integer>> input) {
        List<Integer> cardInput = input.get(InputFlag.PHASE_UPGRADE_CARD.getId());

        final Player player = marsContext.getPlayer();

        UpgradePhaseHelper.upgradePhase(player, cardInput.get(0));
    }

    @Override
    public boolean onBuiltEffectApplicableToItself() {
        return true;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.BUILDING, Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 20;
    }

}
