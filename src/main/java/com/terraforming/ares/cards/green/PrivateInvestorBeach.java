package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 19.02.2023
 */
@RequiredArgsConstructor
@Getter
public class PrivateInvestorBeach implements DiscoveryExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public PrivateInvestorBeach(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Private Investor Beach")
                .description("Requires a Milestone. Flip an ocean.")
                .cardAction(CardAction.PRIVATE_INVESTOR_BEACH)
                .bonuses(List.of(Gain.of(GainType.OCEAN, 1)))
                .build();
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        marsContext.getTerraformingService().revealOcean(marsContext);

        return null;
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
        return List.of(Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 8;
    }

}
