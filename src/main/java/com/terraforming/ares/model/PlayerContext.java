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
    private Integer selectedCorporationCard;

    private Integer previousStage;
    private Integer currentStage;

    private Turn nextTurn;

}
