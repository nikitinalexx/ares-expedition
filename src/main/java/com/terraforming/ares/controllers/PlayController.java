package com.terraforming.ares.controllers;

import com.terraforming.ares.dto.ActionsDto;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.request.*;
import com.terraforming.ares.model.turn.DiscardCardsTurn;
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

    @GetMapping("/push/{gameId}")
    public void getAllProjectCards(@PathVariable long gameId) {
        turnService.pushGame(gameId);
    }

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
    public void skipTurn(@RequestBody PlayerRequest playerUuidRequest) {
        turnService.skipTurn(playerUuidRequest.getPlayer());
    }

    @PostMapping("/game/player/game-end/confirm")
    public void confirmGameEnd(@RequestBody PlayerRequest playerUuidRequest) {
        turnService.confirmGameEnd(playerUuidRequest.getPlayer());
    }

    @PostMapping("/game/player/sell")
    public void sellCards(@RequestBody CardsRequest sellCardsRequest) {
        turnService.sellCards(sellCardsRequest.getPlayerUuid(), sellCardsRequest.getCards());
    }

    @PostMapping("/game/player/sell-vp")
    public void sellVp(@RequestBody PlayerRequest playerRequest) {
        turnService.sellVp(playerRequest.getPlayer());
    }

    @PostMapping("/game/player/mulligan")
    public void mulliganCards(@RequestBody CardsRequest mulliganCardsRequest) {
        turnService.mulliganCards(mulliganCardsRequest.getPlayerUuid(), mulliganCardsRequest.getCards());
    }

    @PostMapping("/game/player/sell/last")
    public void sellCardsLastRoundTurn(@RequestBody CardsRequest sellCardsRequest) {
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
                new DiscardCardsTurn(request.getPlayerUuid(), request.getCards(), request.getCards().size(), false, false, List.of()),
                request.getPlayerUuid(),
                request.getCards()
        );
    }

    @PostMapping("/turn/unmi/rt")
    public TurnResponse unmiRtCorporationTurn(@RequestBody PlayerRequest playerUuidRequest) {
        return turnService.unmiRtCorporationTurn(playerUuidRequest.getPlayer());
    }

    @PostMapping("/turn/collect-income")
    public void collectIncome(@RequestBody CollectIncomeRequest collectIncomeRequest) {
        turnService.collectIncomeTurn(collectIncomeRequest.getPlayerUuid(), collectIncomeRequest.getCardId());
    }

    @PostMapping("/turn/pick-extra-bonus")
    public void pickExtraCardTurn(@RequestBody PlayerRequest playerUuidRequest) {
        turnService.pickExtraBonusTurn(playerUuidRequest.getPlayer());
    }

    @PostMapping("/turn/draft-cards")
    public void draftCards(@RequestBody PlayerRequest playerUuidRequest) {
        turnService.draftCards(playerUuidRequest.getPlayer());
    }

    @PostMapping("/turn/forest")
    public void plantForest(@RequestBody PlayerRequest playerUuidRequest) {
        turnService.plantForest(playerUuidRequest.getPlayer());
    }

    @PostMapping("/turn/temperature")
    public void increaseTemperature(@RequestBody PlayerRequest playerUuidRequest) {
        turnService.increaseTemperature(playerUuidRequest.getPlayer());
    }

    @PostMapping("/turn/infrastructure")
    public void increaseInfrastructure(@RequestBody PlayerRequestWithParams playerRequestWithParams) {
        turnService.increaseInfrastructure(playerRequestWithParams.getPlayer(), playerRequestWithParams.getInputParams());
    }

    @PostMapping("/turn/plants-crisis-token")
    public void plantsIntoCrisisToken(@RequestBody PlayerRequest playerUuidRequest) {
        turnService.plantsIntoCrisisToken(playerUuidRequest.getPlayer());
    }

    @PostMapping("/turn/heat-crisis-token")
    public void heatIntoCrisisToken(@RequestBody PlayerRequest playerUuidRequest) {
        turnService.heatIntoCrisisToken(playerUuidRequest.getPlayer());
    }

    @PostMapping("/turn/cards-crisis-token")
    public void cardsIntoCrisisToken(@RequestBody DiscardCardsRequest cardsRequest) {
        turnService.cardsIntoCrisisToken(cardsRequest);
    }

    @PostMapping("/turn/standard")
    public void standardProject(@RequestBody StandardProjectRequest request) {
        turnService.standardProjectTurn(request.getPlayerUuid(), request.getType(), request.getInputParams());
    }

    @PostMapping("/turn/heat-exchange")
    public void exchangeHeatRequest(@RequestBody ExchangeHeatRequest request) {
        turnService.exchangeHeatRequest(request.getPlayerUuid(), request.getValue());
    }

    @PostMapping("/turn/crysis-immediate-choice")
    public void crysisImmediateChoiceRequest(@RequestBody CrysisChoiceRequest crysisImmediateChoiceRequest) {
        turnService.crysisImmediateChoiceTurn(crysisImmediateChoiceRequest);
    }

    @PostMapping("/turn/crysis-persistent-choice")
    public void crysisPersistentChoiceRequest(@RequestBody CrysisChoiceRequest crysisPersistentChoiceRequest) {
        turnService.crysisPersistentChoiceTurn(crysisPersistentChoiceRequest);
    }

    @PostMapping("/turn/crysis-persistent-all")
    public void crysisPersistentAllTurn(@RequestBody PlayerRequest playerUuidRequest) {
        turnService.crysisPersistentAllTurn(playerUuidRequest.getPlayer());
    }

    @PostMapping("/turn/crysis-immediate-all")
    public void crysisImmediateAllTurn(@RequestBody PlayerRequest playerUuidRequest) {
        turnService.crysisImmediateAllTurn(playerUuidRequest.getPlayer());
    }

    @PostMapping("/turn/crisis-dummy-choice")
    public void crysisDummyHandChoiceTurn(@RequestBody CrisisDummyHandChoiceRequest request) {
        turnService.crysisDummyHandChoiceTurn(request);
    }

    @PostMapping("/turn/crisis-vp-to-token")
    public void crisisVpToTokenTurn(@RequestBody CardsRequest sellCardsRequest) {
        turnService.crisisVpToTokenTurn(sellCardsRequest.getPlayerUuid(), sellCardsRequest.getCards());
    }

    @PostMapping("/turn/resolve-ocean-detriment")
    public void resolveOceanDetrimentTurn(@RequestBody CrysisChoiceRequest request) {
        turnService.resolveOceanDetrimentTurn(request);
    }

}
