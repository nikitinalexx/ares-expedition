package com.terraforming.ares.services;

import com.terraforming.ares.mars.CrysisData;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.parameters.ParameterColor;
import com.terraforming.ares.model.turn.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by oleksii.nikitin
 * Creation date 27.02.2023
 */
@Service
@RequiredArgsConstructor
public class StateTransitionService {
    private final CardService cardService;
    private final ShuffleService shuffleService;
    private final CrisisDetrimentService crisisDetrimentService;
    private final WinPointsService winPointsService;
    private final MarsContextProvider contextProvider;
    private final Random random = new Random();

    public void performStateTransferIntoResolveDetrimentTokens(MarsGame game) {
        if (game.getStateType() == StateType.GAME_END) {
            return;
        }

        final MarsContext context = contextProvider.provide(game);

        final Planet planet = game.getPlanet();
        if (planet.getOxygenColor() == ParameterColor.P
                || planet.getTemperatureColor() == ParameterColor.P
                || planet.isValidOcean(ParameterColor.P)
                || game.getCrysisData().getCrysisCards().getCards().isEmpty()) {
            game.setStateType(StateType.GAME_END, context,
                    game.getCrysisData().getCrysisCards().getCards().isEmpty()
                            ? "Crisis deck became empty"
                            : "One of the global parameters became Purple");
            return;
        }

        crisisDetrimentService.updateDetrimentTokens(game);

        if (crisisDetrimentService.detrimentFromOceanIsPresent(game)) {
            List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());

            final Player player = players.get(random.nextInt(players.size()));
            player.addNextTurn(new ResolveOceanDetrimentTurn(player.getUuid(), Map.of(), true));

            game.setStateType(StateType.RESOLVE_OCEAN_DETRIMENT, context);
        } else {
            performStateTransferIntoResolveCrysis(game);
        }
    }

    public void performStateTransferIntoResolveCrysis(MarsGame game) {
        if (game.getStateType() == StateType.GAME_END) {
            return;
        }

        final MarsContext context = contextProvider.provide(game);

        final List<CrysisCard> crysisCards = game.getCrysisData()
                .getOpenedCards()
                .stream()
                .map(cardService::getCrysisCard)
                .collect(Collectors.toList());

        if (crysisCards.stream().anyMatch(card -> !card.persistentEffectRequiresChoice()
                && card.cardAction() != CrysisCardAction.PLAY_AND_DISCARD_CRYSIS)) {
            game.setStateType(StateType.RESOLVE_PERSISTENT_ALL, context);
        } else {
            performStateTransferIntoResolveCrysisWithChoice(game);
        }
    }


    public void performStateTransferIntoCrisisDrawDummyHand(MarsGame game) {
        if (game.getStateType() == StateType.GAME_END) {
            return;
        }

        final CrysisData crysisData = game.getCrysisData();

        List<CrisisDummyCard> result = new ArrayList<>();

        List<CrisisDummyCard> availableCards = new ArrayList<>(CrisisDummyCard.ALL_SOLO_DUMMY_CARDS);
        if (!CollectionUtils.isEmpty(crysisData.getUsedDummyCards())) {
            availableCards.removeAll(crysisData.getUsedDummyCards());
        }

        shuffleService.shuffle(availableCards);

        if (availableCards.isEmpty()) {
            availableCards = new ArrayList<>(CrisisDummyCard.ALL_SOLO_DUMMY_CARDS);
            shuffleService.shuffle(availableCards);
            crysisData.setUsedDummyCards(new ArrayList<>());
        } else if (availableCards.size() == 1) {
            CrisisDummyCard availableCard = availableCards.get(0);
            availableCards = new ArrayList<>(CrisisDummyCard.ALL_SOLO_DUMMY_CARDS);
            shuffleService.shuffle(availableCards);
            availableCards.remove(availableCard);
            crysisData.setUsedDummyCards(new ArrayList<>());
            result.add(availableCard);
        }

        for (int i = result.size(); i < 2; i++) {
            result.add(availableCards.get(0));
            availableCards.remove(0);
        }

        crysisData.getUsedDummyCards().addAll(result);
        crysisData.setCurrentDummyCards(result);
        crysisData.setChosenDummyPhases(List.of());

        game.setStateType(StateType.CRISIS_DRAW_DUMMY_HAND, contextProvider.provide(game));
    }

    public void performStateTransferIntoResolveCrysisWithChoice(MarsGame game) {
        if (game.getStateType() == StateType.GAME_END) {
            return;
        }

        final List<CrysisCard> crysisCards = game.getCrysisData()
                .getOpenedCards()
                .stream()
                .map(cardService::getCrysisCard)
                .collect(Collectors.toList());

        if (crysisCards.stream().anyMatch(CrysisCard::persistentEffectRequiresChoice)) {
            List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
            shuffleService.shuffle(players);

            final List<CrysisCard> cardsWithChoice = crysisCards.stream()
                    .filter(CrysisCard::persistentEffectRequiresChoice)
                    .collect(Collectors.toList());

            for (int i = 0; i < cardsWithChoice.size(); i++) {
                final CrysisCard crysisCard = cardsWithChoice.get(i);
                final Player player = players.get(i % players.size());
                player.addNextTurn(new CrysisCardPersistentChoiceTurn(player.getUuid(), crysisCard.getId(), Map.of(), true));
            }
            game.setStateType(StateType.RESOLVE_CRYSIS_WITH_CHOICE, contextProvider.provide(game));
        } else {
            performStateTransferIntoTakeNextCrysisCard(game);
        }
    }

    public void performStateTransferIntoTakeNextCrysisCard(MarsGame game) {
        if (game.getStateType() == StateType.GAME_END) {
            return;
        }

        final CrysisData crysisData = game.getCrysisData();
        if (crysisData.getEasyModeTurnsLeft() != 0) {
            crysisData.setEasyModeTurnsLeft(crysisData.getEasyModeTurnsLeft() - 1);
            performStateTransferIntoCrisisDrawDummyHand(game);
            return;
        }

        final List<Integer> openedCrysisCards = crysisData.getCrysisCards().dealCards(1);

        final CrysisCard crysisCard = cardService.getCrysisCard(openedCrysisCards.get(0));

        List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());

        for (Player player : players) {
            if (crysisCard.immediateEffectRequiresChoice()) {
                player.addNextTurn(new CrysisCardImmediateChoiceTurn(player.getUuid(), crysisCard.getId(), Map.of(), true));
            } else {
                player.addNextTurn(new CrysisImmediateAllTurn(player.getUuid(), true));
            }
        }
        game.setStateType(StateType.DRAW_CRYSIS_CARD, contextProvider.provide(game));

        crysisData.getOpenedCards().add(crysisCard.getId());
        crysisData.getCardToTokensCount().put(crysisCard.getId(), crysisCard.initialTokens());
    }

    public void performStateTransferFromPhase(MarsGame game, int fromState) {
        if (game.gameEndCondition()) {
            game.setStateType(StateType.GAME_END, contextProvider.provide(game));
            return;
        }

        if (fromState == 0 && game.isDummyHandMode()) {
            game.updateDummyHand(shuffleService);
        }

        Set<Integer> pickedPhases = game.getPlayerUuidToPlayer()
                .values()
                .stream()
                .map(Player::getChosenPhase)
                .collect(Collectors.toSet());
        if (game.isDummyHandMode() && fromState >= 0) {
            pickedPhases.add(game.getCurrentDummyHand());
        }
        if (game.isCrysis() && fromState >= 0) {
            pickedPhases.addAll(game.getCrysisData().getChosenDummyPhases());
        }

        if (!crisisDetrimentService.canPlayFirstPhase(game)) {
            pickedPhases.remove(1);
        }

        if (fromState <= 1 && pickedPhases.contains(1)) {
            transferToPhaseOne(game);
        } else if (fromState <= 2 && pickedPhases.contains(2)) {
            transferToPhaseTwo(game);
        } else if (fromState <= 3 && pickedPhases.contains(3)) {
            game.setStateType(StateType.PERFORM_BLUE_ACTION, contextProvider.provide(game));
            game.getPlayerUuidToPlayer().values().forEach(
                    player -> {
                        if (player.getChosenPhase() == 3 && player.hasPhaseUpgrade(Constants.PHASE_3_UPGRADE_REVEAL_CARDS)) {
                            final List<Integer> cards = cardService.dealCards(game, 3);
                            for (Integer cardId : cards) {
                                final Card card = cardService.getCard(cardId);
                                if (card.getColor() == CardColor.BLUE || card.getColor() == CardColor.RED) {
                                    player.getHand().addCard(cardId);
                                }
                            }
                        }
                    });
        } else if (fromState <= 4 && pickedPhases.contains(4)) {
            game.setStateType(StateType.COLLECT_INCOME, contextProvider.provide(game));
        } else if (fromState <= 5 && pickedPhases.contains(5)) {
            game.setStateType(StateType.DRAFT_CARDS, contextProvider.provide(game));
            game.getPlayerUuidToPlayer().values().forEach(player -> player.addNextTurn(new DraftCardsTurn(player.getUuid())));
        } else if (game.getPlayerUuidToPlayer().values().stream().anyMatch(player -> player.getHand().size() > Constants.MAX_HAND_SIZE_LAST_ROUND)) {
            game.setStateType(StateType.SELL_EXTRA_CARDS, contextProvider.provide(game));
        } else {
            if (game.isCrysis()) {
                final Planet planet = game.getPlanet();
                if (game.getCrysisData()
                        .getOpenedCards()
                        .stream()
                        .map(cardService::getCrysisCard)
                        .anyMatch(CrysisCard::endGameCard)
                        && planet.isOceansMax() && planet.isTemperatureMax() && planet.isOxygenMax()) {
                    game.getCrysisData().setWonGame(true);
                    game.setStateType(StateType.GAME_END, contextProvider.provide(game),
                            "Congratulations! You have successfully dealt with the crisis and saved the Mars. " +
                                    "Your quick thinking, strategic planning, and unwavering determination have paid off, and the Mars is a safer place because of your efforts. " +
                                    "Well done, and thank you for playing!");
                } else {
                    if (winPointsService.countCrysisWinPoints(game) >= 2
                            && game.getCrysisData().getCardToTokensCount().values().stream().anyMatch(value -> value != 0)) {
                        game.setStateType(StateType.CRISIS_END_STEP, contextProvider.provide(game));
                    } else {
                        performStateTransferIntoResolveDetrimentTokens(game);
                    }
                }
            } else {
                game.setStateType(StateType.PICK_PHASE, contextProvider.provide(game));
            }
        }
    }

    private void transferToPhaseOne(MarsGame game) {
        game.setStateType(StateType.BUILD_GREEN_PROJECTS, contextProvider.provide(game));

        for (Player player : game.getPlayerUuidToPlayer().values()) {
            List<BuildDto> builds = new ArrayList<>();

            if (player.getChosenPhase() != 1 || !crisisDetrimentService.canGetFirstPhaseBonus(game)) {
                builds.add(new BuildDto(BuildType.GREEN, 0));
            } else {
                if (player.hasPhaseUpgrade(Constants.PHASE_1_UPGRADE_DISCOUNT)) {
                    builds.add(new BuildDto(BuildType.GREEN, 6));
                } else if (player.hasPhaseUpgrade(Constants.PHASE_1_UPGRADE_BUILD_EXTRA)) {
                    builds.add(new BuildDto(BuildType.GREEN, 3));
                    builds.add(new BuildDto(BuildType.GREEN, 0, 12));
                } else {
                    builds.add(new BuildDto(BuildType.GREEN, 3));
                }
            }
            player.setBuilds(builds);
        }
    }

    private void transferToPhaseTwo(MarsGame game) {
        game.setStateType(StateType.BUILD_BLUE_RED_PROJECTS, contextProvider.provide(game));

        for (Player player : game.getPlayerUuidToPlayer().values()) {
            List<BuildDto> builds = new ArrayList<>();
            builds.add(new BuildDto(BuildType.BLUE_RED));

            if (player.getChosenPhase() == 2) {
                if (player.hasPhaseUpgrade(Constants.PHASE_2_UPGRADE_PROJECT_AND_MC)) {
                    builds.add(new BuildDto(BuildType.BLUE_RED_OR_MC));
                } else if (player.hasPhaseUpgrade(Constants.PHASE_2_UPGRADE_PROJECT_AND_CARD)) {
                    player.getHand().addCards(cardService.dealCards(game, 1));
                    builds.add(new BuildDto(BuildType.BLUE_RED));
                } else {
                    builds.add(new BuildDto(BuildType.BLUE_RED_OR_CARD));
                }
            }
            player.setBuilds(builds);
        }
    }

}
