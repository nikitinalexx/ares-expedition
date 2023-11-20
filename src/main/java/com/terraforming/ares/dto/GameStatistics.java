package com.terraforming.ares.dto;

import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 03.12.2022
 */
@Getter
public class GameStatistics {
    public static final int MAX_TURNS_TO_CONSIDER = 50;

    private final Map<Integer, Map<Integer, Integer>> turnToWinCardsOccurence;
    private final Map<Integer, Map<Integer, Integer>> turnToCardsOccurence;
    private final Map<Integer, Integer> corporationToWin = new HashMap<>();
    private final Map<Integer, Integer> corporationToOccurence = new HashMap<>();


    private long totalGames = 0;
    private long totalTurnsCount = 0;
    private long totalPointsCount = 0;

    private final List<Long> winners;


    public GameStatistics() {
        turnToWinCardsOccurence = new HashMap<>();
        turnToCardsOccurence = new HashMap<>();

        for (int turn = 1; turn <= MAX_TURNS_TO_CONSIDER; turn++) {
            turnToWinCardsOccurence.put(turn, new HashMap<>());
            turnToCardsOccurence.put(turn, new HashMap<>());
        }

        winners = new ArrayList<>(List.of(0L, 0L, 0L, 0L));
    }

    public synchronized void cardOccured(int turn, int card) {
        if (turn == 0) {
            turn++;
        }
        Map<Integer, Integer> cardIdToOccurence = turnToCardsOccurence.get(turn);

        cardIdToOccurence.compute(card, (cardId, occurence) -> {
            if (occurence == null) {
                occurence = 0;
            }
            return occurence + 1;
        });

    }

    public synchronized void winCardOccured(int turn, int card) {
        if (turn == 0) {
            turn++;
        }
        Map<Integer, Integer> cardIdToOccurence = turnToWinCardsOccurence.get(turn);

        cardIdToOccurence.compute(card, (cardId, occurence) -> {
            if (occurence == null) {
                occurence = 0;
            }
            return occurence + 1;
        });
    }

    public synchronized void corporationWon(int corporation) {
        corporationToWin.compute(corporation, (key, value) -> {
            if (value == null) {
                value = 1;
            } else {
                value++;
            }
            return value;
        });
    }

    public synchronized void corporationOccured(int corporation) {
        corporationToOccurence.compute(corporation, (key, value) -> {
            if (value == null) {
                value = 1;
            } else {
                value++;
            }
            return value;
        });
    }

    public synchronized void addTotalGames(int games) {
        totalGames += games;
    }

    public synchronized void addTotalTurnsCount(int turnsCount) {
        totalTurnsCount += turnsCount;
    }

    public synchronized void addTotalPointsCount(int points) {
        totalPointsCount += points;
    }

    public synchronized void addPlayerWins(int index) {
        winners.set(index, winners.get(index) + 1);
    }


}
