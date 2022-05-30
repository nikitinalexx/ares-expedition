package com.terraforming.ares.controllers;

import com.terraforming.ares.dto.ActionsDto;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.request.*;
import com.terraforming.ares.model.turn.DiscardCardsTurn;
import com.terraforming.ares.model.turn.DiscardDraftedCardsTurn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.GameService;
import com.terraforming.ares.services.TurnService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ActionsDto getNextAction(@PathVariable String playerUuid) {
        return gameService.getNextActions(playerUuid);
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

    @PostMapping("/game/player/game-end/confirm")
    public void confirmGameEnd(@RequestBody PlayerUuidRequest playerUuidRequest) {
        turnService.confirmGameEnd(playerUuidRequest.getPlayerUuid());
    }

    @PostMapping("/game/player/sell")
    public void sellCards(@RequestBody SellCardsRequest sellCardsRequest) {
        turnService.sellCards(sellCardsRequest.getPlayerUuid(), sellCardsRequest.getCards());
    }

    @PostMapping("/game/player/sell/last")
    public void sellCardsLastRoundTurn(@RequestBody SellCardsRequest sellCardsRequest) {
        turnService.sellCardsLastRoundTurn(sellCardsRequest.getPlayerUuid(), sellCardsRequest.getCards());
    }

    @PostMapping("/turn/build/green")
    public TurnResponse buildGreenProject(@RequestBody BuildProjectRequest buildProjectRequest) {
        return turnService.buildGreenProjectCard(
                buildProjectRequest.getPlayerUuid(),
                buildProjectRequest.getCardId(),
                buildProjectRequest.getPayments(),
                buildProjectRequest.getInputParams()
        );
    }

    @PostMapping("/turn/build/blue-red")
    public TurnResponse buildBlueRedProject(@RequestBody BuildProjectRequest buildProjectRequest) {
        return turnService.buildBlueRedProjectCard(
                buildProjectRequest.getPlayerUuid(),
                buildProjectRequest.getCardId(),
                buildProjectRequest.getPayments(),
                buildProjectRequest.getInputParams()
        );
    }

    @PostMapping("/turn/blue/action")
    public TurnResponse performBlueAction(@RequestBody BlueActionRequest blueActionRequest) {
        return turnService.performBlueAction(
                blueActionRequest.getPlayerUuid(),
                blueActionRequest.getCardId(),
                blueActionRequest.getInputParams()
        );
    }

    @PostMapping("/turn/cards/discard")
    public TurnResponse discardCards(@RequestBody DiscardCardsRequest request) {
        return turnService.discardCards(
                new DiscardCardsTurn(request.getPlayerUuid(), request.getCards(), request.getCards().size(), false),
                request.getPlayerUuid(),
                request.getCards(),
                true
        );
    }

    @PostMapping("/turn/cards/discard/drafted")
    public TurnResponse discardDraftedCards(@RequestBody DiscardCardsRequest request) {
        return turnService.discardCards(
                new DiscardDraftedCardsTurn(request.getPlayerUuid(), request.getCards()),
                request.getPlayerUuid(),
                request.getCards(),
                false
        );
    }

    @PostMapping("/turn/collect-income")
    public void collectIncome(@RequestBody PlayerUuidRequest playerUuidRequest) {
        turnService.collectIncomeTurn(playerUuidRequest.getPlayerUuid());
    }

    @PostMapping("/turn/pick-card")
    public void pickExtraCardTurn(@RequestBody PlayerUuidRequest playerUuidRequest) {
        turnService.pickExtraCardTurn(playerUuidRequest.getPlayerUuid());
    }

    @PostMapping("/turn/draft-cards")
    public void draftCards(@RequestBody PlayerUuidRequest playerUuidRequest) {
        turnService.draftCards(playerUuidRequest.getPlayerUuid());
    }

    @PostMapping("/turn/forest")
    public void plantForest(@RequestBody PlayerUuidRequest playerUuidRequest) {
        turnService.plantForest(playerUuidRequest.getPlayerUuid());
    }

    @PostMapping("/turn/temperature")
    public void increaseTemperature(@RequestBody PlayerUuidRequest playerUuidRequest) {
        turnService.increaseTemperature(playerUuidRequest.getPlayerUuid());
    }

    @PostMapping("/turn/standard")
    public void standardProject(@RequestBody StandardProjectRequest request) {
        turnService.standardProjectTurn(request.getPlayerUuid(), request.getType());
    }

    @PostMapping("/turn/heat-exchange")
    public void exchangeHeatRequest(@RequestBody ExchangeHeatRequest request) {
        turnService.exchangeHeatRequest(request.getPlayerUuid(), request.getValue());
    }

}
