package com.terraforming.ares.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.terraforming.ares.cards.blue.FilterFeeders;
import com.terraforming.ares.model.turn.Turn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.util.*;


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
    private boolean activatedBlueActionTwice;
    private Integer selectedCorporationCard;
    @Builder.Default
    private Map<Class<?>, Integer> cardResourcesCount = new HashMap<>();
    @Builder.Default
    private int terraformingRating = Constants.STARTING_RT;
    private int actionsInSecondPhase;
    private boolean pickedCardInSecondPhase;
    private int canBuildInFirstPhase;
    private int forests;
    private boolean builtSpecialDesignLastTurn;
    private boolean builtWorkCrewsLastTurn;
    private boolean canBuildAnotherGreenWith9Discount;
    private boolean assortedEnterprisesDiscount;
    private boolean assortedEnterprisesGreenAvailable;
    private boolean selfReplicatingDiscount;
    private boolean mayNiDiscount;
    private boolean unmiCorporation;
    private boolean hasUnmiAction;
    private boolean didUnmiAction;
    private boolean mulligan;

    private boolean confirmedGameEndThirdPhase;

    private Integer previousChosenPhase;
    @Builder.Default
    private Integer chosenPhase = -1;

    private List<Turn> nextTurns;

    private boolean ai;

    private int mc;
    private int mcIncome;

    private int cardIncome;

    private int heat;
    private int heatIncome;

    private int plants;
    private int plantsIncome;

    private int steelIncome;
    private int titaniumIncome;

    @Builder.Default
    private List<Integer> phaseCards = new ArrayList<>(List.of(0, 0, 0, 0, 0));

    public Player(Player copy) {
        this.uuid = copy.uuid;
        this.name = copy.name;
        this.hand = new Deck(copy.hand);

        this.played = new Deck(copy.played);

        this.activatedBlueCards = new Deck(copy.activatedBlueCards);
        this.activatedBlueActionTwice = copy.activatedBlueActionTwice;
        this.selectedCorporationCard = copy.selectedCorporationCard;

        this.cardResourcesCount = new HashMap<>(copy.getCardResourcesCount());

        this.terraformingRating = copy.terraformingRating;
        this.actionsInSecondPhase = copy.actionsInSecondPhase;
        this.pickedCardInSecondPhase = copy.pickedCardInSecondPhase;
        this.canBuildInFirstPhase = copy.canBuildInFirstPhase;
        this.forests = copy.forests;
        this.builtSpecialDesignLastTurn = copy.builtSpecialDesignLastTurn;
        this.builtWorkCrewsLastTurn = copy.builtWorkCrewsLastTurn;
        this.canBuildAnotherGreenWith9Discount = copy.canBuildAnotherGreenWith9Discount;
        this.assortedEnterprisesDiscount = copy.assortedEnterprisesDiscount;
        this.assortedEnterprisesGreenAvailable = copy.assortedEnterprisesGreenAvailable;
        this.selfReplicatingDiscount = copy.selfReplicatingDiscount;
        this.mayNiDiscount = copy.mayNiDiscount;
        this.unmiCorporation = copy.unmiCorporation;
        this.hasUnmiAction = copy.hasUnmiAction;
        this.didUnmiAction = copy.didUnmiAction;
        this.mulligan = copy.mulligan;

        this.confirmedGameEndThirdPhase = copy.confirmedGameEndThirdPhase;

        this.previousChosenPhase = copy.previousChosenPhase;

        this.chosenPhase = copy.chosenPhase;

        this.nextTurns = (copy.nextTurns == null ? null: new LinkedList<>(copy.nextTurns));

        this.ai = copy.ai;

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

    @JsonIgnore//todo is it needed
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

    public void setActionsInSecondPhase(int actionsInSecondPhase) {
        if (this.actionsInSecondPhase == 1) {
            setPickedCardInSecondPhase(true);
        }
        this.actionsInSecondPhase = actionsInSecondPhase;
    }

    public void addResources(Card toCard, int count) {
        this.cardResourcesCount.put(
                toCard.getClass(),
                this.cardResourcesCount.get(toCard.getClass()) + count
        );
        if (count > 0 && toCard.getCollectableResource() == CardCollectableResource.MICROBE && this.cardResourcesCount.containsKey(FilterFeeders.class)) {
            this.cardResourcesCount.put(FilterFeeders.class, this.cardResourcesCount.get(FilterFeeders.class) + 1);
        }
    }

    public void initResources(Card toCard) {
        this.cardResourcesCount.put(toCard.getClass(), 0);
    }

    public void updatePhaseCard(int phaseIndex, int card) {
        phaseCards.set(phaseIndex, card);
    }
    public void clearRoundResults() {
        chosenPhase = null;
        activatedBlueCards = Deck.builder().build();
        activatedBlueActionTwice = false;
        actionsInSecondPhase = 0;
        canBuildInFirstPhase = 0;
        pickedCardInSecondPhase = false;
        assortedEnterprisesDiscount = false;
        selfReplicatingDiscount = false;
        mayNiDiscount = false;
        assortedEnterprisesGreenAvailable = false;
        hasUnmiAction = false;
        didUnmiAction = false;
        mulligan = true;
    }

    public void clearPhaseResults() {
        hasUnmiAction = false;
        didUnmiAction = false;

        builtSpecialDesignLastTurn = false;
        builtWorkCrewsLastTurn = false;
        canBuildAnotherGreenWith9Discount = false;
        assortedEnterprisesDiscount = false;
        selfReplicatingDiscount = false;
        mayNiDiscount = false;
        assortedEnterprisesGreenAvailable = false;
    }

}
