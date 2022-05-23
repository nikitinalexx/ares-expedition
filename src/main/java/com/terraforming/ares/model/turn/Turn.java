package com.terraforming.ares.model.turn;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = BuildBlueRedProjectTurn.class, name = "BUILD_BLUE_RED_PROJECT"),
        @JsonSubTypes.Type(value = BuildGreenProjectTurn.class, name = "BUILD_GREEN_PROJECT"),
        @JsonSubTypes.Type(value = CollectIncomeTurn.class, name = "COLLECT_INCOME"),
        @JsonSubTypes.Type(value = CorporationChoiceTurn.class, name = "PICK_CORPORATION"),
        @JsonSubTypes.Type(value = DiscardCardsTurn.class, name = "DISCARD_CARDS"),
        @JsonSubTypes.Type(value = DraftCardsTurn.class, name = "DRAFT_CARDS"),
        @JsonSubTypes.Type(value = ExchangeHeatTurn.class, name = "EXCHANGE_HEAT"),
        @JsonSubTypes.Type(value = IncreaseTemperatureTurn.class, name = "INCREASE_TEMPERATURE"),
        @JsonSubTypes.Type(value = PerformBlueActionTurn.class, name = "PERFORM_BLUE_ACTION"),
        @JsonSubTypes.Type(value = PhaseChoiceTurn.class, name = "PICK_PHASE"),
        @JsonSubTypes.Type(value = PickExtraCardTurn.class, name = "PICK_EXTRA_CARD"),
        @JsonSubTypes.Type(value = PlantForestTurn.class, name = "PLANT_FOREST"),
        @JsonSubTypes.Type(value = SellCardsTurn.class, name = "SELL_CARDS"),
        @JsonSubTypes.Type(value = SkipTurn.class, name = "SKIP_TURN"),
        @JsonSubTypes.Type(value = StandardProjectTurn.class, name = "STANDARD_PROJECT"),
        @JsonSubTypes.Type(value = GameEndConfirmTurn.class, name = "GAME_END_CONFIRM"),
        @JsonSubTypes.Type(value = SellCardsLastRoundTurn.class, name = "SELL_CARDS_LAST_ROUND")
})
public interface Turn {

    String getPlayerUuid();

    @JsonIgnore
    TurnType getType();

}
