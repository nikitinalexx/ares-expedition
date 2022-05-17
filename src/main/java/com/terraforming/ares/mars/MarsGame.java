package com.terraforming.ares.mars;

import com.terraforming.ares.model.Deck;
import com.terraforming.ares.model.Planet;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.StateType;
import lombok.Getter;

import java.util.List;
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
public class MarsGame {
    private static final int INITIAL_CORPORATIONS_SIZE = 2;

    private long id;
    private final int playersCount;
    private final Map<String, Player> playerUuidToPlayer;
    private Deck projectsDeck;//TODO what if it gets empty
    private final Deck corporationsDeck;
    private final Planet planet;
    private Planet planetAtTheStartOfThePhase;
    private StateType stateType;
    private int currentPhase = -1;

    public MarsGame(int playersCount, int playerHandSize, Deck projectsDeck, Deck corporationsDeck, Planet planet) {
        this.playersCount = playersCount;
        this.projectsDeck = projectsDeck;
        this.corporationsDeck = corporationsDeck;
        this.planet = planet;
        this.stateType = StateType.PICK_CORPORATIONS;

        playerUuidToPlayer = Stream.generate(
                () -> Player.builder()
                        .uuid(UUID.randomUUID().toString())
                        .hand(projectsDeck.dealCardsDeck(playerHandSize))
                        .corporations(corporationsDeck.dealCardsDeck(INITIAL_CORPORATIONS_SIZE))
                        .played(Deck.builder().build())
                        .build()
        )
                .limit(playersCount)
                .collect(Collectors.toMap(Player::getUuid, Function.identity()));
    }

    public Player getPlayerByUuid(String playerUuid) {
        return getPlayerUuidToPlayer().get(playerUuid);
    }

    public void setId(long id) {
        this.id = id;
    }

    public List<Integer> dealCards(int count) {
        return projectsDeck.dealCards(count);
    }

    public void setStateType(StateType stateType) {
        this.stateType = stateType;
        switch (stateType) {
            case BUILD_GREEN_PROJECTS:
                currentPhase = 1;
                break;
            case BUILD_BLUE_RED_PROJECTS:
                currentPhase = 2;
                break;
            case PERFORM_BLUE_ACTION:
                currentPhase = 3;
                break;
            case COLLECT_INCOME:
                currentPhase = 4;
                break;
            case DRAFT_CARDS:
                currentPhase = 5;
                break;
            case SELL_EXTRA_CARDS:
                currentPhase = 6;
                break;
            default:
                currentPhase = -1;
        }
        if (currentPhase != -1) {
            planetAtTheStartOfThePhase = new Planet(planet);
        }

    }

}
