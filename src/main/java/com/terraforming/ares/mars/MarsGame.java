package com.terraforming.ares.mars;

import com.terraforming.ares.model.Deck;
import com.terraforming.ares.model.Planet;
import com.terraforming.ares.model.PlayerContext;
import com.terraforming.ares.model.StateType;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Getter
@Setter
public class MarsGame {
    private static final int INITIAL_CORPORATIONS_SIZE = 2;

    private long id;
    private int playersCount;
    private Map<String, PlayerContext> playerContexts;
    private Deck projectsDeck;
    private Deck corporationsDeck;
    private Planet planet;
    private StateType stateType;

    public MarsGame(int playersCount, int playerHandSize, Deck projectsDeck, Deck corporationsDeck, Planet planet) {
        this.playersCount = playersCount;
        this.projectsDeck = projectsDeck;
        this.corporationsDeck = corporationsDeck;
        this.planet = planet;
        this.stateType = StateType.PICK_CORPORATIONS;

        playerContexts = Stream.generate(
                () -> PlayerContext.builder()
                        .uuid(UUID.randomUUID().toString())
                        .hand(projectsDeck.dealCards(playerHandSize))
                        .corporations(corporationsDeck.dealCards(INITIAL_CORPORATIONS_SIZE))
                        .played(Deck.builder().build())
                        .build()
        )
                .limit(playersCount)
                .collect(Collectors.toMap(PlayerContext::getUuid, Function.identity()));
    }

    public PlayerContext getPlayerByUuid(String playerUuid) {
        return getPlayerContexts().get(playerUuid);
    }

    public Deck getCorporationsChoice(String playerUuid) {
        return getPlayerContext(playerUuid).getCorporations();
    }

    public void pickCorporation(String playerUuid, int corporationCardId) {
        PlayerContext playerContext = getPlayerContext(playerUuid);

        Deck corporations = playerContext.getCorporations();

        if (corporations.containsCard(corporationCardId)) {
            playerContext.setSelectedCorporationCard(corporationCardId);
        } else {
            throw new IllegalArgumentException(String.format("Corporation card with id %s not found", corporationCardId));
        }
    }

    private PlayerContext getPlayerContext(String playerUuid) {
        PlayerContext playerContext = playerContexts.get(playerUuid);

        if (playerContext == null) {
            throw new IllegalArgumentException(String.format("Player with uuid %s not found", playerUuid));
        }

        return playerContext;
    }

}
