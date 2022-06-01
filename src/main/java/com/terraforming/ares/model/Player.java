package com.terraforming.ares.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.terraforming.ares.cards.blue.FilterFeeders;
import com.terraforming.ares.model.turn.Turn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


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

    private boolean confirmedGameEndThirdPhase;

    private Integer previousChosenPhase;
    private Integer chosenPhase;

    private List<Turn> nextTurns;

    public void addNextTurn(Turn turn) {
        if (CollectionUtils.isEmpty(nextTurns)) {
            nextTurns = new LinkedList<>();
        }
        nextTurns.add(turn);
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

    private int mc;
    private int mcIncome;

    private int cardIncome;

    private int heat;
    private int heatIncome;

    private int plants;
    private int plantsIncome;

    private int steelIncome;
    private int titaniumIncome;

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

    public void clearRoundResults() {
        chosenPhase = null;
        activatedBlueCards = Deck.builder().build();
        activatedBlueActionTwice = false;
        actionsInSecondPhase = 0;
        canBuildInFirstPhase = 0;
        pickedCardInSecondPhase = false;
        assortedEnterprisesDiscount = false;
        selfReplicatingDiscount = false;
        assortedEnterprisesGreenAvailable = false;
    }

}
