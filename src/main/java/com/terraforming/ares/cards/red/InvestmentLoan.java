package com.terraforming.ares.cards.red;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class InvestmentLoan implements BaseExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public InvestmentLoan(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Investment Loan")
                .description("Requires you to spend 1 TR. Gain 10 MC.")
                .bonuses(List.of(Gain.of(GainType.TERRAFORMING_RATING, -1), Gain.of(GainType.MC, 10)))
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setTerraformingRating(player.getTerraformingRating() - 1);
        player.setMc(player.getMc() + 10);

        return null;
    }

    @Override
    public int getWinningPoints() {
        return 1;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.EARTH, Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 1;
    }

}
