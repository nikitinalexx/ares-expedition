package com.terraforming.ares.mars;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.awards.BaseAward;
import com.terraforming.ares.model.milestones.Milestone;
import com.terraforming.ares.services.CardService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.*;
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
    private int turns;
    @Setter
    private List<BaseAward> awards;
    @Setter
    private List<Milestone> milestones;
    private boolean hasAi;

    public MarsGame(List<String> playerNames,
                    int playerHandSize,
                    Deck projectsDeck,
                    Deck corporationsDeck,
                    Planet planet,
                    boolean mulligan,
                    List<BaseAward> awards,
                    List<Milestone> milestones,
                    List<Boolean> computers) {
        this.projectsDeck = projectsDeck;
        this.corporationsDeck = corporationsDeck;
        this.planet = planet;
        this.awards = awards;
        this.milestones = milestones;
        this.hasAi = (computers.stream().anyMatch(item -> item));

        List<Player> players = new ArrayList<>();

        for (int i = 0; i < playerNames.size(); i++) {
            players.add(
                    Player.builder()
                            .uuid(UUID.randomUUID().toString())
                            .name(playerNames.get(i))
                            //.hand(Deck.builder().cards(new LinkedList<>(List.of(31,29,30,34,35))).build())
                            .hand(projectsDeck.dealCardsDeck(playerHandSize))
                            .corporations(corporationsDeck.dealCardsDeck(INITIAL_CORPORATIONS_SIZE))
                            .played(Deck.builder().build())
                            .mulligan(mulligan)
                            .ai(computers.get(i))
                            .build()
            );
        }
        playerUuidToPlayer = players.stream().collect(Collectors.toMap(Player::getUuid, Function.identity()));

        this.stateType = StateType.PICK_CORPORATIONS;
        this.currentPhase = Constants.PICK_CORPORATIONS_PHASE;
        this.planetAtTheStartOfThePhase = new Planet(planet);
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

    public void setStateType(StateType stateType, CardService cardService) {
        playerUuidToPlayer.values().forEach(Player::clearPhaseResults);
        assignMilestones(cardService);
        if (stateType == StateType.GAME_END) {
            assignAwards(cardService);
        }


        this.stateType = stateType;
        switch (stateType) {
            case PICK_CORPORATIONS:
                currentPhase = Constants.PICK_CORPORATIONS_PHASE;
                break;
            case PICK_PHASE:
                currentPhase = -1;
                turns++;
                break;
            case BUILD_GREEN_PROJECTS:
                currentPhase = Constants.BUILD_GREEN_PROJECTS_PHASE;
                break;
            case BUILD_BLUE_RED_PROJECTS:
                currentPhase = Constants.BUILD_BLUE_RED_PROJECTS_PHASE;
                break;
            case PERFORM_BLUE_ACTION:
                currentPhase = Constants.PERFORM_BLUE_ACTION_PHASE;
                break;
            case COLLECT_INCOME:
                currentPhase = Constants.COLLECT_INCOME_PHASE;
                break;
            case DRAFT_CARDS:
                currentPhase = Constants.DRAFT_CARDS_PHASE;
                break;
            case SELL_EXTRA_CARDS:
                currentPhase = Constants.SELL_EXTRA_CARDS_PHASE;
                break;
            default:
                currentPhase = -1;
        }
        if (currentPhase != -1) {
            planetAtTheStartOfThePhase = new Planet(planet);
        }

    }

    private void assignAwards(CardService cardService) {
        awards.forEach(award -> award.assignWinners(playerUuidToPlayer.values(), cardService));
    }

    private void assignMilestones(CardService cardService) {
        milestones.stream()
                .filter(achievement -> !achievement.isAchieved())
                .forEach(achievement -> {
                            playerUuidToPlayer.values()
                                    .stream()
                                    .filter(player -> achievement.isAchievable(player, cardService))
                                    .forEach(achievement::setAchieved);
                            if (achievement.isAchieved()) {
                                playerUuidToPlayer.values()
                                        .forEach(player -> achievement.setValue(player, achievement.getValue(player, cardService)));
                            }
                        }
                );
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
