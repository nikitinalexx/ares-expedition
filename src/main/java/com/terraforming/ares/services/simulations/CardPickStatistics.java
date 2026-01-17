package com.terraforming.ares.services.simulations;

import com.terraforming.ares.dataset.GameResult;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CardPickStatistics {

    public static final class CardStats {

        public int timesPicked;
        public long pickedRankSum;
        public long pickedRankCount;

        public int wins;
        public int losses;
        public int draws;

        public int totalGames;

        void onPicked(int pickedRank) {
            timesPicked++;
            pickedRankSum += pickedRank;
            pickedRankCount++;
        }

        void onResult(int result) {
            totalGames++;
            switch (result) {
                case 1 -> wins++;
                case 2 -> losses++;
                case 0 -> draws++;
            }
        }

        public double averagePickedRank() {
            if (pickedRankCount == 0) {
                return 0.0;
            }
            return (double) pickedRankSum / (double) pickedRankCount;
        }

        public double winRate() {
            if (totalGames == 0) {
                return 0.0;
            }
            return (double) wins / (double) totalGames;
        }
    }

    private final Map<Integer, CardStats> statsByCardId = new ConcurrentHashMap<>();

    public void onCardPicked(int cardId, int pickedRank) {
        statsByCardId
                .computeIfAbsent(cardId, id -> new CardStats())
                .onPicked(pickedRank);
    }

    public void collectFromGame(int winner, List<Player> players) {

        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);

            for (Integer cardId : player.getPlayed().getCards()) {
                statsByCardId.computeIfAbsent(cardId, id -> new CardStats()).onResult(winner == 0 ? 0 : (winner == (i + 1) ? 1 : 2));
            }
        }
    }

    public Map<Integer, CardStats> getStats() {
        return statsByCardId;
    }
}
