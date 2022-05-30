package com.terraforming.ares.mars;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.terraforming.ares.model.Deck;
import com.terraforming.ares.model.Planet;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.StateType;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Getter
@NoArgsConstructor
public class MarsGame {
    private static final int INITIAL_CORPORATIONS_SIZE = 2;

    private Long id;
    private Map<String, Player> playerUuidToPlayer;
    private Deck projectsDeck;
    private Deck corporationsDeck;
    private Planet planet;
    private Planet planetAtTheStartOfThePhase;
    private StateType stateType;
    private int currentPhase = -1;
    @JsonIgnore
    private int updateCounter;

    public MarsGame(List<String> playerNames, int playerHandSize, Deck projectsDeck, Deck corporationsDeck, Planet planet) {
        this.projectsDeck = projectsDeck;
        this.corporationsDeck = corporationsDeck;
        this.planet = planet;
        this.stateType = StateType.PICK_CORPORATIONS;

        playerUuidToPlayer = playerNames.stream().map(playerName ->
                Player.builder()
                        .uuid(UUID.randomUUID().toString())
                        .name(playerName)
                        //.hand(Deck.builder().cards(new LinkedList<>(List.of(219, 102, 103, 98, 209))).build())
                        .hand(projectsDeck.dealCardsDeck(playerHandSize))
                        .corporations(corporationsDeck.dealCardsDeck(INITIAL_CORPORATIONS_SIZE))
                        .played(Deck.builder().build())
                        .build()).collect(Collectors.toMap(Player::getUuid, Function.identity())
        );
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

    public void mergeDeck(Deck newDeck) {
        LinkedList<Integer> cards = newDeck.getCards();

        cards.removeAll(projectsDeck.getCards());

        playerUuidToPlayer.values().forEach(player -> {
            cards.removeAll(player.getPlayed().getCards());
            cards.removeAll(player.getHand().getCards());
        });

        projectsDeck.addCards(cards);
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

    public void iterateUpdateCounter() {
        this.updateCounter++;
    }

    public boolean timeToSave() {
        return gameEndCondition() || this.updateCounter % 10 == 0;
    }

    @JsonIgnore
    public boolean gameEndCondition() {
        return planet.isOceansMax() && planet.isTemperatureMax() && planet.isOxygenMax();
    }
}
