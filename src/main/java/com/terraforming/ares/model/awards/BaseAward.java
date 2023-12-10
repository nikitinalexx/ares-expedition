package com.terraforming.ares.model.awards;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by oleksii.nikitin
 * Creation date 13.06.2022
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CelebrityAward.class, name = "CELEBRITY"),
        @JsonSubTypes.Type(value = CollectorAward.class, name = "COLLECTOR"),
        @JsonSubTypes.Type(value = GeneratorAward.class, name = "GENERATOR"),
        @JsonSubTypes.Type(value = IndustrialistAward.class, name = "INDUSTRIALIST"),
        @JsonSubTypes.Type(value = ProjectManagerAward.class, name = "PROJECT_MANAGER"),
        @JsonSubTypes.Type(value = ResearcherAward.class, name = "RESEARCHER"),
        @JsonSubTypes.Type(value = BotanistAward.class, name = "BOTANIST"),
        @JsonSubTypes.Type(value = FloraHarvestAward.class, name = "FLORA_HARVEST"),
        @JsonSubTypes.Type(value = BuilderAward.class, name = "BUILDER"),
        @JsonSubTypes.Type(value = GardenerAward.class, name = "GARDENER"),
        @JsonSubTypes.Type(value = CriterionAward.class, name = "CRITERION"),
})
public abstract class BaseAward {
    @Setter
    @Getter
    private Map<String, Integer> winPoints = new ConcurrentHashMap<>();

    @JsonIgnore
    public abstract AwardType getType();

    protected abstract Set<Player> getFirstPlaceWinners(Collection<Player> players, CardService cardService);

    protected abstract Set<Player> getSecondPlaceWinners(Collection<Player> players, CardService cardService);

    @JsonIgnore
    public Map<String, Integer> getAwards() {
        return new HashMap<>(winPoints);
    }

    @JsonIgnore
    public Integer getPlayerWinPoints(Player player) {
        return winPoints.getOrDefault(player.getUuid(), 0);
    }

    public void assignWinners(Collection<Player> players, CardService cardService) {
        if (players.size() <= 1) {
            return;
        }

        Set<Player> firstPlaceWinners = getFirstPlaceWinners(players, cardService);

        addPoints(firstPlaceWinners, (firstPlaceWinners.size() > 1) ? 4 : 5);

        if (firstPlaceWinners.size() > 1) {
            return;
        }

        Set<Player> secondPlaceWinners = getSecondPlaceWinners(players, cardService);

        addPoints(secondPlaceWinners, (secondPlaceWinners.size() > 1) ? 1 : 2);
    }

    private void addPoints(Set<Player> players, int points) {
        players.forEach(player -> winPoints.put(player.getUuid(), points));
    }

    @JsonIgnore
    public abstract int getMaxValue();


}
