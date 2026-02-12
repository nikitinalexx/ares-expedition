package com.terraforming.ares.services.ai.turnProcessors.random;

import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.SpecialEffectsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class AiRandomSellCardsService {
    private final SpecialEffectsService specialEffectsService;

    public void sellCardsAsyncByChance(Player player) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (!player.isAiSoldCards()) {
            player.setAiSoldCards(true);
            if (random.nextInt(5) == 0 && player.getMc() < 10 && player.getHand().size() > 3) {
                player.getHand().getCards().remove(random.nextInt(player.getHand().size()));
                player.setMc(player.getMc() + specialEffectsService.getCardPrice(player));
            }
        }
    }

}
