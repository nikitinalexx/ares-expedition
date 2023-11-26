package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.blue.VolcanicPools;
import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.MarsContextProvider;
import com.terraforming.ares.services.TerraformingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class VolcanicPoolsAiCardProjection<T extends Card> implements AiCardProjection<VolcanicPools> {
    private final MarsContextProvider marsContextProvider;
    private final CardService cardService;
    private final TerraformingService terraformingService;

    @Override
    public Class<VolcanicPools> getType() {
        return VolcanicPools.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card, int network) {
        if (terraformingService.canRevealOcean(game)) {
            int oceanPrice = Math.max(0, 12 - cardService.countPlayedTags(player, Set.of(Tag.ENERGY)));
            if (player.getMc() >= oceanPrice) {
                player.setMc(player.getMc() - oceanPrice);
                LinkedList<Integer> handBeforeOceans = new LinkedList<>(player.getHand().getCards());
                terraformingService.revealOcean(
                        marsContextProvider.provide(game, player)
                );
                if (player.getHand().getCards().size() > handBeforeOceans.size()) {
                    player.setHand(Deck.builder().cards(handBeforeOceans).build());
                    return MarsGameRowDifference.builder()
                            .greenCards(Constants.GREEN_CARDS_RATIO)
                            .redCards(Constants.RED_CARDS_RATIO)
                            .blueCards(Constants.BLUE_CARDS_RATIO)
                            .build();
                }
            }
        }

        return new MarsGameRowDifference();
    }
}
