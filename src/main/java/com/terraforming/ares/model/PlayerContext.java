package com.terraforming.ares.model;

import com.terraforming.ares.model.turn.Turn;
import lombok.Builder;
import lombok.Data;


/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Builder
@Data
public class PlayerContext {
    private String uuid;
    private Deck hand;
    private Deck played;
    private Deck corporations;
    private Deck activatedBlueCards;
    private boolean activatedBlueActionTwice;
    private Integer selectedCorporationCard;

    private Integer previousChosenStage;
    private Integer chosenStage;

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
        previousChosenStage = chosenStage;
        chosenStage = null;
        activatedBlueCards = Deck.builder().build();
        activatedBlueActionTwice = false;
    }

}
