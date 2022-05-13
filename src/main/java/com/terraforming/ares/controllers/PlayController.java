package com.terraforming.ares.controllers;

import com.terraforming.ares.dto.ActionDto;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.payments.Payment;
import com.terraforming.ares.model.request.*;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.GameService;
import com.terraforming.ares.services.TurnService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PlayController {
    private final GameService gameService;
    private final TurnService turnService;

    @GetMapping("/action/next/{playerUuid}")
    public ActionDto getNextAction(@PathVariable String playerUuid) {
        return new ActionDto(gameService.getNextAction(playerUuid));
    }

    @GetMapping("/turns/next/{playerUuid}")
    public List<TurnType> getPossibleTurns(@PathVariable String playerUuid) {
        return gameService.getPossibleTurns(playerUuid);
    }

    @PostMapping("/game/player/corporation")
    public void chooseCorporation(@RequestBody ChooseCorporationRequest chooseCorporationRequest) {
        turnService.chooseCorporationTurn(chooseCorporationRequest);
    }

    @PostMapping("/game/player/phase")
    public void choosePhase(@RequestBody ChoosePhaseRequest choosePhaseRequest) {
        turnService.choosePhaseTurn(choosePhaseRequest.getPlayerUuid(), choosePhaseRequest.getPhaseId());
    }

    @PostMapping("/game/player/skip")
    public void skipTurn(@RequestBody PlayerUuidRequest playerUuidRequest) {
        turnService.skipTurn(playerUuidRequest.getPlayerUuid());
    }

    @PostMapping("/game/player/sell")
    public void sellCards(@RequestBody SellCardsRequest sellCardsRequest) {
        turnService.sellCards(sellCardsRequest.getPlayerUuid(), sellCardsRequest.getCards());
    }

    @PostMapping("/turn/build/green")
    public void buildGreenProject(@RequestBody BuildProjectRequest buildProjectRequest) {
        turnService.buildGreenProjectCard(
                buildProjectRequest.getPlayerUuid(),
                buildProjectRequest.getCardId(),
                buildProjectRequest.getPayments(),
                buildProjectRequest.getInputParams()
        );
    }

    @PostMapping("/turn/build/blue-red")
    public void buildBlueRedProject(@RequestBody BuildProjectRequest buildProjectRequest) {
        turnService.buildBlueRedProjectCard(
                buildProjectRequest.getPlayerUuid(),
                buildProjectRequest.getCardId(),
                buildProjectRequest.getPayments(),
                buildProjectRequest.getInputParams()
        );
    }

    public TurnResponse performBlueAction(String playerUuid,
                                          int projectId,
                                          @RequestParam(required = false) List<Integer> inputParams) {
        return turnService.performBlueAction(playerUuid, projectId, inputParams);
    }

    public TurnResponse discardCards(String playerUuid, List<Integer> cards) {
        return turnService.discardCards(playerUuid, cards);
    }

}
