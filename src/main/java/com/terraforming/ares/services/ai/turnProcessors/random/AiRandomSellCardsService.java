package com.terraforming.ares.services.ai.turnProcessors.random;

import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.ai.AiEndgameService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
@RequiredArgsConstructor
public class AiRandomSellCardsService {
    private final AiEndgameService aiEndgameService;
    private final Random random = new Random();

    public void sellCardsAsyncByChance(Player player) {
        if (!player.isAiSoldCards()) {
            player.setAiSoldCards(true);
            if (random.nextInt(5) == 0 && player.getMc() < 10 && player.getHand().size() > 3) {
                player.getHand().getCards().remove(random.nextInt(player.getHand().size()));
                player.setMc(player.getMc() + aiEndgameService.getCardPrice(player));
            }
        }
    }

}
