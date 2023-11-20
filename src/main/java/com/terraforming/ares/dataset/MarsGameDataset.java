package com.terraforming.ares.dataset;

import com.terraforming.ares.model.Player;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 26.11.2022
 */
@Getter
public class MarsGameDataset {
    private final List<String> players;
    private final Map<String, List<MarsGameRow>> playerToRows = new HashMap<>();

    public MarsGameDataset(Map<String, Player> playerUuidToPlayer) {
        this.players = new ArrayList<>(playerUuidToPlayer.keySet());
        if (this.players.size() != 2) {
            throw new IllegalStateException("Only 2 players supported for a model so far");
        }
        playerUuidToPlayer.keySet().forEach(playerUuid -> playerToRows.put(playerUuid, new ArrayList<>()));
    }

    public void markWinner(String player) {
        playerToRows.forEach(
                (p, data) -> {
                    int result = player.equals(p) ? 1 : 0;
                    data.forEach(dataRow -> dataRow.setWinner(result));
                }
        );
    }

    public List<MarsGameRow> getFirstPlayerRows() {
        return playerToRows.get(players.get(0));
    }

    public List<MarsGameRow> getSecondPlayerRows() {
        return playerToRows.get(players.get(1));
    }

}
