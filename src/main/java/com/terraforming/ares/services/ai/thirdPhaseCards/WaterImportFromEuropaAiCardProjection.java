package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.blue.WaterImportFromEuropa;
import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Deck;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.MarsContextProvider;
import com.terraforming.ares.services.TerraformingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedList;

@Component
@RequiredArgsConstructor
public class WaterImportFromEuropaAiCardProjection<T extends Card> implements AiCardProjection<WaterImportFromEuropa> {
    private final MarsContextProvider marsContextProvider;

    private final TerraformingService terraformingService;

    @Override
    public Class<WaterImportFromEuropa> getType() {
        return WaterImportFromEuropa.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card) {
        if (terraformingService.canRevealOcean(game)) {
            int oceanPrice = Math.max(0, 12 - player.getTitaniumIncome());
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
