package com.terraforming.ares.cards.red;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import com.terraforming.ares.services.TerraformingService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class DeimosDown implements BaseExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public DeimosDown(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Deimos Down")
                .description("Raise the temperature 3 steps. Gain 7 MC.")
                .bonuses(List.of(Gain.of(GainType.TEMPERATURE, 3), Gain.of(GainType.MC, 7)))
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        TerraformingService terraformingService = marsContext.getTerraformingService();
        for (int i = 1; i <= 3; i++) {
            terraformingService.increaseTemperature(marsContext);
        }

        marsContext.getPlayer().setMc(marsContext.getPlayer().getMc() + 7);

        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SPACE, Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 30;
    }

}
