package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.ProgressivePolicies;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.TerraformingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
@RequiredArgsConstructor
public class ProgressivePoliciesActionProcessor implements BlueActionCardProcessor<ProgressivePolicies> {
    private final TerraformingService terraformingService;
    private final CardService cardService;

    @Override
    public Class<ProgressivePolicies> getType() {
        return ProgressivePolicies.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, Card actionCard) {
        int eventTags = cardService.countPlayedTags(player, Set.of(Tag.EVENT));
        int price = (eventTags >= 4) ? 5 : 10;

        player.setMc(player.getMc() - price);

        terraformingService.raiseOxygen(game, player);

        return null;
    }
}
