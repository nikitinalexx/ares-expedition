package com.terraforming.ares.mars;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.awards.*;
import com.terraforming.ares.model.milestones.*;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ShuffleService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    private List<Expansion> expansions;
    private boolean dummyHandMode;
    private List<Integer> dummyHand;
    private List<Integer> usedDummyHand = new ArrayList<>();


    public MarsGame(MarsGame copy) {
        this.id = copy.id;
        this.playerUuidToPlayer = copy.getPlayerUuidToPlayer().values().stream().map(Player::new).collect(Collectors.toMap(Player::getUuid, Function.identity()));
        this.projectsDeck = new Deck(copy.projectsDeck);
        this.planet = new Planet(copy.planet);
        this.planetAtTheStartOfThePhase = new Planet(copy.planetAtTheStartOfThePhase);
        this.stateType = copy.stateType;
        this.currentPhase = copy.currentPhase;
        this.turns = copy.turns;
        this.awards = copyAwards(copy);
        this.milestones = copyMilestones(copy);
        this.hasAi = copy.hasAi;
        this.expansions = copy.expansions;
        this.dummyHandMode = copy.dummyHandMode;
        if (copy.dummyHandMode) {
            this.dummyHand = new ArrayList<>(copy.dummyHand);
            this.usedDummyHand = new ArrayList<>(copy.usedDummyHand);
        }
    }

    private List<BaseAward> copyAwards(MarsGame marsGame) {
        List<BaseAward> otherAwards = new ArrayList<>();
        for (BaseAward award : marsGame.getAwards()) {
            BaseAward anotherAward;
            if (award.getType() == AwardType.INDUSTRIALIST) {
                anotherAward = new IndustrialistAward();
            } else if (award.getType() == AwardType.PROJECT_MANAGER) {
                anotherAward = new ProjectManagerAward();
            } else if (award.getType() == AwardType.GENERATOR) {
                anotherAward = new GeneratorAward();
            } else if (award.getType() == AwardType.CELEBRITY) {
                anotherAward = new CelebrityAward();
            } else if (award.getType() == AwardType.COLLECTOR) {
                anotherAward = new CollectorAward();
            } else {
                anotherAward = new ResearcherAward();
            }
            anotherAward.setWinPoints(new ConcurrentHashMap<>(award.getWinPoints()));
            otherAwards.add(anotherAward);
        }
        return otherAwards;
    }

    private List<Milestone> copyMilestones(MarsGame marsGame) {
        List<Milestone> otherMilestones = new ArrayList<>();
        for (Milestone milestone : marsGame.getMilestones()) {
            Milestone anotherMilestone;
            if (milestone.getType() == MilestoneType.BUILDER) {
                anotherMilestone = new BuilderMilestone();
            } else if (milestone.getType() == MilestoneType.DIVERSIFIER) {
                anotherMilestone = new DiversifierMilestone();
            } else if (milestone.getType() == MilestoneType.ENERGIZER) {
                anotherMilestone = new EnergizerMilestone();
            } else if (milestone.getType() == MilestoneType.FARMER) {
                anotherMilestone = new FarmerMilestone();
            } else if (milestone.getType() == MilestoneType.LEGEND) {
                anotherMilestone = new LegendMilestone();
            } else if (milestone.getType() == MilestoneType.MAGNATE) {
                anotherMilestone = new MagnateMilestone();
            } else if (milestone.getType() == MilestoneType.PLANNER) {
                anotherMilestone = new PlannerMilestone();
            } else if (milestone.getType() == MilestoneType.SPACE_BARON) {
                anotherMilestone = new SpaceBaronMilestone();
            } else if (milestone.getType() == MilestoneType.TERRAFORMER) {
                anotherMilestone = new TerraformerMilestone();
            } else if (milestone.getType() == MilestoneType.TYCOON) {
                anotherMilestone = new TycoonMilestone();
            } else {
                anotherMilestone = new GardenerMilestone();
            }

            anotherMilestone.getAchievedByPlayers().addAll(milestone.getAchievedByPlayers());
            anotherMilestone.getPlayerToValue().putAll(milestone.getPlayerToValue());

            otherMilestones.add(anotherMilestone);
        }
        return otherMilestones;
    }

    public MarsGame(List<String> playerNames,
                    int playerHandSize,
                    Deck projectsDeck,
                    Deck corporationsDeck,
                    Planet planet,
                    boolean mulligan,
                    List<BaseAward> awards,
                    List<Milestone> milestones,
                    List<Boolean> computers,
                    List<Expansion> expansions,
                    boolean dummyHandMode,
                    List<Integer> dummyHand) {
        this.projectsDeck = projectsDeck;
        this.corporationsDeck = corporationsDeck;
        this.planet = planet;
        this.awards = awards;
        this.milestones = milestones;
        this.expansions = expansions;
        this.hasAi = (computers.stream().anyMatch(item -> item));
        this.dummyHandMode = dummyHandMode;
        this.dummyHand = new ArrayList<>(dummyHand);

        List<Player> players = new ArrayList<>();

        for (int i = 0; i < playerNames.size(); i++) {
            players.add(
                    Player.builder()
                            .uuid(UUID.randomUUID().toString() + i)
                            .name(playerNames.get(i))
                            //.hand(Deck.builder().cards(new LinkedList<>(List.of(310, 312, 311, 313))).build())
                            .hand(projectsDeck.dealCardsDeck(playerHandSize))
                            .corporations(corporationsDeck.dealCardsDeck(INITIAL_CORPORATIONS_SIZE))
                            //.corporations(Deck.builder().cards(new LinkedList<>(List.of(10206, 10005))).build())
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
                .filter(milestone -> !milestone.isAchieved())
                .forEach(milestone -> {
                            playerUuidToPlayer.values()
                                    .stream()
                                    .filter(player -> milestone.isAchievable(player, cardService))
                                    .forEach(milestone::setAchieved);
                            if (milestone.isAchieved()) {
                                playerUuidToPlayer.values()
                                        .forEach(player -> {
                                            milestone.setValue(player, milestone.getValue(player, cardService));
                                            player.getPlayed().getCards().stream().map(cardService::getCard).forEach(card -> card.onMilestoneGained(this, player, milestone));
                                        });
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
    public int getCurrentDummyHand() {
        if (!isDummyHandMode()) {
            throw new IllegalStateException("Dummy hand only available in dummy hand mode");
        }
        if (CollectionUtils.isEmpty(usedDummyHand)) {
            throw new IllegalStateException("Calling get from dummy hand before it was initialized");
        }
        return usedDummyHand.get(usedDummyHand.size() - 1);
    }

    public void updateDummyHand(ShuffleService shuffleService) {
        if (!isDummyHandMode()) {
            throw new IllegalStateException("Dummy hand only available in dummy hand mode");
        }
        if (CollectionUtils.isEmpty(dummyHand)) {
            dummyHand = IntStream.rangeClosed(1, 5).boxed().collect(Collectors.toList());
            shuffleService.shuffle(dummyHand);
            usedDummyHand = new ArrayList<>();
        }
        usedDummyHand.add(dummyHand.get(0));
        dummyHand.remove(0);
    }

    @JsonIgnore
    public boolean gameEndCondition() {
        return planet.isOceansMax() && planet.isTemperatureMax() && planet.isOxygenMax();
    }
}
