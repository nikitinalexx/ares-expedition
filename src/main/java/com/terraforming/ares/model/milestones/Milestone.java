package com.terraforming.ares.model.milestones;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by oleksii.nikitin
 * Creation date 13.06.2022
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = BuilderMilestone.class, name = "BUILDER"),
        @JsonSubTypes.Type(value = DiversifierMilestone.class, name = "DIVERSIFIER"),
        @JsonSubTypes.Type(value = EnergizerMilestone.class, name = "ENERGIZER"),
        @JsonSubTypes.Type(value = FarmerMilestone.class, name = "FARMER"),
        @JsonSubTypes.Type(value = LegendMilestone.class, name = "LEGEND"),
        @JsonSubTypes.Type(value = MagnateMilestone.class, name = "MAGNATE"),
        @JsonSubTypes.Type(value = PlannerMilestone.class, name = "PLANNER"),
        @JsonSubTypes.Type(value = SpaceBaronMilestone.class, name = "SPACE_BARON"),
        @JsonSubTypes.Type(value = TerraformerMilestone.class, name = "TERRAFORMER"),
        @JsonSubTypes.Type(value = TycoonMilestone.class, name = "TYCOON"),
        @JsonSubTypes.Type(value = GardenerMilestone.class, name = "GARDENER"),
        @JsonSubTypes.Type(value = EcologistMilestone.class, name = "ECOLOGIST"),
        @JsonSubTypes.Type(value = MinimalistMilestone.class, name = "MINIMALIST"),
        @JsonSubTypes.Type(value = TerranMilestone.class, name = "TERRAN"),
        @JsonSubTypes.Type(value = TriadMasteryMilestone.class, name = "TRIAD_MASTERY")
})
public abstract class Milestone {
    @Getter
    private final Set<String> achievedByPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    @Getter
    private final Map<String, Integer> playerToValue = new ConcurrentHashMap<>();

    public Set<String> getAchievedByPlayers() {
        return achievedByPlayers;
    }

    public Map<String, Integer> getPlayerToValue() {
        return playerToValue;
    }

    @JsonIgnore
    public boolean isAchieved(Player player) {
        return achievedByPlayers.contains(player.getUuid());
    }

    @JsonIgnore
    public boolean isAchieved() {
        return !achievedByPlayers.isEmpty();
    }

    public void setAchieved(Player player) {
        achievedByPlayers.add(player.getUuid());
    }

    public void setValue(Player player, int milestoneValue) {
        playerToValue.put(player.getUuid(), milestoneValue);
    }

    @JsonIgnore
    public abstract MilestoneType getType();

    public abstract boolean isAchievable(Player player, CardService cardService);
    public abstract int getValue(Player player, CardService cardService);
    @JsonIgnore
    public abstract int getMaxValue();
}
