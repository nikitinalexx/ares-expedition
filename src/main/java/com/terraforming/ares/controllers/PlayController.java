package com.terraforming.ares.controllers;

import com.terraforming.ares.dto.ActionDto;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.payments.Payment;
import com.terraforming.ares.model.request.ChooseCorporationRequest;
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

    public void choosePhase(String playerUuid, int phase) {
        turnService.choosePhaseTurn(playerUuid, phase);
    }

    public void skipTurn(String playerUuid) {
        turnService.skipTurn(playerUuid);
    }

    public void sellCards(String playerUuid, List<Integer> cards) {
        turnService.sellCards(playerUuid, cards);
    }

    @PostMapping("/turn/build/green")
    public void buildGreenProject(String playerUuid,
                                  int cardId,
                                  List<Payment> payments,
                                  @RequestParam(required = false) Map<Integer, List<Integer>> inputParams) {
        turnService.buildGreenProjectCard(playerUuid, cardId, payments, inputParams);
    }

    public void buildBlueRedProject(String playerUuid,
                                    int cardId,
                                    List<Payment> payments,
                                    @RequestParam(required = false) Map<Integer, List<Integer>> inputParams) {
        turnService.buildBlueRedProjectCard(playerUuid, cardId, payments, inputParams);
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
