package com.terraforming.ares.services.ai.turnProcessors.network2;

import com.terraforming.ares.cards.blue.BacterialAggregates;
import com.terraforming.ares.cards.green.PrivateInvestorBeach;
import com.terraforming.ares.cards.red.CeosFavoriteProject;
import com.terraforming.ares.cards.red.SyntheticCatastrophe;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.milestones.Milestone;
import com.terraforming.ares.model.parameters.OceanRequirement;
import com.terraforming.ares.model.parameters.ParameterColor;
import com.terraforming.ares.model.payments.MegacreditsPayment;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.CardValidationService;
import com.terraforming.ares.services.SpecialEffectsService;
import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.ai.advanced.AdvancedAiDataCollectionService;
import com.terraforming.ares.services.ai.dl4j.NNService;
import com.terraforming.ares.services.ai.dl4j.Prediction;
import com.terraforming.ares.services.ai.turnProcessors.AiTurnService;
import com.terraforming.ares.services.ai.turnProcessors.AiUtility;
import com.terraforming.ares.services.ai.turnProcessors.network2.buildParams.AiOptimalBuildService;
import com.terraforming.ares.services.ai.turnProcessors.network2.buildParams.CardWithInputParams;
import com.terraforming.ares.services.ai.turnProcessors.network2.dto.CardWithChanceModifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class Network2ProjectBuildService {
    private static final double ALPHA = 0.15;
    private static final Map<Class<?>, Integer> REAL_CARD_TO_DUMMY_CARD = Map.of(
            CeosFavoriteProject.class, AiConstants.CEOS_FAVORITE_PROJECT_DUMMY_ID,
            SyntheticCatastrophe.class, AiConstants.SYNTHETIC_CATASTOPHY_DUMMY_ID,
            PrivateInvestorBeach.class, AiConstants.PRIVATE_INVESTOR_BEACH_DUMMY_ID
    );

    private final SpecialEffectsService specialEffectsService;
    private final AiOptimalBuildService aiOptimalBuildService;
    private final Network2PaymentService network2PaymentService;
    private final AiTurnService aiTurnService;
    private final AdvancedAiDataCollectionService advancedAiDataCollectionService;
    private final NNService nnService;
    private final CardService cardService;
    private final AiUtility aiUtility;

    public List<CardWithChanceModifier> getBestCardProjectionsIgnoreRequirements(MarsGame marsGame, Player player, List<Card> cards) {
        List<Card> cardsWithStrongRestrictions = getCardsWithStrongRestrictions(marsGame, player, cards);

        cards = new ArrayList<>(cards);
        cards.removeAll(cardsWithStrongRestrictions);


        List<float[]> buildProjections = new ArrayList<>();
        for (Card specialCard : cardsWithStrongRestrictions) {
            Integer dummyCardId = REAL_CARD_TO_DUMMY_CARD.get(specialCard.getClass());
            MarsGame gameCopy = projectBuildDummyCard(marsGame, player, specialCard.getId(), dummyCardId);
            buildProjections.add(getGameParams(gameCopy, player.getUuid()));
        }

        List<CardWithInputParams> optimalInputsForCards = aiOptimalBuildService.getOptimalInputsForCards(marsGame, player, cards);

        for (CardWithInputParams cardWithInputParams : optimalInputsForCards) {
            Card card = cardWithInputParams.getCard();
            List<Map<Integer, List<Integer>>> inputParamsVariations = cardWithInputParams.getInputParamsVariations();
            MegacreditsPayment mcPayment = network2PaymentService.getMcPaymentOnly(marsGame, player, card, inputParamsVariations.isEmpty() ? Map.of() : inputParamsVariations.getFirst());

            for (Map<Integer, List<Integer>> inputParams : inputParamsVariations) {
                MarsGame gameCopy = projectBuildCardNoRequirements(marsGame, player, card, inputParams, mcPayment);
                buildProjections.add(getGameParams(gameCopy, player.getUuid()));
            }
        }

        List<Prediction> predictions = nnService.predictBatch(buildProjections, NNService.ModelType.OPTIMIZED);
        int pointer = 0;

        List<CardWithChanceModifier> cardWithChanceModifiers = new ArrayList<>();

        for (Card cardWithSpecialCase : cardsWithStrongRestrictions) {
            cardWithChanceModifiers.add(new CardWithChanceModifier(cardWithSpecialCase, predictions.get(pointer++).baseProb));
        }

        for (CardWithInputParams cardWithInputParams : optimalInputsForCards) {
            Card card = cardWithInputParams.getCard();

            if (!cardWithInputParams.getInputParamsVariations().isEmpty()) {
                Prediction bestPrediction = null;
                for (int i = 0; i < cardWithInputParams.getInputParamsVariations().size(); i++) {
                    Prediction nextPrediction = predictions.get(pointer++);
                    if (bestPrediction == null || nextPrediction.baseProb > bestPrediction.baseProb) {
                        bestPrediction = nextPrediction;
                    }
                }
                cardWithChanceModifiers.add(new CardWithChanceModifier(card, bestPrediction.baseProb));
            } else {
                cardWithChanceModifiers.add(new CardWithChanceModifier(card, predictions.get(pointer++).baseProb));
            }
        }

        for (CardWithChanceModifier cardWithChanceModifier : cardWithChanceModifiers) {
            cardWithChanceModifier.setModifier(cardRequirementsModificator(marsGame, player, cardWithChanceModifier.getCard()));
        }

        return cardWithChanceModifiers;
    }

    public List<Card> getCardsWithStrongRestrictions(MarsGame marsGame, Player player, List<Card> cards) {
        return cards.stream().filter(card -> {
            if (card.getClass() == CeosFavoriteProject.class && (player.getCardResourcesCount().isEmpty() || player.getCardResourcesCount().size() == 1 && player.getCardResourcesCount().getOrDefault(BacterialAggregates.class, 0) > AiConstants.BACTERIAL_AGGREGATES_COUNT_FOR_MICROBE_PUT)) {
                return true;
            }

            if (card.getCardMetadata().getCardAction() == CardAction.SYNTHETIC_CATASTROPHE && player.getPlayed().getCards().stream().map(cardService::getCard).noneMatch(c -> c.getColor() == CardColor.RED)) {
                return true;
            }

            return card.getClass() == PrivateInvestorBeach.class && getMilestoneMultiplier(marsGame, player) < 1;
        }).toList();
    }


    private double getMilestoneMultiplier(MarsGame game, Player player) {
        List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        Player opponent = (players.get(0).getUuid().equals(player.getUuid())) ? players.get(1) : players.get(0);

        double minEffectiveSteps = 10.0; // Базовая "дистанция"

        int achievedCount = 0;
        for (Milestone m : game.getMilestones()) {
            // 1. Если МЫ уже взяли этот майлстоун — условие выполнено идеально
            if (m.isAchieved(player)) {
                minEffectiveSteps = 0;
                break;
            }

            if (m.isAchieved()) {
                achievedCount++;
                continue;
            }

            double ourProgress = (double) m.getValue(player, cardService) / m.getMaxValue();

            double bestOpponentProgress = (double) m.getValue(opponent, cardService) / m.getMaxValue();

            double distance = (1.0 - ourProgress);

            if (bestOpponentProgress > ourProgress) {
                distance += (bestOpponentProgress - ourProgress) * 0.5;
            }

            double normalizedSteps = distance * 10.0;
            if (normalizedSteps < minEffectiveSteps) {
                minEffectiveSteps = normalizedSteps;
            }
        }

        if (achievedCount >= 3) {
            return 0;
        }

        return Math.exp(-0.15 * minEffectiveSteps);
    }

    private float[] getGameParams(MarsGame game, String playerUuid) {
        return advancedAiDataCollectionService.collectData(game, game.getPlayerByUuid(playerUuid));
    }

    private MarsGame projectBuildDummyCard(MarsGame game, Player player, Integer originalCardId, Integer dummyCardId) {
        game = new MarsGame(game);
        player = game.getPlayerByUuid(player.getUuid());

        player.getHand().removeCard(originalCardId);
        player.getHand().addCard(dummyCardId);

        int originalPlayerHandSize = player.getHand().size() - 1;//-1 because we build 1 card

        Card dummyCard = cardService.getCard(dummyCardId);

        Map<Integer, List<Integer>> dummyInputParams = Map.of();
        MegacreditsPayment mcPayment = network2PaymentService.getMcPaymentOnly(game, player, dummyCard, dummyInputParams);

        if (dummyCard.getColor() == CardColor.GREEN) {
            aiTurnService.buildGreenProjectSyncNoRequirements(game, player, dummyCardId, List.of(mcPayment), dummyInputParams);
        } else {
            aiTurnService.buildBlueRedProjectSyncNoRequirements(game, player, dummyCardId, List.of(mcPayment), dummyInputParams);
        }

        aiUtility.simulateDummyHandInsteadOfNewCards(player, originalPlayerHandSize);

        return game;
    }

    public MarsGame projectBuildCardNoRequirements(MarsGame game, Player player, Card card, Map<Integer, List<Integer>> inputParameters, MegacreditsPayment payment) {
        game = new MarsGame(game);
        player = game.getPlayerByUuid(player.getUuid());

        List<Integer> marsDummyInput = inputParameters.get(InputFlag.MARS_UNIVERSITY_DUMMY_INPUT.getId());
        int marsUniversityCardsToSimulate = 0;
        if (!CollectionUtils.isEmpty(marsDummyInput)) {
            marsUniversityCardsToSimulate = marsDummyInput.getFirst();
            inputParameters.put(InputFlag.MARS_UNIVERSITY_CARD.getId(), List.of(InputFlag.SKIP_ACTION.getId()));
        }

        int originalPlayerHandSize = player.getHand().size() - 1;//-1 because we build 1 card

        if (card.getColor() == CardColor.GREEN) {
            aiTurnService.buildGreenProjectSyncNoRequirements(game, player, card.getId(), List.of(payment), inputParameters);
        } else {
            aiTurnService.buildBlueRedProjectSyncNoRequirements(game, player, card.getId(), List.of(payment), inputParameters);
        }

        aiUtility.simulateDummyHandInsteadOfNewCards(player, originalPlayerHandSize);
        aiUtility.addDummyCards(player, marsUniversityCardsToSimulate);

        return game;
    }

    private double cardRequirementsModificator(MarsGame game, Player player, Card card) {
        if (card.getClass() == PrivateInvestorBeach.class) {
            return getMilestoneMultiplier(game, player);
        }

        Planet planet = game.getPlanet();

        OceanRequirement oceanReq = card.getOceanRequirement();
        if (oceanReq != null) {
            int current = planet.oceansBuilt();
            int min = oceanReq.getMinValue();
            int max = oceanReq.getMaxValue();

            // 1. Условие уже выполнено
            if (current >= min && current <= max) {
                return 1.0;
            }

            // 2. Слишком много океанов (условие провалено безвозвратно)
            if (current > max) {
                return 0.0;
            }

            // 3. Нужно больше океанов (расчет дистанции до min)
            int stepsRequired = min - current;
            return Math.exp(-ALPHA * stepsRequired);

        }

        List<ParameterColor> temperatureRequirement = card.getTemperatureRequirement();
        if (!CollectionUtils.isEmpty(temperatureRequirement)) {
            boolean canAmplify = specialEffectsService.ownsSpecialEffect(player, SpecialEffect.AMPLIFY_GLOBAL_REQUIREMENT);
            ParameterColor minRequirement = temperatureRequirement.getFirst();
            if (minRequirement != ParameterColor.P && canAmplify) {
                minRequirement = ParameterColor.values()[minRequirement.ordinal() - 1];
            }
            ParameterColor maxRequirement = temperatureRequirement.getLast();
            if (maxRequirement != ParameterColor.W && canAmplify) {
                maxRequirement = ParameterColor.values()[maxRequirement.ordinal() + 1];
            }

            int current = planet.getCurrentLevel(GlobalParameter.TEMPERATURE);
            int min = getTemperatureStepFromColor(minRequirement, false);
            int max = getTemperatureStepFromColor(maxRequirement, true);

            if (current >= min && current <= max) {
                return 1.0;
            }

            if (current > max) {
                return 0.0;
            }

            int stepsRequired = min - current;
            return Math.exp(-ALPHA * stepsRequired);

        }

        List<ParameterColor> oxygenRequirement = card.getOxygenRequirement();
        if (!CollectionUtils.isEmpty(oxygenRequirement)) {
            boolean canAmplify = specialEffectsService.ownsSpecialEffect(player, SpecialEffect.AMPLIFY_GLOBAL_REQUIREMENT);
            ParameterColor minRequirement = oxygenRequirement.getFirst();
            if (minRequirement != ParameterColor.P && canAmplify) {
                minRequirement = ParameterColor.values()[minRequirement.ordinal() - 1];
            }
            ParameterColor maxRequirement = oxygenRequirement.getLast();
            if (maxRequirement != ParameterColor.W && canAmplify) {
                maxRequirement = ParameterColor.values()[maxRequirement.ordinal() + 1];
            }

            int current = planet.getCurrentLevel(GlobalParameter.OXYGEN);
            int min = getOxygenStepFromColor(minRequirement, false);
            int max = getOxygenStepFromColor(maxRequirement, true);

            if (current >= min && current <= max) {
                return 1.0;
            }

            if (current > max) {
                return 0.0;
            }

            int stepsRequired = min - current;
            return Math.exp(-ALPHA * stepsRequired);

        }

        if (card instanceof PrivateInvestorBeach) {
            double minNormalizedDistance = 1.0; // 1.0 — это "максимально далеко"
            boolean anyMilestoneAvailable = false;

            for (Milestone milestone : game.getMilestones()) {
                if (milestone.isAchieved(player)) {
                    return 1.0;
                }
                if (milestone.isAchieved()) {
                    continue;
                }

                anyMilestoneAvailable = true;

                double currentProgress = (double) milestone.getValue(player, cardService) / milestone.getMaxValue();
                double distance = 1.0 - currentProgress;

                if (distance < minNormalizedDistance) {
                    minNormalizedDistance = distance;
                }
            }

            if (!anyMilestoneAvailable) return 0.0;

            return Math.exp(-ALPHA * (minNormalizedDistance * 8.0));
        }

        List<Tag> tagRequirements = card.getTagRequirements();
        if (!CollectionUtils.isEmpty(tagRequirements)) {
            Map<Tag, Long> ownedTags = cardService.countPlayedTagsAsMap(player);

            // Определяем прогресс игры для динамической гаммы (как в нейронке)
            int currentTurn = Math.max(game.getTurns(), 1);
            double progress = Math.min((double) currentTurn / Constants.ESTIMATED_TURNS_PER_GAME, 1.0);

            // 1. Считаем Науку (Science)
            int scienceRequired = 0;
            for (Tag t : tagRequirements) if (t == Tag.SCIENCE) scienceRequired++;

            if (scienceRequired > 0) {
                int owned = ownedTags.getOrDefault(Tag.SCIENCE, 0L).intValue();
                if (owned >= scienceRequired) return 1.0;

                // Динамическая гамма для науки (смягчается к концу игры)
                double gammaScience = Math.max(0.35 - (progress * 0.15), 0.15);
                int gap = scienceRequired - owned;
                return Math.exp(-gammaScience * gap);
            }

            // 2. Проверяем на "Комбо" (3+ разных типа тегов)
            long distinctNonScienceTags = tagRequirements.stream()
                    .filter(t -> t != Tag.SCIENCE)
                    .distinct()
                    .count();

            if (distinctNonScienceTags >= 3) {
                double gammaCombo = 0.7; // Самый жесткий штраф
                int maxGap = 0;
                for (Tag t : tagRequirements.stream().distinct().toList()) {
                    if (ownedTags.getOrDefault(t, 0L) == 0) {
                        maxGap = 1; // Если хоть одного тега нет, штрафуем по полной
                        break;
                    }
                }
                return Math.exp(-gammaCombo * maxGap);
            }

            // 3. Обычные одиночные/парные теги
            Map<Tag, Integer> reqMap = new HashMap<>();
            for (Tag t : tagRequirements) reqMap.put(t, reqMap.getOrDefault(t, 0) + 1);

            double minMultiplier = 1.0;
            double gammaCommon = 0.5;

            for (var entry : reqMap.entrySet()) {
                int owned = ownedTags.getOrDefault(entry.getKey(), 0L).intValue();
                if (owned < entry.getValue()) {
                    int gap = entry.getValue() - owned;
                    double currentMult = Math.exp(-gammaCommon * gap);
                    // Берем самый жесткий штраф среди всех требуемых тегов
                    if (currentMult < minMultiplier) minMultiplier = currentMult;
                }
            }
            return minMultiplier;
        }

        return 1.0;
    }

    public int getTemperatureStepFromColor(ParameterColor color, boolean isMaxBoundary) {
        return switch (color) {
            case P -> // Purple: 0-5 (Min=0, Max=5)
                    isMaxBoundary ? 5 : 0;
            case R -> // Red: 6-10 (Min=6, Max=10)
                    isMaxBoundary ? 10 : 6;
            case Y -> // Yellow: 11-15 (Min=11, Max=15)
                    isMaxBoundary ? 15 : 11;
            case W -> // White: 16-19 (Min=16, Max=19)
                    isMaxBoundary ? 19 : 16;
            // Если карта не имеет требования по цвету, или требование уже выполнено
        };
    }

    private int getOxygenStepFromColor(ParameterColor color, boolean isMaxBoundary) {
        return switch (color) {
            case P -> // Purple: 0-2 (Min=0, Max=2)
                    isMaxBoundary ? 2 : 0;
            case R -> // Red: 3-6 (Min=3, Max=6)
                    isMaxBoundary ? 6 : 3;
            case Y -> // Yellow: 7-11 (Min=7, Max=11)
                    isMaxBoundary ? 11 : 7;
            case W -> // White: 12-14 (Min=12, Max=14)
                    isMaxBoundary ? 14 : 12;
            // Если карта не имеет требования по цвету, или требование уже выполнено
        };
    }


}
