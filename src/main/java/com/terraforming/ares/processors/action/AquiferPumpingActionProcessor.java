package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.AquiferPumping;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Deck;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.parameters.Ocean;
import com.terraforming.ares.services.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@Service
@RequiredArgsConstructor
public class AquiferPumpingActionProcessor implements BlueActionCardProcessor<AquiferPumping> {
    private final CardService deckService;

    @Override
    public Class<AquiferPumping> getType() {
        return AquiferPumping.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player) {
        player.setMc(player.getMc() - (10 - player.getSteelIncome() * 2));

        Ocean ocean = game.getPlanet().revealOcean().orElseThrow(() -> new IllegalStateException("Trying to flip an ocean when no oceans left"));

        player.setMc(player.getMc() + ocean.getMc());
        player.setPlants(player.getPlants() + ocean.getPlants());

        if (ocean.getCards() != 0) {
            Deck deck = game.getProjectsDeck().dealCards(ocean.getCards());

            for (Integer card : deck.getCards()) {
                player.getHand().addCard(card);
            }
        }

        player.setTerraformingRating(player.getTerraformingRating() + 1);

        player.getPlayed().getCards().stream().map(deckService::getProjectCard).forEach(
                projectCard -> projectCard.onOceanFlippedEffect(player)
        );

        return null;
    }


}
