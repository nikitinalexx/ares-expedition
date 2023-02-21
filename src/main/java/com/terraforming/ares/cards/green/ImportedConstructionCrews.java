package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.parameters.ParameterColor;
import com.terraforming.ares.services.UpgradePhaseHelper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 20.02.2023
 */
@RequiredArgsConstructor
@Getter
public class ImportedConstructionCrews implements DiscoveryExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public ImportedConstructionCrews(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Imported Construction Crews")
                .description("Requires yellow temperature or warmer. Upgrade 2 phase cards.")
                .cardAction(CardAction.UPDATE_PHASE_CARD_TWICE)
                .build();
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card project, Map<Integer, List<Integer>> input) {
        List<Integer> cardInput = input.get(InputFlag.PHASE_UPGRADE_CARD.getId());

        UpgradePhaseHelper.upgradePhase(marsContext.getCardService(), marsContext.getGame(), marsContext.getPlayer(), cardInput.get(0));
        UpgradePhaseHelper.upgradePhase(marsContext.getCardService(), marsContext.getGame(), marsContext.getPlayer(), cardInput.get(0));
    }

    @Override
    public boolean onBuiltEffectApplicableToItself() {
        return true;
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }


    @Override
    public List<ParameterColor> getTemperatureRequirement() {
        return List.of(ParameterColor.Y, ParameterColor.W);
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 10;
    }

}
