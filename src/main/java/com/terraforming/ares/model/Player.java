package com.terraforming.ares.model;

import com.terraforming.ares.model.turn.Turn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
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

    private boolean confirmedGameEndThirdPhase;

    private Integer previousChosenPhase;
    private Integer chosenPhase;

    private Turn nextTurn;

    private int mc;
    private int mcIncome;

    private int cardIncome;

    private int heat;
    private int heatIncome;

    private int plants;
    private int plantsIncome;

    private int steelIncome;
    private int titaniumIncome;

    public void clearRoundResults() {
        chosenPhase = null;
        activatedBlueCards = Deck.builder().build();
        activatedBlueActionTwice = false;
        actionsInSecondPhase = 0;
        canBuildInFirstPhase = 0;
        pickedCardInSecondPhase = false;
    }

}
