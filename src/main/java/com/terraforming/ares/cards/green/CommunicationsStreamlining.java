package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.UpgradePhaseHelper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.terraforming.ares.model.SpecialEffect.COMMUNICATIONS_STREAMLINING;

/**
 * Created by oleksii.nikitin
 * Creation date 17.02.2023
 */
@RequiredArgsConstructor
@Getter
public class CommunicationsStreamlining implements DiscoveryExpansionBlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public CommunicationsStreamlining(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Communications Streamlining")
                .description("Upgrade your phase 3 card. Effect: When you reveal an upgraded phase card, gain 1 MC.")
                .cardAction(CardAction.COMMUNICATIONS_STREAMLINING)
                .build();
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card project, Map<Integer, List<Integer>> input) {
        List<Integer> cardInput = input.get(InputFlag.PHASE_UPGRADE_CARD.getId());

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
    public List<Tag> getTags() {
        return List.of(Tag.SCIENCE, Tag.ENERGY);
    }

    @Override
    public Set<SpecialEffect> getSpecialEffects() {
        return Set.of(COMMUNICATIONS_STREAMLINING);
    }

    @Override
    public int getPrice() {
        return 10;
    }

}
