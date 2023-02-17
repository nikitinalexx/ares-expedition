package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import com.terraforming.ares.services.CardService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 17.02.2023
 */
@RequiredArgsConstructor
@Getter
public class Warehouses implements DiscoveryExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public Warehouses(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Warehouses")
                .description("Upgrade a phase card. During the production phase, this produces 2 MC.")
                .cardAction(CardAction.UPDATE_PHASE_CARD)
                .incomes(List.of(Gain.of(GainType.MC, 2)))
                .build();
    }

    @Override
    public void postProjectBuiltEffect(CardService cardService, MarsGame game, Player player, Card project, Map<Integer, List<Integer>> input) {
        List<Integer> cardInput = input.get(InputFlag.PHASE_UPGRADE_CARD.getId());

        Integer phaseInput = cardInput.get(0);

        //0 to 4. represents 1 to 5 phase.
        int phaseId = phaseInput / 2;

        //0 is default phase. 1 is first upgrade, 2 is second upgrade.
        int card = phaseInput % 2 + 1;

        player.updatePhaseCard(phaseId, card);
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
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setMcIncome(player.getHeatIncome() + 2);

        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.BUILDING);
    }

    @Override
    public int getPrice() {
        return 17;
    }

}
