package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.parameters.ParameterColor;
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
public class Zeppelins implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public Zeppelins(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Zeppelins")
                .description("Requires red oxygen or higher. During the production phase, this produces 1 MC per Forest you have.")
                .cardAction(CardAction.MC_FOREST_INCOME)
                .build();
    }

    @Override
    public void payAgain(MarsGame game, CardService cardService, Player player) {
        player.setMc(player.getMc() + player.getForests());
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public void onForestBuiltEffect(Player player) {
        player.setMcIncome(player.getMcIncome() + 1);
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setMcIncome(player.getMcIncome() + player.getForests());

        return null;
    }

    @Override
    public List<ParameterColor> getOxygenRequirement() {
        return List.of(ParameterColor.R, ParameterColor.Y, ParameterColor.W);
    }

    @Override
    public int getWinningPoints() {
        return 1;
    }

    @Override
    public int getPrice() {
        return 10;
    }
}
