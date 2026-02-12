package com.terraforming.ares.services.ai.network2;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.MarsContextProvider;
import com.terraforming.ares.services.ai.AiDiscoveryDecisionService;
import com.terraforming.ares.services.ai.advanced.IDataCollect;
import com.terraforming.ares.services.ai.dl4j.NNService;
import com.terraforming.ares.services.ai.dl4j.Prediction;
import com.terraforming.ares.services.ai.network2.buildParams.AiInputOptimizer;
import com.terraforming.ares.services.ai.network2.buildParams.OptimizedInputDecisions;
import com.terraforming.ares.services.ai.network2.buildParams.SharedInputAnalysis;
import com.terraforming.ares.services.ai.network2.dto.CardWithChanceAndInput;
import com.terraforming.ares.services.ai.network2.dto.CardWithChanceModifier;
import com.terraforming.ares.services.ai.network2.projection.CardProjectionService;
import com.terraforming.ares.services.ai.network2.projection.Scenario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class Network2CorporationAndMulliganService {
    private static final int MULLIGAN_CARDS_TO_CHECK_FROM_DECK = 100;
    private final CardService cardService;
    private final MarsContextProvider marsContextProvider;
    private final AiDiscoveryDecisionService aiDiscoveryDecisionService;
    private final Network2ProjectBuildService network2ProjectBuildService;
    private final Network2CorporationInputService network2CorporationInputService;
    private final AiInputOptimizer optimizer;
    private final CardProjectionService cardProjectionService;

    private final IDataCollect iDataCollect;
    private final NNService nnService;

    record HandEvaluation(
            Map<Integer, Double> cardIdToDelta,
            double handValue,
            double avgDeckDelta
    ) {
    }

    public record CorpEvaluation(
            int corporationId,
            OptimizedInputDecisions inputDecisions
    ) {}


    public List<Integer> getCardsToDiscardForMulligan(MarsGame game, String playerUuid) {
        game = new MarsGame(game);
        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        Player player = game.getPlayerByUuid(playerUuid);
        Player anotherPlayer = players.get(0) == player ? players.get(1) : players.get(0);

        SharedInputAnalysis sharedInputAnalysis = network2CorporationInputService.analyzeCorporationsForSharedInput(player.getCorporations().getCards().stream().map(cardService::getCard)
                .map(card -> card.getCardMetadata().getCardAction()).collect(Collectors.toSet())
        );
        OptimizedInputDecisions optimizedDecisions = (sharedInputAnalysis == null ? null : optimizer.optimizeInputDecisions(game, player, sharedInputAnalysis));

        anotherPlayer.setMc(60);//TODO NEED TO CHECK CHANCE
        //TODO not all corporations are projected well


        MarsGame corp1Game = projectPlayerBuildCorporationExperiment(game, player, player.getCorporations().getCards().getFirst(), optimizedDecisions);
        MarsGame corp2Game = projectPlayerBuildCorporationExperiment(game, player, player.getCorporations().getCards().getLast(), optimizedDecisions);

        List<Prediction> predictions = nnService.predictBatch(List.of(iDataCollect.collectData(corp1Game, corp1Game.getPlayerByUuid(playerUuid)), iDataCollect.collectData(corp2Game, corp2Game.getPlayerByUuid(playerUuid))), player);
        List<Double> baseChances = List.of(predictions.getFirst().baseProb, predictions.getLast().baseProb);
        if (dominates(baseChances.getFirst(), baseChances.getLast())) {
            return discardCardsOnlyForOneCorp(evaluateHandForCorp(corp1Game, playerUuid, baseChances.getFirst()), corp1Game.getPlayerByUuid(playerUuid).getHand().getCards());
        } else if (dominates(baseChances.getLast(), baseChances.getFirst())) {
            return discardCardsOnlyForOneCorp(evaluateHandForCorp(corp2Game, playerUuid, baseChances.getLast()), corp2Game.getPlayerByUuid(playerUuid).getHand().getCards());
        }

        return discardCardsForTwoCorps(
                corp1Game,
                corp2Game,
                playerUuid,
                baseChances.getFirst(),
                baseChances.getLast()
        );
    }

    private List<Integer> discardCardsOnlyForOneCorp(
            HandEvaluation eval,
            List<Integer> hand
    ) {
        List<Integer> discard = new ArrayList<>();

        for (Integer cardId : hand) {
            double delta = eval.cardIdToDelta().get(cardId);
            if (delta < eval.avgDeckDelta) {
                discard.add(cardId);
            }
        }

        return discard;
    }

    private HandEvaluation evaluateHandForCorp(
            MarsGame game,
            String playerUuid,
            double baseProb
    ) {
        Player player = game.getPlayerByUuid(playerUuid);

        List<Integer> originalHand = new ArrayList<>(player.getHand().getCards());

        // ==== 1. Base prob without each hand card ====
        List<float[]> statesWithoutCards = new ArrayList<>();
        for (Integer cardId : originalHand) {
            player.getHand().removeCard(cardId);
            statesWithoutCards.add(iDataCollect.collectData(game, player));
            player.getHand().addCard(cardId);
        }

        List<Prediction> preds = nnService.predictBatch(statesWithoutCards, player);

        Map<Integer, Double> cardIdToBaseChance = new HashMap<>();
        for (int i = 0; i < originalHand.size(); i++) {
            cardIdToBaseChance.put(originalHand.get(i), preds.get(i).baseProb);
        }

        // ==== 2. Projections (hand + deck sample) ====
        Deck deck = cardService.createProjectsDeck(game.getExpansions());
        deck.removeCards(player.getHand().getCards());

        List<Card> cardsToCheck = Stream.concat(
                originalHand.stream().map(cardService::getCard),
                deck.dealCards(Math.min(deck.size(), MULLIGAN_CARDS_TO_CHECK_FROM_DECK))
                        .stream()
                        .map(cardService::getCard)
        ).toList();

        List<CardWithChanceModifier> projections =
                network2ProjectBuildService.getBestCardProjectionsIgnoreRequirements(
                        game,
                        player,
                        cardsToCheck
                );

        Map<Integer, Double> handDeltas = new HashMap<>();
        double totalDeckDelta = 0;
        int deckCount = 0;

        for (CardWithChanceModifier p : projections) {
            int cardId = p.getCard().getId();
            boolean isHandCard = originalHand.contains(cardId);

            double delta = (p.getChance() - (isHandCard ? cardIdToBaseChance.get(cardId) : baseProb)) * p.getModifier();

            if (originalHand.contains(cardId)) {
                handDeltas.put(cardId, delta);
            } else {
                totalDeckDelta += delta;
                deckCount++;
            }
        }

        double avgDeckDelta = deckCount > 0
                ? totalDeckDelta / deckCount
                : 0;

        double handValue = handDeltas.values().stream()
                .mapToDouble(d -> Math.max(0, d))
                .sum();

        return new HandEvaluation(handDeltas, handValue, avgDeckDelta);
    }

    private List<Integer> discardCardsForTwoCorps(
            MarsGame corpAGame,
            MarsGame corpBGame,
            String playerUuid,
            double baseProbA,
            double baseProbB
    ) {
        HandEvaluation handA = evaluateHandForCorp(corpAGame, playerUuid, baseProbA);
        HandEvaluation handB = evaluateHandForCorp(corpBGame, playerUuid, baseProbB);

        Player player = corpAGame.getPlayerByUuid(playerUuid);
        List<Integer> originalHand = new ArrayList<>(player.getHand().getCards());

        // ==== 1. A доминирует ====
        if (handDominates(handA.handValue(), handB.handValue())) {
            return discardCardsOnlyForOneCorp(handA, originalHand);
        }

        // ==== 2. B доминирует ====
        if (handDominates(handB.handValue(), handA.handValue())) {
            return discardCardsOnlyForOneCorp(handB, originalHand);
        }

        // ==== 3. Универсальный режим ====
        List<Integer> discard = new ArrayList<>();
        double threshold = (handA.avgDeckDelta() + handB.avgDeckDelta()) * 0.5;

        for (Integer cardId : originalHand) {
            double bestDelta = Math.max(
                    handA.cardIdToDelta().getOrDefault(cardId, 0.0),
                    handB.cardIdToDelta().getOrDefault(cardId, 0.0)
            );

            if (bestDelta < threshold) {
                discard.add(cardId);
            }
        }

        return discard;
    }


    private boolean handDominates(double handA, double handB) {
        return (handA - handB >= 0.08) || ((handA - handB) >= 0.5 * Math.max(handB, 0.05));
    }

    private boolean dominates(double a, double b) {
        if (true) {
            return false;//TODO
        }
        double diff = a - b;
        if (diff >= 0.12) return true;

        double relative = diff / (1.0 - Math.min(a, b));
        return relative >= 0.35;
    }


    private MarsGame projectPlayerBuildCorporationExperiment(MarsGame game, Player player, int selectedCorporationId, OptimizedInputDecisions optimizedDecisions) {
        game = new MarsGame(game);
        player = game.getPlayerByUuid(player.getUuid());
        List<Integer> originalHandBeforeCorporationBuild = new ArrayList<>(player.getHand().getCards());

        player.setSelectedCorporationCard(selectedCorporationId);
        player.setMulligan(false);
        player.getPlayed().addCard(selectedCorporationId);

        Card card = cardService.getCard(selectedCorporationId);
        final MarsContext marsContext = marsContextProvider.provide(game, player);
        card.buildProject(marsContext);
        if (card.onBuiltEffectApplicableToItself()) {
            card.postProjectBuiltEffect(marsContext, card, network2CorporationInputService.getCorporationInput(game, player, card.getCardMetadata().getCardAction(), optimizedDecisions));
        }
        player.getHand().getCards().clear();
        player.getHand().getCards().addAll(originalHandBeforeCorporationBuild);

        return game;
    }

    public CorpEvaluation chooseCorporation(MarsGame game, String playerUuid) {
        game = new MarsGame(game);
        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        Player player = game.getPlayerByUuid(playerUuid);
        Player anotherPlayer = players.get(0) == player ? players.get(1) : players.get(0);

        SharedInputAnalysis sharedInputAnalysis = network2CorporationInputService.analyzeCorporationsForSharedInput(player.getCorporations().getCards().stream().map(cardService::getCard)
                .map(card -> card.getCardMetadata().getCardAction()).collect(Collectors.toSet())
        );
        OptimizedInputDecisions optimizedDecisions = (sharedInputAnalysis == null ? null : optimizer.optimizeInputDecisions(game, player, sharedInputAnalysis));
        anotherPlayer.setMc(60);//TODO NEED TO CHECK CHANCE

        MarsGame corp1Game = projectPlayerBuildCorporationExperiment(game, player, player.getCorporations().getCards().getFirst(), optimizedDecisions);
        MarsGame corp2Game = projectPlayerBuildCorporationExperiment(game, player, player.getCorporations().getCards().getLast(), optimizedDecisions);

        List<Prediction> predictions = nnService.predictBatch(List.of(iDataCollect.collectData(corp1Game, corp1Game.getPlayerByUuid(playerUuid)), iDataCollect.collectData(corp2Game, corp2Game.getPlayerByUuid(playerUuid))), player);

        double corp1Synergy = evaluateHandSynergy(corp1Game, playerUuid);
        double corp2Synergy = evaluateHandSynergy(corp2Game, playerUuid);

        if (Math.max(predictions.getFirst().baseProb, corp1Synergy) > Math.max(predictions.getLast().baseProb, corp2Synergy)) {
            return new CorpEvaluation(player.getCorporations().getCards().getFirst(), optimizedDecisions);
        } else {
            return new CorpEvaluation(player.getCorporations().getCards().getLast(), optimizedDecisions);
        }
    }

    private double evaluateHandSynergy(
            MarsGame game,
            String playerUuid
    ) {
        Player player = game.getPlayerByUuid(playerUuid);
        player.setBuilds(List.of(new BuildDto(BuildType.GREEN_OR_BLUE)));

        List<CardWithChanceAndInput> validCards = cardProjectionService.getAvailableProjectsSync(game, player, null);

        List<float[]> futureStates = new ArrayList<>();
        for (CardWithChanceAndInput cardProj : validCards) {
            // Симулируем шаг
            MarsGame nextGame = cardProjectionService.projectBuildCardWithRequirements(game, player, cardProj);
            futureStates.add(iDataCollect.collectData(nextGame, nextGame.getPlayerByUuid(player.getUuid())));
        }

        List<Prediction> predictions = nnService.predictBatch(futureStates, player);

        List<Double> chances = new ArrayList<>();

        for (Prediction prediction : predictions) {
            chances.add(prediction.baseProb);

        }

        chances.sort(Comparator.reverseOrder());

        int K = Math.min(4, chances.size());

        double sum = 0;
        for (int i = 0; i < K; i++) {
            sum += chances.get(i);
        }

        return sum / K;
    }





}
