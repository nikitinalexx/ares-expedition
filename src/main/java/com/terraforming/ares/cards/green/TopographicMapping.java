package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.UpgradePhaseHelper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 19.02.2023
 */
@RequiredArgsConstructor
@Getter
public class TopographicMapping implements DiscoveryExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public TopographicMapping(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Topographic Mapping")
                .description("Choose a tag and add to this card. Upgrade a phase card")
                .cardAction(CardAction.TOPOGRAPHIC_MAPPING)
                .build();
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card project, Map<Integer, List<Integer>> input) {
        List<Integer> tagInput = input.get(InputFlag.TAG_INPUT.getId());
        final Tag tag = Tag.byIndex(tagInput.get(0));
        marsContext.getPlayer().getCardToTag().put(TopographicMapping.class, List.of(tag));

        List<Integer> cardInput = input.get(InputFlag.PHASE_UPGRADE_CARD.getId());
        UpgradePhaseHelper.upgradePhase(marsContext.getPlayer(), cardInput.get(0));
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
        return List.of(Tag.DYNAMIC, Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 10;
    }

}
