package com.terraforming.ares.services.ai.network2;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.MarsContextProvider;
import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.ai.advanced.IDataCollect;
import com.terraforming.ares.services.ai.dl4j.NNService;
import com.terraforming.ares.services.ai.dl4j.Prediction;
import com.terraforming.ares.services.ai.network2.buildParams.AiInputOptimizer;
import com.terraforming.ares.services.ai.network2.buildParams.OptimizedInputDecisions;
import com.terraforming.ares.services.ai.network2.buildParams.SharedInputAnalysis;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class Network2CorporationAndMulliganService {
    private static final int MULLIGAN_CARDS_TO_CHECK_FROM_DECK = 100;
    private static final int CORP_SIMULATIONS = 50;

    private final CardService cardService;
    private final MarsContextProvider marsContextProvider;
    private final Network2CorporationInputService network2CorporationInputService;
    private final AiInputOptimizer optimizer;
    private final Network2DiscardCardsProcessor network2DiscardCardsProcessor;

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
    ) {
    }

    public record StartupDecision(int corporationId, List<Integer> cardsToDiscard, double expectedValue) {
    }

    public List<Integer> getCardsToDiscard(MarsGame originalGame, String playerUuid) {
        originalGame = new MarsGame(originalGame);
        final List<Player> players = new ArrayList<>(originalGame.getPlayerUuidToPlayer().values());
        Player originalPlayer = originalGame.getPlayerByUuid(playerUuid);
        Player anotherPlayer = players.get(0) == originalPlayer ? players.get(1) : players.get(0);
        anotherPlayer.setMc(60);

        List<Integer> startupHand = new ArrayList<>(originalPlayer.getHand().getCards());
        List<Integer> corps = new ArrayList<>(originalPlayer.getCorporations().getCards());

        List<StartupDecision> options = new ArrayList<>();

        SharedInputAnalysis sharedInputAnalysis = network2CorporationInputService.analyzeCorporationsForSharedInput(originalPlayer.getCorporations().getCards().stream().map(cardService::getCard)
                .map(card -> card.getCardMetadata().getCardAction()).collect(Collectors.toSet())
        );
        OptimizedInputDecisions optimizedDecisions = (sharedInputAnalysis == null ? null : optimizer.optimizeInputDecisions(originalGame, originalPlayer, sharedInputAnalysis));

        Deck projectsDeck = cardService.createProjectsDeck(originalGame.getExpansions());
        projectsDeck.removeCards(originalPlayer.getHand().getCards());
        List<Integer> deck = new ArrayList<>(projectsDeck.getCards());

        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (Integer corpId : corps) {
            // Проводим N симуляций для каждой корпорации
            double totalValue = 0;

            MarsGame simGame = new MarsGame(originalGame);
            Player simPlayer = simGame.getPlayerByUuid(playerUuid);
            applyCorporationEffect(simGame, simPlayer, corpId, optimizedDecisions);

            double baseProbBeforeMulligan = nnService.predictBatch(List.of(iDataCollect.collectData(simGame, simPlayer.getUuid())), simPlayer).getFirst().baseProb;
            List<Integer> currentKeep = optimizeMulliganForCorp(simGame, simPlayer, startupHand, baseProbBeforeMulligan);
            Card corporationCard = cardService.getCard(corpId);
            CardAction cardAction = corporationCard.getCardMetadata().getCardAction();

            List<float[]> simAfterMulligan = new ArrayList<>();

            for (int i = 0; i < CORP_SIMULATIONS; i++) {
                // 3. Выполняем "честный" муллиган именно под эту корпу
                // Здесь внутри используется нейронка, которая уже видит построенную корпу!
                int totalCardsGet = startupHand.size() - currentKeep.size();
                if (cardAction == CardAction.INVENTRIX_CORPORATION) {
                    totalCardsGet += 3;
                }

                List<Integer> newCards = randomSubset(deck, totalCardsGet, random);
                if (cardAction == CardAction.ZETACELL_CORPORATION) {
                    newCards.addAll(getFiveRandomCards(deck, random, newCards));
                } else if (cardAction == CardAction.DEVTECHS_CORPORATION) {
                    getFiveRandomCards(deck, random, newCards).stream().map(cardService::getCard).filter(c -> c.getColor() == CardColor.GREEN).forEach(c -> newCards.add(c.getId()));
                } else if (cardAction == CardAction.LAUNCH_STAR_CORPORATION) {
                    while (true) {
                        int randomCard = deck.get(random.nextInt(deck.size()));
                        if (newCards.contains(randomCard)) {
                            continue;
                        }
                        Card card = cardService.getCard(randomCard);
                        if (card.getColor() != CardColor.BLUE) {
                            continue;
                        }
                        newCards.add(randomCard);
                        break;
                    }
                } else if (cardAction == CardAction.MINING_GUILD_CORPORATION) {
                    while (true) {
                        int randomCard = deck.get(random.nextInt(deck.size()));
                        if (newCards.contains(randomCard)) {
                            continue;
                        }
                        Card card = cardService.getCard(randomCard);
                        if (!card.getTags().contains(Tag.BUILDING)) {
                            continue;
                        }
                        newCards.add(randomCard);
                        break;
                    }
                } else if (cardAction == CardAction.ECOLINE_CORPORATION) {
                    while (true) {
                        int randomCard = deck.get(random.nextInt(deck.size()));
                        if (newCards.contains(randomCard)) {
                            continue;
                        }
                        Card card = cardService.getCard(randomCard);
                        if (!card.getTags().contains(Tag.PLANT)) {
                            continue;
                        }
                        newCards.add(randomCard);
                        break;
                    }
                }

                simPlayer.getHand().addCards(newCards);

                if (cardAction == CardAction.ZETACELL_CORPORATION) {
                    List<Integer> bestCards = network2DiscardCardsProcessor.getBestCards(simGame, simPlayer, new ArrayList<>(simPlayer.getHand().getCards()), simPlayer.getHand().getCards().size() - 4);
                    simPlayer.getHand().getCards().clear();
                    simPlayer.getHand().getCards().addAll(bestCards);
                }

                simAfterMulligan.add(iDataCollect.collectData(simGame, simPlayer.getUuid()));

                simPlayer.getHand().getCards().clear();
                simPlayer.getHand().addCards(currentKeep);
            }

            List<Prediction> predictions = nnService.predictBatch(simAfterMulligan, simPlayer);

            for (Prediction pred : predictions) {
                totalValue += pred.baseProb;
            }
            List<Integer> cardsToDiscard = new ArrayList<>(startupHand);
            cardsToDiscard.removeAll(currentKeep);

            options.add(new StartupDecision(corpId, cardsToDiscard, totalValue / CORP_SIMULATIONS));
        }

        return getBestStartupDecision(options).cardsToDiscard();
    }

    private StartupDecision getBestStartupDecision(List<StartupDecision> options) {
        StartupDecision bestStartupDecision;
        if (AiConstants.EXPLORATION_ON_CORP_PICK && (ThreadLocalRandom.current().nextInt(10) == 1 || Math.abs(options.get(0).expectedValue - options.get(1).expectedValue) <= 0.02)) {
            bestStartupDecision = options.get(ThreadLocalRandom.current().nextInt(2));
        } else {
            bestStartupDecision = options.stream().max(Comparator.comparingDouble(StartupDecision::expectedValue)).orElseThrow();
        }
        return bestStartupDecision;
    }

    private List<Integer> getFiveRandomCards(List<Integer> deck, ThreadLocalRandom random, List<Integer> newCards) {
        List<Integer> fiveRandomCards = new ArrayList<>();
        while (fiveRandomCards.size() < 5) {
            int randomCard = deck.get(random.nextInt(deck.size()));
            if (newCards.contains(randomCard) || fiveRandomCards.contains(randomCard)) {
                continue;
            }
            fiveRandomCards.add(randomCard);
        }
        return fiveRandomCards;
    }

    public <T> List<T> randomSubset(List<T> list, int count, Random rnd) {
        int size = list.size();
        if (count >= size) return list;

        return rnd.ints(0, size)
                .distinct()
                .limit(count)
                .mapToObj(list::get)
                .collect(Collectors.toList());
    }

    private void applyCorporationEffect(MarsGame simGame, Player simPlayer, int selectedCorporationId, OptimizedInputDecisions optimizedDecisions) {
        simPlayer.setSelectedCorporationCard(selectedCorporationId);
        simPlayer.setMulligan(false);
        simPlayer.getPlayed().addCard(selectedCorporationId);
        List<Integer> originalHand = new ArrayList<>(simPlayer.getHand().getCards());

        CorporationCard card = (CorporationCard) cardService.getCard(selectedCorporationId);
        final MarsContext marsContext = marsContextProvider.provide(simGame, simPlayer);
        card.buildProject(marsContext);
        if (card.onBuiltEffectApplicableToItself()) {
            card.postProjectBuiltEffect(marsContext, card, network2CorporationInputService.getCorporationInput(simGame, simPlayer, card.getCardMetadata().getCardAction(), optimizedDecisions));
        }
        simPlayer.getHand().getCards().clear();
        simPlayer.getHand().getCards().addAll(originalHand);
    }

    private List<Integer> optimizeMulliganForCorp(MarsGame game, Player player, List<Integer> fullHand, double baseProb) {
        // ВАЖНО: Мы здесь используем твой метод, который считает дельты карт,
        // но так как корпа уже "построена" в simGame, дельты будут ОЧЕНЬ точными.
        // Нейронка скажет: "Для этой корпы сталь бесполезна, дельта карты 0.001 -> сброс".

        HandEvaluation eval = evaluateHandForCorp(game, player.getUuid(), baseProb);
        List<Integer> toKeep = new ArrayList<>();

        for (Integer cardId : fullHand) {
            if (eval.cardIdToDelta().getOrDefault(cardId, 0.0) >= eval.avgDeckDelta()) {
                toKeep.add(cardId);
            }
        }

        // Применяем изменения к руке симуляционного игрока
        player.getHand().getCards().clear();
        player.getHand().addCards(toKeep);

        return toKeep;
    }

    private HandEvaluation evaluateHandForCorp(
            MarsGame game,
            String playerUuid,
            double baseProb // Вероятность с текущей полной рукой
    ) {
        Player player = game.getPlayerByUuid(playerUuid);
        List<Integer> originalHand = new ArrayList<>(player.getHand().getCards());

        // ==== 1. Оценка ценности каждой карты в руке ====
        // Считаем дельту: (Шанс с рукой) - (Шанс без этой конкретной карты)
        List<float[]> statesWithoutCards = new ArrayList<>();
        for (Integer cardId : originalHand) {
            player.getHand().removeCard(cardId);
            statesWithoutCards.add(iDataCollect.collectData(game, player.getUuid()));
            player.getHand().addCard(cardId);
        }

        Deck deck = cardService.createProjectsDeck(game.getExpansions());
        deck.removeCards(player.getHand().getCards());
        List<Integer> deckSample = deck.dealCards(Math.min(deck.size(), MULLIGAN_CARDS_TO_CHECK_FROM_DECK));

        List<float[]> statesWithExtraCard = new ArrayList<>();
        for (Integer deckCardId : deckSample) {
            // ВАЖНО: Мы добавляем карту в ПОЛНУЮ руку (8+1)
            player.getHand().addCard(deckCardId);
            statesWithExtraCard.add(iDataCollect.collectData(game, player.getUuid()));
            player.getHand().removeCard(deckCardId);
        }

        List<float[]> allStates = new ArrayList<>(statesWithoutCards);
        allStates.addAll(statesWithExtraCard);

        List<Prediction> allPreds = nnService.predictBatch(allStates, player);


        List<Prediction> handPreds = allPreds.subList(0, statesWithoutCards.size());
        List<Prediction> deckPreds = allPreds.subList(statesWithoutCards.size(), allPreds.size());


        Map<Integer, Double> handDeltas = new HashMap<>();
        double totalHandValue = 0;

        for (int i = 0; i < originalHand.size(); i++) {
            int cardId = originalHand.get(i);
            // Дельта показывает, сколько "шансов на победу" приносит именно эта карта, находясь в руке
            double delta = baseProb - handPreds.get(i).baseProb;

            // Мы берем только положительные дельты (карты-активы)
            double score = Math.max(0, delta);
            handDeltas.put(cardId, score);
            totalHandValue += score;
        }


        // Считаем среднюю вероятность "руки со случайной дырой"
        // Это наш реалистичный Baseline: "у меня 7 карт, и я жду восьмую"
        double sumBaseMinusOne = handPreds.stream()
                .mapToDouble(p -> p.baseProb)
                .average()
                .orElse(baseProb);


        double totalDeckDelta = 0;

        for (Prediction p : deckPreds) {
            // СРАВНИВАЕМ: (Шанс с новой картой) против (Среднего шанса без одной старой карты)
            // Это и есть цена замены: мы "платим" за вход новой карты потерей одной старой
            double swapValue = p.baseProb - sumBaseMinusOne;
            totalDeckDelta += Math.max(0, swapValue);
        }

        double avgDeckDelta = !deckPreds.isEmpty()
                ? totalDeckDelta / deckPreds.size()
                : 0;

        return new HandEvaluation(handDeltas, totalHandValue, avgDeckDelta);
    }

    public CorpEvaluation chooseCorporation(MarsGame originalGame, String playerUuid) {
        originalGame = new MarsGame(originalGame);
        final List<Player> players = new ArrayList<>(originalGame.getPlayerUuidToPlayer().values());
        Player originalPlayer = originalGame.getPlayerByUuid(playerUuid);
        Player anotherPlayer = players.get(0) == originalPlayer ? players.get(1) : players.get(0);
        anotherPlayer.setMc(60);

        List<Integer> startupHand = new ArrayList<>(originalPlayer.getHand().getCards());
        List<Integer> corps = new ArrayList<>(originalPlayer.getCorporations().getCards());

        List<StartupDecision> options = new ArrayList<>();

        SharedInputAnalysis sharedInputAnalysis = network2CorporationInputService.analyzeCorporationsForSharedInput(originalPlayer.getCorporations().getCards().stream().map(cardService::getCard)
                .map(card -> card.getCardMetadata().getCardAction()).collect(Collectors.toSet())
        );
        OptimizedInputDecisions optimizedDecisions = (sharedInputAnalysis == null ? null : optimizer.optimizeInputDecisions(originalGame, originalPlayer, sharedInputAnalysis));

        Deck projectsDeck = cardService.createProjectsDeck(originalGame.getExpansions());
        projectsDeck.removeCards(originalPlayer.getHand().getCards());
        List<Integer> deck = projectsDeck.getCards();

        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (Integer corpId : corps) {
            // Проводим N симуляций для каждой корпорации
            double totalValue = 0;

            MarsGame simGame = new MarsGame(originalGame);
            Player simPlayer = simGame.getPlayerByUuid(playerUuid);
            applyCorporationEffect(simGame, simPlayer, corpId, optimizedDecisions);

            Card corporationCard = cardService.getCard(corpId);
            CardAction cardAction = corporationCard.getCardMetadata().getCardAction();


            if (cardAction != CardAction.INVENTRIX_CORPORATION
                    && cardAction != CardAction.DEVTECHS_CORPORATION
                    && cardAction != CardAction.LAUNCH_STAR_CORPORATION
                    && cardAction != CardAction.MINING_GUILD_CORPORATION
                    && cardAction != CardAction.ECOLINE_CORPORATION
                    && cardAction != CardAction.ZETACELL_CORPORATION) {
                options.add(new StartupDecision(corpId, List.of(), nnService.predictBatch(List.of(iDataCollect.collectData(simGame, simPlayer.getUuid())), simPlayer).getFirst().baseProb));
            } else {
                List<float[]> simAfterCardsAcquisition = new ArrayList<>();

                for (int i = 0; i < CORP_SIMULATIONS; i++) {
                    int totalCardsGet = 0;

                    List<Integer> newCards = new ArrayList<>();

                    if (cardAction == CardAction.INVENTRIX_CORPORATION) {
                        newCards.addAll(randomSubset(deck, totalCardsGet, random));
                    } else if (cardAction == CardAction.ZETACELL_CORPORATION) {
                        newCards.addAll(getFiveRandomCards(deck, random, newCards));
                    } else if (cardAction == CardAction.DEVTECHS_CORPORATION) {
                        getFiveRandomCards(deck, random, newCards).stream().map(cardService::getCard).filter(c -> c.getColor() == CardColor.GREEN).forEach(c -> newCards.add(c.getId()));
                    } else if (cardAction == CardAction.LAUNCH_STAR_CORPORATION) {
                        while (true) {
                            int randomCard = deck.get(random.nextInt(deck.size()));
                            Card card = cardService.getCard(randomCard);
                            if (card.getColor() != CardColor.BLUE) {
                                continue;
                            }
                            newCards.add(randomCard);
                            break;
                        }
                    } else if (cardAction == CardAction.MINING_GUILD_CORPORATION) {
                        while (true) {
                            int randomCard = deck.get(random.nextInt(deck.size()));
                            Card card = cardService.getCard(randomCard);
                            if (!card.getTags().contains(Tag.BUILDING)) {
                                continue;
                            }
                            newCards.add(randomCard);
                            break;
                        }
                    } else if (cardAction == CardAction.ECOLINE_CORPORATION) {
                        while (true) {
                            int randomCard = deck.get(random.nextInt(deck.size()));
                            Card card = cardService.getCard(randomCard);
                            if (!card.getTags().contains(Tag.PLANT)) {
                                continue;
                            }
                            newCards.add(randomCard);
                            break;
                        }
                    }

                    simPlayer.getHand().addCards(newCards);

                    if (cardAction == CardAction.ZETACELL_CORPORATION) {
                        List<Integer> bestCards = network2DiscardCardsProcessor.getBestCards(simGame, simPlayer, new ArrayList<>(simPlayer.getHand().getCards()), simPlayer.getHand().getCards().size() - 4);
                        simPlayer.getHand().getCards().clear();
                        simPlayer.getHand().getCards().addAll(bestCards);
                    }

                    simAfterCardsAcquisition.add(iDataCollect.collectData(simGame, simPlayer.getUuid()));

                    simPlayer.getHand().getCards().clear();
                    simPlayer.getHand().addCards(startupHand);

                }

                List<Prediction> predictions = nnService.predictBatch(simAfterCardsAcquisition, simPlayer);

                for (Prediction pred : predictions) {
                    totalValue += pred.baseProb;
                }

                options.add(new StartupDecision(corpId, List.of(), totalValue / CORP_SIMULATIONS));
            }
        }

        return new CorpEvaluation(getBestStartupDecision(options).corporationId(), optimizedDecisions);
    }

}
