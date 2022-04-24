package com.alex.nikitin.terraformingm.mars;

import com.alex.nikitin.terraformingm.model.Deck;
import com.alex.nikitin.terraformingm.model.Game;
import com.alex.nikitin.terraformingm.model.Planet;
import com.alex.nikitin.terraformingm.model.PlayerContext;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
public class MarsGame implements Game {
    private static final int INITIAL_HAND_SIZE = 10;

    private int playersCount;
    private List<PlayerContext> playerContexts;
    private Deck projectsDeck;
    private Deck corporationsDeck;
    private Planet planet;

    public MarsGame(int playersCount, Deck projectsDeck, Deck corporationsDeck, Planet planet) {
        this.playersCount = playersCount;
        this.projectsDeck = projectsDeck;
        this.corporationsDeck = corporationsDeck;
        this.planet = planet;

        playerContexts = Stream.generate(
                () -> PlayerContext.builder()
                        .hand(projectsDeck.dealCards(INITIAL_HAND_SIZE))
                        .played(Deck.builder().build())
                        .build()
        )
                .limit(playersCount)
                .collect(Collectors.toList());
    }

}
