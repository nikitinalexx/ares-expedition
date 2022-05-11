package com.terraforming.ares.cards.green;

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
public class GanymedeShipyard implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public GanymedeShipyard(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Ganymede Shipyard")
                .description("When you play a Space, you pay 6 MC less for it.")
                .incomes(List.of(Gain.of(GainType.TITANIUM, 2)))
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setTitaniumIncome(player.getTitaniumIncome() + 2);

        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SPACE, Tag.JUPITER);
    }

    @Override
    public int getPrice() {
        return 17;
    }
}
