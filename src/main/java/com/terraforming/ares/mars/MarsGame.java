package com.terraforming.ares.mars;

import com.terraforming.ares.model.Deck;
import com.terraforming.ares.model.Planet;
import com.terraforming.ares.model.Player;
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
    private Map<String, Player> playerUuidToPlayer;
    private Deck projectsDeck;//TODO what if it gets empty
    private Deck corporationsDeck;
    private Planet planet;
    private StateType stateType;

    public MarsGame(int playersCount, int playerHandSize, Deck projectsDeck, Deck corporationsDeck, Planet planet) {
        this.playersCount = playersCount;
        this.projectsDeck = projectsDeck;
        this.corporationsDeck = corporationsDeck;
        this.planet = planet;
        this.stateType = StateType.PICK_CORPORATIONS;

        playerUuidToPlayer = Stream.generate(
                () -> Player.builder()
                        .uuid(UUID.randomUUID().toString())
                        .hand(projectsDeck.dealCards(playerHandSize))
                        .corporations(corporationsDeck.dealCards(INITIAL_CORPORATIONS_SIZE))
                        .played(Deck.builder().build())
                        .build()
        )
                .limit(playersCount)
                .collect(Collectors.toMap(Player::getUuid, Function.identity()));
    }

    public Player getPlayerByUuid(String playerUuid) {
        return getPlayerUuidToPlayer().get(playerUuid);
    }

    public Deck getCorporationsChoice(String playerUuid) {
        return getPlayer(playerUuid).getCorporations();
    }

    public void pickCorporation(String playerUuid, int corporationCardId) {
        Player player = getPlayer(playerUuid);

        Deck corporations = player.getCorporations();

        if (corporations.containsCard(corporationCardId)) {
            player.setSelectedCorporationCard(corporationCardId);
        } else {
            throw new IllegalArgumentException(String.format("Corporation card with id %s not found", corporationCardId));
        }
    }

    private Player getPlayer(String playerUuid) {
        Player player = playerUuidToPlayer.get(playerUuid);

        if (player == null) {
            throw new IllegalArgumentException(String.format("Player with uuid %s not found", playerUuid));
        }

        return player;
    }

}
