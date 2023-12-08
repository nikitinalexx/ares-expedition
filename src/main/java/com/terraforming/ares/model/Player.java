package com.terraforming.ares.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.terraforming.ares.model.turn.Turn;
import lombok.*;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;


/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Player {
    private String uuid;
    private String name;
    private Deck hand;
    @Builder.Default
    private Deck played = Deck.builder().build();
    private Deck corporations;
    @Builder.Default
    private Deck activatedBlueCards = Deck.builder().build();
    private int blueActionExtraActivationsLeft;
    private Integer selectedCorporationCard;
    @Builder.Default
    private Map<Class<?>, Integer> cardResourcesCount = new HashMap<>();
    @Builder.Default
    private Map<Class<?>, List<Tag>> cardToTag = new HashMap<>();
    @Builder.Default
    private int terraformingRating = Constants.STARTING_RT;
    private int forests;
    private boolean builtSpecialDesignLastTurn;
    private boolean unmiCorporation;
    private boolean hasUnmiAction;
    private boolean didUnmiAction;
    private boolean mulligan;
    @Builder.Default
    private int austellarMilestone = -1;
    private int lunaProjectOffice = 0;

    private boolean confirmedGameEndThirdPhase;

    private Integer previousChosenPhase;
    @Builder.Default
    private Integer chosenPhase = -1;

    private List<Turn> nextTurns;

    //TODO left here for back compatibility, remove later
    private boolean ai;

    private PlayerDifficulty difficulty;

    private int mc;
    private int mcIncome;

    private int cardIncome;

    private int heat;
    private int heatIncome;

    private int plants;
    private int plantsIncome;

    private int steelIncome;
    private int titaniumIncome;

    private int extraPoints;

    @Builder.Default
    private List<Integer> phaseCards = new ArrayList<>(List.of(0, 0, 0, 0, 0));

    @Builder.Default
    private List<BuildDto> builds = new ArrayList<>();

    public Player(Player copy) {
        this.uuid = copy.uuid;
        this.name = copy.name;
        this.hand = new Deck(copy.hand);
        this.extraPoints = copy.extraPoints;

        this.played = new Deck(copy.played);

        this.activatedBlueCards = new Deck(copy.activatedBlueCards);
        this.blueActionExtraActivationsLeft = copy.blueActionExtraActivationsLeft;
        this.selectedCorporationCard = copy.selectedCorporationCard;
        this.corporations = copy.corporations;

        this.cardResourcesCount = new HashMap<>(copy.getCardResourcesCount());
        this.cardToTag = copy.getCardToTag().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> new ArrayList<>(entry.getValue())
                ));

        this.terraformingRating = copy.terraformingRating;
        this.forests = copy.forests;
        this.builtSpecialDesignLastTurn = copy.builtSpecialDesignLastTurn;
        this.unmiCorporation = copy.unmiCorporation;
        this.hasUnmiAction = copy.hasUnmiAction;
        this.didUnmiAction = copy.didUnmiAction;
        this.mulligan = copy.mulligan;
        this.austellarMilestone = copy.austellarMilestone;

        this.confirmedGameEndThirdPhase = copy.confirmedGameEndThirdPhase;

        this.previousChosenPhase = copy.previousChosenPhase;

        this.chosenPhase = copy.chosenPhase;

        this.nextTurns = (copy.nextTurns == null ? null : new LinkedList<>(copy.nextTurns));

        this.ai = copy.ai;
        this.difficulty = copy.difficulty;

        this.mc = copy.mc;
        this.mcIncome = copy.mcIncome;

        this.cardIncome = copy.cardIncome;

        this.heat = copy.heat;
        this.heatIncome = copy.heatIncome;

        this.plants = copy.plants;
        this.plantsIncome = copy.plantsIncome;

        this.steelIncome = copy.steelIncome;
        this.titaniumIncome = copy.titaniumIncome;

        this.phaseCards = new ArrayList<>(copy.phaseCards);

        this.builds = new ArrayList<>(copy.builds);
    }

    public void setTerraformingRating(int terraformingRating) {
        if (terraformingRating > this.terraformingRating && unmiCorporation && !didUnmiAction) {
            hasUnmiAction = true;
        }
        this.terraformingRating = terraformingRating;
    }

    public void addNextTurn(Turn turn) {
        if (CollectionUtils.isEmpty(nextTurns)) {
            nextTurns = new LinkedList<>();
        }
        nextTurns.add(turn);
    }

    public void addFirstTurn(Turn turn) {
        if (CollectionUtils.isEmpty(nextTurns)) {
            nextTurns = new LinkedList<>();
        }
        nextTurns.add(0, turn);
    }

    @JsonIgnore
    public Turn getNextTurn() {
        if (CollectionUtils.isEmpty(nextTurns)) {
            return null;
        } else {
            return nextTurns.get(0);
        }
    }

    public void removeNextTurn() {
        if (!CollectionUtils.isEmpty(nextTurns)) {
            nextTurns.remove(0);
        }
    }

    public void removeResources(Card toCard, int count) {
        this.cardResourcesCount.put(
                toCard.getClass(),
                this.cardResourcesCount.get(toCard.getClass()) - count
        );
    }

    public void initResources(Card toCard) {
        this.cardResourcesCount.put(toCard.getClass(), 0);
    }

    public void updatePhaseCard(int phaseIndex, int card) {
        phaseCards.set(phaseIndex, card);
    }

    public boolean hasPhaseUpgrade(int upgrade) {
        return phaseCards.get(upgrade / 3) == (upgrade % 3);
    }

    public boolean isPhaseUpgraded(int phase) {
        return phaseCards.get(phase - 1) != 0;
    }

    public int countPhaseUpgrades() {
        int upgrades = 0;
        for (int i = 0; i < phaseCards.size(); i++) {
            if (phaseCards.get(i) != 0) {
                upgrades++;
            }
        }
        return upgrades;
    }

    public boolean canBuildAny(List<BuildType> types) {
        for (BuildDto availableBuild : builds) {
            if (types.contains(availableBuild.getType())) {
                return true;
            }
        }
        return false;
    }

    public boolean canBuildGreen() {
        return builds.stream().anyMatch(build -> build.getType().isGreen());
    }

    public boolean canBuildBlueRed() {
        return builds.stream().anyMatch(build -> build.getType().isBlueRed());
    }

    public boolean cantBuildAnything() {
        return builds.isEmpty();
    }

    public void clearRoundResults() {
        builds = new ArrayList<>();

        chosenPhase = null;
        activatedBlueCards = Deck.builder().build();
        blueActionExtraActivationsLeft = 0;
        hasUnmiAction = false;
        didUnmiAction = false;
        mulligan = true;
    }

    public void clearPhaseResults() {
        builds = new ArrayList<>();

        hasUnmiAction = false;
        didUnmiAction = false;

        builtSpecialDesignLastTurn = false;
    }

    @JsonIgnore
    public boolean isFirstBot() {
        return this.uuid.endsWith("0");
    }

    @JsonIgnore
    public boolean isSecondBot() {
        return this.uuid.endsWith("1");
    }

    @JsonIgnore
    public boolean isComputer() {
        return difficulty != null && difficulty != PlayerDifficulty.NONE;
    }

}
