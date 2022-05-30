package com.terraforming.ares.controllers;

import com.terraforming.ares.dto.*;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.turn.DiscardCardsTurn;
import com.terraforming.ares.model.turn.Turn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.repositories.caching.CachingGameRepository;
import com.terraforming.ares.services.CardFactory;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.GameService;
import com.terraforming.ares.services.WinPointsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@RestController
@RequiredArgsConstructor
@CrossOrigin
public class GameController {
    private final GameService gameService;
    private final CardFactory cardFactory;
    private final CardService cardService;
    private final WinPointsService winPointsService;
    private final CachingGameRepository cachingGameRepository;

    @PostMapping("/game/new")
    public PlayerUuidsDto startNewGame(@RequestBody GameParameters gameParameters) {
        try {
            int playersCount = gameParameters.getPlayerNames().size();
            if (playersCount == 0 || playersCount > 4) {
                throw new IllegalArgumentException("Only 1 to 4 players are supported so far");
            }

            MarsGame marsGame = gameService.startNewGame(gameParameters);

            Map<String, Player> playerNameToPlayer = marsGame.getPlayerUuidToPlayer().values().stream()
                    .collect(Collectors.toMap(
                            Player::getName, Function.identity()
                    ));


            return PlayerUuidsDto.builder()
                    .players(gameParameters.getPlayerNames().stream()
                            .map(
                                    playerName -> PlayerReference.builder()
                                            .name(playerName)
                                            .uuid(playerNameToPlayer.get(playerName).getUuid())
                                            .build()
                            )
                            .collect(Collectors.toList())
                    )
                    .build();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/game/player/{playerUuid}")
    public GameDto getGameByPlayerUuid(@PathVariable String playerUuid) {
        MarsGame game = gameService.getGame(playerUuid);

        Planet phasePlanet = game.getPlanetAtTheStartOfThePhase();

        return GameDto.builder()
                .phase(game.getCurrentPhase())
                .player(buildCurrentPlayer(game.getPlayerByUuid(playerUuid)))
                .temperature(game.getPlanet().getTemperature())
                .phaseTemperature(phasePlanet != null ? phasePlanet.getTemperature() : null)
                .oxygen(game.getPlanet().getOxygen())
                .phaseOxygen(phasePlanet != null ? phasePlanet.getOxygen() : null)
                .oceans(game.getPlanet().getRevealedOceans().stream().map(OceanDto::of).collect(Collectors.toList()))
                .phaseOceans(phasePlanet != null ? phasePlanet.getRevealedOceans().size() : null)
                .otherPlayers(buildOtherPlayers(game, playerUuid))
                .turns(game.getTurns())
                .build();
    }

    @GetMapping("/cache/reset")
    public int resetGameCache() {
        return cachingGameRepository.evictGameCache();
    }

    @GetMapping("/game/short/player/{playerUuid}")
    public GameDtoShort getShortGameByPlayerUuid(@PathVariable String playerUuid) {
        MarsGame game = gameService.getGame(playerUuid);

        Planet phasePlanet = game.getPlanetAtTheStartOfThePhase();

        return GameDtoShort.builder()
                .temperature(game.getPlanet().getTemperature())
                .phaseTemperature(phasePlanet != null ? phasePlanet.getTemperature() : null)
                .oxygen(game.getPlanet().getOxygen())
                .phaseOxygen(phasePlanet != null ? phasePlanet.getOxygen() : null)
                .oceans(game.getPlanet().getRevealedOceans().stream().map(OceanDto::of).collect(Collectors.toList()))
                .phaseOceans(phasePlanet != null ? phasePlanet.getRevealedOceans().size() : null)
                .otherPlayers(buildOtherPlayers(game, playerUuid))
                .build();
    }

    @GetMapping("/projects")
    public List<CardDto> getAllProjectCards() {
        List<Card> corporations = cardFactory.getAllCorporations();
        List<Card> projects = cardFactory.getAllProjects();

        return Stream.of(corporations, projects)
                .flatMap(List::stream)
                .map(CardDto::from)
                .collect(Collectors.toList());
    }

    private List<AnotherPlayerDto> buildOtherPlayers(MarsGame game, String currentPlayerUuid) {
        return game.getPlayerUuidToPlayer().values()
                .stream()
                .filter(player -> !player.getUuid().equals(currentPlayerUuid))
                .map(this::buildAnotherPlayer)
                .collect(Collectors.toList());
    }

    private AnotherPlayerDto buildAnotherPlayer(Player player) {
        return AnotherPlayerDto.builder()
                .playerUuid(player.getUuid())
                .name(player.getName())
                .phase(player.getChosenPhase())
                .winPoints(winPointsService.countWinPoints(player))
                .mc(player.getMc())
                .mcIncome(player.getMcIncome())
                .cardIncome(player.getCardIncome())
                .heat(player.getHeat())
                .heatIncome(player.getHeatIncome())
                .plants(player.getPlants())
                .plantsIncome(player.getPlantsIncome())
                .steelIncome(player.getSteelIncome())
                .titaniumIncome(player.getTitaniumIncome())
                .terraformingRating(player.getTerraformingRating())
                .forests(player.getForests())
                .hand(player.getHand().getCards().stream().map(cardService::getCard).map(CardDto::from).collect(Collectors.toList()))
                .played(player.getPlayed().getCards().stream().map(cardService::getCard).map(CardDto::from).collect(Collectors.toList()))
                .cardResources(getPlayerCardResources(player))
                .build();
    }

    private PlayerDto buildCurrentPlayer(Player player) {
        Deck corporations = player.getCorporations();

        return PlayerDto.builder()
                .playerUuid(player.getUuid())
                .name(player.getName())
                .corporations(corporations.getCards().stream().map(cardService::getCard).map(CardDto::from).collect(Collectors.toList()))
                .hand(player.getHand().getCards().stream().map(cardService::getCard).map(CardDto::from).collect(Collectors.toList()))
                .played(player.getPlayed().getCards().stream().map(cardService::getCard).map(CardDto::from).collect(Collectors.toList()))
                .corporationId(player.getSelectedCorporationCard())
                .phase(player.getChosenPhase())
                .previousPhase(player.getPreviousChosenPhase())
                .mc(player.getMc())
                .mcIncome(player.getMcIncome())
                .cardIncome(player.getCardIncome())
                .heat(player.getHeat())
                .heatIncome(player.getHeatIncome())
                .plants(player.getPlants())
                .plantsIncome(player.getPlantsIncome())
                .steelIncome(player.getSteelIncome())
                .titaniumIncome(player.getTitaniumIncome())
                .nextTurn(buildTurnDto(player.getNextTurn()))
                .cardResources(getPlayerCardResources(player))
                .activatedBlueCards(player.getActivatedBlueCards().getCards())
                .activatedBlueActionTwice(player.isActivatedBlueActionTwice())
                .terraformingRating(player.getTerraformingRating())
                .winPoints(winPointsService.countWinPoints(player))
                .forests(player.getForests())
                .builtSpecialDesignLastTurn(player.isBuiltSpecialDesignLastTurn())
                .builtWorkCrewsLastTurn(player.isBuiltWorkCrewsLastTurn())
                .canBuildAnotherGreenWith9Discount(player.isCanBuildAnotherGreenWith9Discount())
                .assortedEnterprisesDiscount(player.isAssortedEnterprisesDiscount())
                .selfReplicatingDiscount(player.isSelfReplicatingDiscount())
                .build();
    }

    private Map<Integer, Integer> getPlayerCardResources(Player player) {
        return player.getPlayed().getCards().stream().map(cardService::getCard)
                .filter(card -> card.getCollectableResource() != CardCollectableResource.NONE)
                .collect(Collectors.toMap(
                        Card::getId,
                        card -> player.getCardResourcesCount().get(card.getClass())
                ));
    }

    private TurnDto buildTurnDto(Turn turn) {
        if (turn != null && turn.getType() == TurnType.DISCARD_CARDS) {
            DiscardCardsTurn discardCardsTurnDto = (DiscardCardsTurn) turn;
            return DiscardCardsTurnDto.builder()
                    .size(discardCardsTurnDto.getSize())
                    .onlyFromSelectedCards(discardCardsTurnDto.isOnlyFromSelectedCards())
                    .cards(
                            discardCardsTurnDto.getCards().stream().map(cardService::getCard).map(CardDto::from).collect(Collectors.toList())
                    ).build();
        }
        return null;

    }

}
