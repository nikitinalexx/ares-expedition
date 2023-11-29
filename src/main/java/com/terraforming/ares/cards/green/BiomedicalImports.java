package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.UpgradePhaseHelper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 20.02.2023
 */
@RequiredArgsConstructor
@Getter
public class BiomedicalImports implements DiscoveryExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public BiomedicalImports(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Biomedical Imports")
                .description("Increase the oxygen once OR upgrade a phase card.")
                .cardAction(CardAction.BIOMEDICAL_IMPORTS)
                .build();
    }
    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card project, Map<Integer, List<Integer>> input) {
        List<Integer> raiseOxygenInput = input.get(InputFlag.BIOMEDICAL_IMPORTS_RAISE_OXYGEN.getId());
        if (!CollectionUtils.isEmpty(raiseOxygenInput)) {
            marsContext.getTerraformingService().raiseOxygen(marsContext);
            return;
        }

        List<Integer> upgradePhaseInput = input.get(InputFlag.PHASE_UPGRADE_CARD.getId());

        UpgradePhaseHelper.upgradePhase(marsContext.getPlayer(), upgradePhaseInput.get(0));
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
    public List<Tag> getTags() {
        return List.of(Tag.SCIENCE, Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 9;
    }

}
