package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import com.terraforming.ares.services.CardService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class LunarBeam implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public LunarBeam(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Lunar Beam")
                .description("Requires you to spend 1 TR. During the production phase, this produces 4 heat.")
                .incomes(List.of(Gain.of(GainType.HEAT, 4)))
                .bonuses(List.of(Gain.of(GainType.TERRAFORMING_RATING, -1)))
                .build();
    }

    @Override
    public void payAgain(MarsGame game, CardService cardService, Player player) {
        player.setHeat(player.getHeat() + 4);
    }

    @Override
    public boolean canPayAgain() {
        return true;
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setTerraformingRating(player.getTerraformingRating() - 1);
        player.setHeatIncome(player.getHeatIncome() + 4);

        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.EARTH, Tag.ENERGY);
    }

    @Override
    public int getPrice() {
        return 9;
    }
}
