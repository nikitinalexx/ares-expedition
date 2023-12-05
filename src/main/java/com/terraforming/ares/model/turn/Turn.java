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
        @JsonSubTypes.Type(value = IncreaseInfrastructureTurn.class, name = "INCREASE_INFRASTRUCTURE"),
        @JsonSubTypes.Type(value = PerformBlueActionTurn.class, name = "PERFORM_BLUE_ACTION"),
        @JsonSubTypes.Type(value = PhaseChoiceTurn.class, name = "PICK_PHASE"),
        @JsonSubTypes.Type(value = PickExtraBonusSecondPhase.class, name = "PICK_EXTRA_BONUS_SECOND_PHASE"),
        @JsonSubTypes.Type(value = PlantForestTurn.class, name = "PLANT_FOREST"),
        @JsonSubTypes.Type(value = SellCardsTurn.class, name = "SELL_CARDS"),
        @JsonSubTypes.Type(value = SkipTurn.class, name = "SKIP_TURN"),
        @JsonSubTypes.Type(value = StandardProjectTurn.class, name = "STANDARD_PROJECT"),
        @JsonSubTypes.Type(value = GameEndConfirmTurn.class, name = "GAME_END_CONFIRM"),
        @JsonSubTypes.Type(value = SellCardsLastRoundTurn.class, name = "SELL_CARDS_LAST_ROUND"),
        @JsonSubTypes.Type(value = UnmiRtTurn.class, name = "UNMI_RT"),
        @JsonSubTypes.Type(value = MulliganTurn.class, name = "MULLIGAN"),
        @JsonSubTypes.Type(value = CrysisCardPersistentChoiceTurn.class, name = "RESOLVE_PERSISTENT_WITH_CHOICE"),
        @JsonSubTypes.Type(value = CrysisCardImmediateChoiceTurn.class, name = "RESOLVE_IMMEDIATE_WITH_CHOICE"),
        @JsonSubTypes.Type(value = CrysisPersistentAllTurn.class, name = "RESOLVE_PERSISTENT_ALL"),
        @JsonSubTypes.Type(value = CrysisImmediateAllTurn.class, name = "RESOLVE_IMMEDIATE_ALL"),
        @JsonSubTypes.Type(value = PlantsToCrisisTokenTurn.class, name = "PLANTS_TO_CRYSIS_TOKEN"),
        @JsonSubTypes.Type(value = CardsToCrisisTokenTurn.class, name = "CARDS_TO_CRISIS_TOKEN"),
        @JsonSubTypes.Type(value = SellVpTurn.class, name = "SELL_VP"),
        @JsonSubTypes.Type(value = CrisisVpToTokenTurn.class, name = "CRISIS_VP_TO_TOKEN"),
        @JsonSubTypes.Type(value = ResolveOceanDetrimentTurn.class, name = "RESOLVE_OCEAN_DETRIMENT")

})
public interface Turn {

    String getPlayerUuid();

    @JsonIgnore
    TurnType getType();

    @JsonIgnore
    /*
      True if this turn is the prototype of the future expected player turn
     */
    default boolean expectedAsNextTurn() {
        return false;
    }

}
