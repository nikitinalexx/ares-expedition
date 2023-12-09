package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.green.OrbitalCleanup;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.services.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 06.05.2022
 */
@Component
@RequiredArgsConstructor
public class OrbitalCleanupActionProcessor implements BlueActionCardProcessor<OrbitalCleanup> {
    private final CardService cardService;

    @Override
    public Class<OrbitalCleanup> getType() {
        return OrbitalCleanup.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, Card actionCard) {
        int scienceTagsCount = cardService.countPlayedTags(player, Set.of(Tag.SCIENCE));
        player.setMc(player.getMc() + scienceTagsCount);
        return null;
    }
}
