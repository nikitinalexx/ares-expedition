package com.terraforming.ares.services.ai.network2.buildParams;

import com.terraforming.ares.cards.blue.BacterialAggregates;
import com.terraforming.ares.cards.blue.Decomposers;
import com.terraforming.ares.cards.red.CeosFavoriteProject;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.ai.dl4j.Prediction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static com.terraforming.ares.model.InputFlag.DECOMPOSERS_TAKE_CARD;
import static com.terraforming.ares.model.InputFlag.DECOMPOSERS_TAKE_MICROBE;

@Component
@RequiredArgsConstructor
public class AiOptimalBuildService {
    private final AiCardBuildInputAnalyzer analyzer;
    private final AiInputOptimizer optimizer;
    private final CardService cardService;

    /**
     * Основной метод: получает список карт и возвращает оптимальные входные данные для каждой
     */
    public List<CardWithInputParams> getOptimalInputsForCards(
            MarsGame game, Player player, List<Card> cards, Map<SharedInputAnalysis, OptimizedInputDecisions> optimizedInputs) {

        // 1. Анализируем все карты и группируем требования
        SharedInputAnalysis analysis = analyzer.analyzeCards(player, cards);

        // 2. Оптимизируем решения для каждой группы
        OptimizedInputDecisions optimizedDecisions;
        if (optimizedInputs != null && optimizedInputs.containsKey(analysis)) {
            optimizedDecisions = optimizedInputs.get(analysis);
        } else {
            optimizedDecisions = optimizer.optimizeInputDecisions(game, player, analysis);
            if (optimizedInputs != null) {
                optimizedInputs.put(analysis, optimizedDecisions);
            }
        }

        Set<CardAction> playedCards = player.getPlayed().getCards().stream().map(cardService::getCard).map(card -> card.getCardMetadata().getCardAction()).collect(Collectors.toSet());

        List<CardWithInputParams> buildScenarios = new ArrayList<>();

        // 3. Применяем оптимизированные решения к каждой карте
        for (Card card : cards) {
            buildScenarios.add(generateInputBasedOnOptimizedDecisions(player, card, optimizedDecisions, playedCards));
        }

        return buildScenarios;
    }

    public CardWithInputParams generateInputBasedOnOptimizedDecisions(
            Player player, Card card,
            OptimizedInputDecisions decisions) {
        Set<CardAction> playedCards = player.getPlayed().getCards().stream().map(cardService::getCard).map(c -> c.getCardMetadata().getCardAction()).collect(Collectors.toSet());
        return generateInputBasedOnOptimizedDecisions(player, card, decisions, playedCards);
    }

    public CardWithInputParams generateInputBasedOnOptimizedDecisions(
            Player player, Card card,
            OptimizedInputDecisions decisions,
            Set<CardAction> playedCards) {


        CardAction cardAction = card.getCardMetadata().getCardAction();

        Map<InputRequirementType, DecisionWithPrediction<Card>> bestResourceCardPerType = decisions.getBestResourceCardPerType();

        DecisionWithPrediction<Card> bestMicrobePrediction = bestResourceCardPerType.getOrDefault(InputRequirementType.MICROBE_INPUT, null);
        DecisionWithPrediction<Card> bestAnimalPrediction = bestResourceCardPerType.getOrDefault(InputRequirementType.ANIMAL_INPUT, null);

        Card bestMicrobe = (bestMicrobePrediction != null) ? bestMicrobePrediction.value : null;
        Card bestAnimal = (bestAnimalPrediction != null) ? bestAnimalPrediction.value : null;


        if (cardAction == CardAction.CRYOGENIC_SHIPMENT) {
            Map<Integer, List<Integer>> result = new HashMap<>();

            result.put(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(decisions.getBestPhaseUpgradeOverall().value));
            result.put(InputFlag.CRYOGENIC_SHIPMENT_PUT_RESOURCE.getId(), List.of(
                    decisions.isAtLeastMicrobeOrAnimalPresent() ? (decisions.isMicrobeIsBetter() ? bestMicrobe.getId() : bestAnimal.getId()) : InputFlag.SKIP_ACTION.getId()
            ));

            result.putAll(getActiveInputFromCards(player, playedCards, card, result, decisions));

            return new CardWithInputParams(card, result);
        }
        if (cardAction == CardAction.UPDATE_PHASE_CARD_TWICE) {
            return new CardWithInputParams(card, Map.of(
                    InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(decisions.getBestPhaseUpgradeOverall().value, decisions.getSecondBestPhaseUpgradeOverall().value)
            ));
        }
        if (cardAction == CardAction.TOPOGRAPHIC_MAPPING) {
            Map<Integer, List<Integer>> result = new HashMap<>();
            result.put(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(decisions.getBestPhaseUpgradeOverall().value));

            result.put(InputFlag.TAG_INPUT.getId(), List.of(decisions.getBestTagDecision().getFirst().value.ordinal()));

            result.putAll(getActiveInputFromCards(player, playedCards, card, result, decisions));

            return new CardWithInputParams(card, result);
        }
        if (cardAction == CardAction.UPDATE_PHASE_1_CARD) {
            return new CardWithInputParams(card, Map.of(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(decisions.getPhaseUpgradeDecisions().get(0).value)));
        }
        if (cardAction == CardAction.UPDATE_PHASE_2_CARD) {
            return new CardWithInputParams(card, Map.of(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(decisions.getPhaseUpgradeDecisions().get(1).value)));
        }
        if (cardAction == CardAction.COMMUNICATIONS_STREAMLINING) {
            Map<Integer, List<Integer>> result = new HashMap<>();
            result.put(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(decisions.getPhaseUpgradeDecisions().get(2).value));

            result.putAll(getActiveInputFromCards(player, playedCards, card, result, decisions));

            return new CardWithInputParams(card, result);
        }
        if (cardAction == CardAction.UPDATE_PHASE_4_CARD) {
            Map<Integer, List<Integer>> result = new HashMap<>();
            result.put(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(decisions.getPhaseUpgradeDecisions().get(3).value));

            result.putAll(getActiveInputFromCards(player, playedCards, card, result, decisions));

            return new CardWithInputParams(card, result);
        }
        if (cardAction == CardAction.CHOOSE_TAG) {
            Map<Integer, List<Integer>> result = new HashMap<>();
            result.put(InputFlag.TAG_INPUT.getId(), List.of(decisions.getBestTagDecision().getFirst().value.ordinal()));

            result.putAll(getActiveInputFromCards(player, playedCards, card, result, decisions));

            return new CardWithInputParams(card, result);
        }

        if (cardAction != null && AiConstants.CARD_ACTIONS_WITH_PHASE_UPGRADE_EFFECT.contains(cardAction)) {

            Map<Integer, List<Integer>> result = new HashMap<>();
            result.put(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(decisions.getBestPhaseUpgradeOverall().value));

            result.putAll(getActiveInputFromCards(player, playedCards, card, result, decisions));

            return new CardWithInputParams(card, result);
        }
        if (cardAction == CardAction.BIOMEDICAL_IMPORTS) {
            Map<Integer, List<Integer>> result = new HashMap<>();

            DecisionWithPrediction<Integer> bestPhaseUpgradeOverall = decisions.getBestPhaseUpgradeOverall();
            Prediction oxygenPrediction = decisions.getOxygenPrediction();

            if (oxygenPrediction == null || oxygenPrediction.baseProb < bestPhaseUpgradeOverall.prediction.baseProb) {
                result.put(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(decisions.getBestPhaseUpgradeOverall().value));
            } else {
                result.put(InputFlag.BIOMEDICAL_IMPORTS_RAISE_OXYGEN.getId(), List.of(0));
            }
            result.putAll(getActiveInputFromCards(player, playedCards, card, result, decisions));

            return new CardWithInputParams(card, result);
        }
        if (cardAction == CardAction.ASTROFARM) {
            Map<Integer, List<Integer>> result = new HashMap<>();

            if (bestMicrobe != null) {
                result.put(InputFlag.ASTROFARM_PUT_RESOURCE.getId(), List.of(bestMicrobe.getId()));
            } else {
                result.put(InputFlag.ASTROFARM_PUT_RESOURCE.getId(), List.of(InputFlag.SKIP_ACTION.getId()));

            }
            return new CardWithInputParams(card, result);
        }
        if (cardAction == CardAction.EOS_CHASMA) {
            Map<Integer, List<Integer>> result = new HashMap<>();

            if (bestAnimal != null) {
                result.put(InputFlag.EOS_CHASMA_PUT_RESOURCE.getId(), List.of(bestAnimal.getId()));
            } else {
                result.put(InputFlag.EOS_CHASMA_PUT_RESOURCE.getId(), List.of(InputFlag.SKIP_ACTION.getId()));
            }
            return new CardWithInputParams(card, result);
        }
        if (cardAction == CardAction.IMPORTED_HYDROGEN) {

            if (!decisions.isAtLeastMicrobeOrAnimalPresent()) {
                return new CardWithInputParams(card, Map.of(InputFlag.IMPORTED_HYDROGEN_PICK_PLANT.getId(), List.of()));
            }

            List<Map<Integer, List<Integer>>> result = new ArrayList<>(2);
            result.add(Map.of(InputFlag.IMPORTED_HYDROGEN_PICK_PLANT.getId(), List.of()));
            result.add(Map.of(InputFlag.IMPORTED_HYDROGEN_PUT_RESOURCE.getId(), List.of(decisions.isMicrobeIsBetter() ? bestMicrobe.getId() : bestAnimal.getId())));

            return new CardWithInputParams(card, result);
        }
        if (cardAction == CardAction.IMPORTED_NITROGEN) {

            Map<Integer, List<Integer>> result = new HashMap<>();

            result.put(InputFlag.IMPORTED_NITROGEN_ADD_ANIMALS.getId(),
                    List.of(bestAnimal == null ? InputFlag.SKIP_ACTION.getId() : bestAnimal.getId()));
            result.put(InputFlag.IMPORTED_NITROGEN_ADD_MICROBES.getId(),
                    List.of(bestMicrobe == null ? InputFlag.SKIP_ACTION.getId() : bestMicrobe.getId()));

            return new CardWithInputParams(card, result);
        }
        if (cardAction == CardAction.LARGE_CONVOY) {
            if (bestAnimal == null) {
                return new CardWithInputParams(card, Map.of(InputFlag.LARGE_CONVOY_PICK_PLANT.getId(), List.of()));
            }

            List<Map<Integer, List<Integer>>> result = new ArrayList<>(2);
            result.add(Map.of(InputFlag.LARGE_CONVOY_PICK_PLANT.getId(), List.of()));
            result.add(Map.of(InputFlag.LARGE_CONVOY_ADD_ANIMAL.getId(), List.of(bestAnimal.getId())));

            return new CardWithInputParams(card, result);
        }
        if (cardAction == CardAction.LOCAL_HEAT_TRAPPING) {
            Map<Integer, List<Integer>> result = new HashMap<>();


            result.put(InputFlag.LOCAL_HEAT_TRAPPING_PUT_RESOURCE.getId(),
                    List.of(!decisions.isAtLeastMicrobeOrAnimalPresent() ? InputFlag.SKIP_ACTION.getId() : (decisions.isMicrobeIsBetter() ? bestMicrobe.getId() : bestAnimal.getId())));

            return new CardWithInputParams(card, result);
        }

        if (cardAction == CardAction.MARS_UNIVERSITY || cardAction == CardAction.VIRAL_ENHANCERS || cardAction == CardAction.DECOMPOSERS) {
            Map<Integer, List<Integer>> result = new HashMap<>(getActiveInputFromCards(player, playedCards, card, new HashMap<>(), decisions));

            return new CardWithInputParams(card, result);
        }

        if (!card.getCardMetadata().getResourcesOnBuild().isEmpty()
                && card.getCardMetadata().getResourcesOnBuild().getFirst().getType() == CardCollectableResource.ANY) {
            Map<Integer, List<Integer>> result = new HashMap<>();


            Prediction bestPrediction = null;
            Card bestCard = null;
            if (bestAnimalPrediction != null) {
                bestPrediction = bestAnimalPrediction.getPrediction();
                bestCard = bestAnimal;
            }
            if (bestMicrobePrediction != null && (bestPrediction == null || bestMicrobePrediction.prediction.baseProb > bestPrediction.baseProb)) {
                bestPrediction = bestMicrobePrediction.getPrediction();
                bestCard = bestMicrobe;
            }
            DecisionWithPrediction<Card> sciencePrediction = bestResourceCardPerType.getOrDefault(InputRequirementType.SCIENCE_RESOURCE_INPUT, null);
            if (sciencePrediction != null && (bestPrediction == null || sciencePrediction.prediction.baseProb > bestPrediction.baseProb)) {
                bestPrediction = sciencePrediction.getPrediction();
                bestCard = sciencePrediction.getValue();
            }

            if (bestCard == null) {
                throw new IllegalStateException("Trying to get best card for Ceos Favorite Project when none applicable");
            }

            result.put(InputFlag.CEOS_FAVORITE_PUT_RESOURCES.getId(), List.of(bestCard.getId()));
            return new CardWithInputParams(card, result);
        }


        if (cardAction == CardAction.SYNTHETIC_CATASTROPHE) {
            assert decisions.getBestRedCardToRollback() != null;
            List<Map<Integer, List<Integer>>> result = new ArrayList<>();

            Map<Integer, List<Integer>> redCardInputParams = new HashMap<>();
            redCardInputParams.put(InputFlag.SYNTHETIC_CATASTROPHE_CARD.getId(), List.of(decisions.getBestRedCardToRollback()));
            result.add(redCardInputParams);

            return new CardWithInputParams(card, result);
        }

        return new CardWithInputParams(card, getActiveInputFromCards(player, playedCards, card, new HashMap<>(), decisions));
    }

    public Map<Integer, List<Integer>> getActiveInputFromCards(Player player, Set<CardAction> playedCards, Card card, Map<Integer, List<Integer>> input, OptimizedInputDecisions decisions) {
        Map<Integer, List<Integer>> result = new HashMap<>();
        result.putAll(getMarsUniversityInputIfApplicable(player, playedCards, card, input));
        result.putAll(getDecomposersInputIfApplicable(player, playedCards, card, input));
        result.putAll(getViralEnhancersInputIfApplicable(playedCards, card, input, decisions));
        return result;
    }

    private Map<Integer, List<Integer>> getMarsUniversityInputIfApplicable(Player player, Set<CardAction> playedCards,
                                                                           Card card, Map<Integer, List<Integer>> input) {
        if (!cardActionTriggered(playedCards, card, CardAction.MARS_UNIVERSITY)) {
            return Map.of();
        }

        int scienceTagsCount = cardService.countCardTags(card, Set.of(Tag.SCIENCE), input);
        if (scienceTagsCount == 0) {
            return Map.of();
        }

        int howManyCardsCanDiscard = Math.min(scienceTagsCount, (player.getHand().size() - 1));//-1 because we build one card

        if (howManyCardsCanDiscard == 0) {
            return Map.of(InputFlag.MARS_UNIVERSITY_CARD.getId(), List.of(InputFlag.SKIP_ACTION.getId()));
        }

        return Map.of(InputFlag.MARS_UNIVERSITY_DUMMY_INPUT.getId(), List.of(scienceTagsCount));
    }

    private Map<Integer, List<Integer>> getDecomposersInputIfApplicable(Player player, Set<CardAction> playedCards,
                                                                        Card card, Map<Integer, List<Integer>> input) {
        if (!cardActionTriggered(playedCards, card, CardAction.DECOMPOSERS)) {
            return Map.of();
        }
        int tagsCount = cardService.countCardTags(card, Set.of(Tag.ANIMAL, Tag.MICROBE, Tag.PLANT), input);
        if (tagsCount == 0) {
            return Map.of();
        }

        int takeMicrobes = 0;
        int takeCards = 0;
        int microbesOnCard = player.getCardResourcesCount().getOrDefault(Decomposers.class, 0);

        for (int i = 0; i < tagsCount; i++) {
            if (microbesOnCard > 0) {
                takeCards++;
                microbesOnCard--;
            } else {
                takeMicrobes++;
                microbesOnCard++;
            }
        }

        Map<Integer, List<Integer>> result = new HashMap<>();
        if (takeMicrobes != 0) {
            result.put(DECOMPOSERS_TAKE_MICROBE.getId(), List.of(takeMicrobes));
        }
        if (takeCards != 0) {
            result.put(DECOMPOSERS_TAKE_CARD.getId(), List.of(takeCards));
        }
        return result;
    }

    private Map<Integer, List<Integer>> getViralEnhancersInputIfApplicable(Set<CardAction> playedCards, Card card,
                                                                           Map<Integer, List<Integer>> input, OptimizedInputDecisions decisions) {
        if (!cardActionTriggered(playedCards, card, CardAction.VIRAL_ENHANCERS)) {
            return Map.of();
        }
        int tagsCount = cardService.countCardTags(card, Set.of(Tag.ANIMAL, Tag.MICROBE, Tag.PLANT), input);
        if (tagsCount == 0) {
            return Map.of();
        }


        Map<Integer, List<Integer>> result = new HashMap<>();
        if (decisions.isViralEnhancersTakePlants()) {
            result.put(InputFlag.VIRAL_ENHANCERS_TAKE_PLANT.getId(), List.of(tagsCount));
        } else {
            result.put(InputFlag.VIRAL_ENHANCERS_PUT_RESOURCE.getId(), List.of(decisions.getBestResourceCardPerType().get(decisions.isMicrobeIsBetter() ? InputRequirementType.MICROBE_INPUT : InputRequirementType.ANIMAL_INPUT).getValue().getId()));
        }
        return result;
    }

    private boolean cardActionTriggered(Set<CardAction> playedCards, Card card, CardAction cardAction) {
        return card.getCardMetadata().getCardAction() == cardAction || playedCards.contains(cardAction);
    }
}
