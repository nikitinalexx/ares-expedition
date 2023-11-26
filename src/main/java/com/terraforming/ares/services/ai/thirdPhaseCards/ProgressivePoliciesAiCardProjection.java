package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.blue.ProgressivePolicies;
import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.MarsContextProvider;
import com.terraforming.ares.services.TerraformingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class ProgressivePoliciesAiCardProjection<T extends Card> implements AiCardProjection<ProgressivePolicies> {
    private final TerraformingService terraformingService;
    private final MarsContextProvider marsContextProvider;
    private final CardService cardService;

    @Override
    public Class<ProgressivePolicies> getType() {
        return ProgressivePolicies.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card, int network) {
        if (!terraformingService.canIncreaseOxygen(game)) {
            return new MarsGameRowDifference();
        }

        int eventTags = cardService.countPlayedTags(player, Set.of(Tag.EVENT));
        int price = (eventTags >= 4) ? 5 : 10;

        if (player.getMc() < price) {
            return new MarsGameRowDifference();
        }

        player.setMc(player.getMc() - price);
        terraformingService.raiseOxygen(marsContextProvider.provide(game, player));

        return new MarsGameRowDifference();
    }
}
