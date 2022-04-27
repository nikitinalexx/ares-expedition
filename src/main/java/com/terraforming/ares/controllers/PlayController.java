package com.terraforming.ares.controllers;

import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.GameService;
import com.terraforming.ares.services.TurnService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@RestController
@RequiredArgsConstructor
public class PlayController {
    private final GameService gameService;
    private final TurnService turnService;

    @GetMapping("/action/next")
    public String getNextAction(String playerUuid) {
        return gameService.getNextAction(playerUuid);
    }

    @GetMapping("/turns/next")
    public List<TurnType> getPossibleTurns(String playerUuid) {
        return gameService.getPossibleTurns(playerUuid);
    }

    public void chooseCorporation(String playerUuid, int corporationCardId) {
        turnService.chooseCorporationTurn(playerUuid, corporationCardId);
    }

    public void chooseStage(String playerUuid, int stage) {
        turnService.chooseStageTurn(playerUuid, stage);
    }

}
