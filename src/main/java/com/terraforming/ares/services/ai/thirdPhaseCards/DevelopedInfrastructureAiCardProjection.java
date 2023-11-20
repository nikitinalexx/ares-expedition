package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.blue.DevelopedInfrastructure;
import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.MarsContextProvider;
import com.terraforming.ares.services.TerraformingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DevelopedInfrastructureAiCardProjection<T extends Card> implements AiCardProjection<DevelopedInfrastructure> {
    private final TerraformingService terraformingService;
    private final CardService cardService;
    private final MarsContextProvider marsContextProvider;

    @Override
    public Class<DevelopedInfrastructure> getType() {
        return DevelopedInfrastructure.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card) {
        if (!terraformingService.canIncreaseTemperature(game)) {
            return new MarsGameRowDifference();
        }

        int temperaturePrice = 10;

        long minFiveBlueCards = player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .map(Card::getColor)
                .filter(CardColor.BLUE::equals)
                .limit(5)
                .count();

        if (minFiveBlueCards == 5) {
            temperaturePrice -= 5;
        }

        if (player.getMc() < temperaturePrice) {
            return new MarsGameRowDifference();
        }

        terraformingService.increaseTemperature(marsContextProvider.provide(game, player));
        player.setMc(player.getMc() - temperaturePrice);

        return new MarsGameRowDifference();
    }
}
