package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class Landfill implements ExperimentExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public Landfill(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Landfill")
                .description("Raise MC income for each different TYPE of production you have NOW.")
                .cardAction(CardAction.MC_INCOME_PER_INCOME)
                .build();
    }

    @Override
    public void payAgain(MarsGame game, CardService cardService, Player player) {
        player.setMc(player.getMc() + countUniqueIncomes(player));
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
        final Player player = marsContext.getPlayer();

        player.setMcIncome(player.getMcIncome() + countUniqueIncomes(player));

        return null;
    }

    private int countUniqueIncomes(Player player) {
        int extraIncome = 0;

        if (player.getMcIncome() > 0) {
            extraIncome++;
        }
        if (player.getHeatIncome() > 0) {
            extraIncome++;
        }
        if (player.getPlantsIncome() > 0) {
            extraIncome++;
        }
        if (player.getSteelIncome() > 0) {
            extraIncome++;
        }
        if (player.getTitaniumIncome() > 0) {
            extraIncome++;
        }
        if (player.getCardIncome() > 0) {
            extraIncome++;
        }

        return extraIncome;
    }

    @Override
    public int getWinningPoints() {
        return -1;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.BUILDING);
    }

    @Override
    public int getPrice() {
        return 2;
    }
}
