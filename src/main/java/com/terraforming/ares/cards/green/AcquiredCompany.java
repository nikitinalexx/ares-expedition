package com.terraforming.ares.cards.green;

import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.income.Income;
import com.terraforming.ares.model.income.IncomeType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class AcquiredCompany implements BaseExpansionGreenCard {
    private final int id;

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        //todo use getIncomes
        player.setCardIncome(player.getCardIncome() + 1);

        return null;
    }

    @Override
    public List<Income> getIncomes() {
        return List.of(
                Income.of(IncomeType.CARD, 1)
        );
    }

    @Override
    public String description() {
        return "During the production phase, draw a card.";
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.BUILDING, Tag.EARTH);
    }

    @Override
    public int getPrice() {
        return 11;
    }
}
