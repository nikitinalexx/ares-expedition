package com.terraforming.ares.services.policyai.service;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.cards.blue.*;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.action.ActionInputData;
import com.terraforming.ares.model.action.ActionInputDataType;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.StandardProjectService;
import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.ai.turnProcessors.AiTurnService;
import com.terraforming.ares.services.policyai.ResourceHelper;
import com.terraforming.ares.services.policyai.action.ActionHead;
import com.terraforming.ares.services.policyai.action.ActionInputService;
import com.terraforming.ares.services.policyai.action.HeadAction;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;
import com.terraforming.ares.services.policyai.encoder.PolicyTableAndHandEncoder;
import com.terraforming.ares.services.policyai.input.*;
import com.terraforming.ares.validation.action.ActionValidator;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PolicyActionProcessor extends AbstractPolicyProcessor {
    private final PolicyTableAndHandEncoder encoder;
    private final CardService cardService;
    private final AiTurnService aiTurnService;
    private final List<InputGeneratorEffect> tagGeneratorEffects;
    private final List<InputGeneratorEffect> inputGeneratorEffects;
    private final Map<Class<?>, ActionValidator<?>> blueActionValidators;
    private final StandardProjectService standardProjectService;


    public PolicyActionProcessor(List<ActionValidator<?>> validators, StandardProjectService standardProjectService, AiTurnService aiTurnService, CardService cardService, PolicyTableAndHandEncoder encoder, Phase1UpgradeInputEffect phase1UpgradeInputEffect, Phase2UpgradeInputEffect phase2UpgradeInputEffect, Phase3UpgradeInputEffect phase3UpgradeInputEffect, Phase4UpgradeInputEffect phase4UpgradeInputEffect, UniversalPhaseUpgradeInputEffect universalPhaseUpgradeInputEffect, DoublePhaseUpgradeInputEffect doublePhaseUpgradeInputEffect, BiomedicalImportsInputEffect biomedicalImportsInputEffect, CryogenicShipmentInputEffect cryogenicShipmentInputEffect, EosChasmaNationalParkInputEffect eosChasmaNationalParkInputEffect, ViralEnhancersInputEffect viralEnhancersInputEffect, DecomposersInputEffect decomposersInputEffect, MarsUniversityInputEffect marsUniversityInputEffect, ImportedNitrogenInputEffect importedNitrogenInputEffect, LargeConvoyInputEffect largeConvoyInputEffect, LocalHeatTrappingInputEffect localHeatTrappingInputEffect, AstrofarmInputEffect astrofarmInputEffect, CeosFavoriteProjectInputEffect ceosFavoriteProjectInputEffect, SyntheticCatastropheInputEffect syntheticCatastropheInputEffect, ImportedHydrogenInputEffect importedHydrogenInputEffect) {
        this.encoder = encoder;
        this.cardService = cardService;
        this.aiTurnService = aiTurnService;
        this.standardProjectService = standardProjectService;

        tagGeneratorEffects = List.of(
                viralEnhancersInputEffect,
                decomposersInputEffect,
                marsUniversityInputEffect
        );

        inputGeneratorEffects = new ArrayList<>();
        inputGeneratorEffects.add(phase1UpgradeInputEffect);
        inputGeneratorEffects.add(phase2UpgradeInputEffect);
        inputGeneratorEffects.add(phase3UpgradeInputEffect);
        inputGeneratorEffects.add(phase4UpgradeInputEffect);
        inputGeneratorEffects.add(universalPhaseUpgradeInputEffect);
        inputGeneratorEffects.add(doublePhaseUpgradeInputEffect);
        inputGeneratorEffects.add(biomedicalImportsInputEffect);
        inputGeneratorEffects.add(cryogenicShipmentInputEffect);
        inputGeneratorEffects.add(eosChasmaNationalParkInputEffect);
        inputGeneratorEffects.add(viralEnhancersInputEffect);
        inputGeneratorEffects.add(decomposersInputEffect);
        inputGeneratorEffects.add(marsUniversityInputEffect);
        inputGeneratorEffects.add(importedNitrogenInputEffect);
        inputGeneratorEffects.add(largeConvoyInputEffect);
        inputGeneratorEffects.add(localHeatTrappingInputEffect);
        inputGeneratorEffects.add(astrofarmInputEffect);
        inputGeneratorEffects.add(ceosFavoriteProjectInputEffect);
        inputGeneratorEffects.add(syntheticCatastropheInputEffect);
        inputGeneratorEffects.add(importedHydrogenInputEffect);

        blueActionValidators = validators.stream().collect(
                Collectors.toMap(
                        ActionValidator::getType,
                        Function.identity()
                )
        );
    }


    public void processTurn(MarsGame game, Player player, List<TurnType> possibleTurns) {
        if (doImmediateUnconditionalActions(game, player)) {
            return;
        }

        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        Player anotherPlayer = players.get(0) == player ? players.get(1) : players.get(0);

        PolicyRecord record = new PolicyRecord();
        TableContext context = encoder.encode(game, player, anotherPlayer, record);

        boolean isHelion = player.getSelectedCorporationCard() != null
                && cardService.getCard(player.getSelectedCorporationCard()).getCardMetadata().getCardAction() == CardAction.HELION_CORPORATION;


        boolean soldCards = false;
        boolean exchangedHelion = false;

        while (true) {
            List<HeadAction> validActions = new ArrayList<>();

            List<Card> possibleBuilds = getAvailableProjects(game, player);
            Map<HeadAction, Card> actionToBuildCard = new HashMap<>();

            for (Card possibleBuild : possibleBuilds) {
                HeadAction action = ActionInputService.buildProjectAction(possibleBuild);
                validActions.add(action);
                actionToBuildCard.put(action, possibleBuild);
            }

            Map<HeadAction, Card> actionToBlueCard = new HashMap<>();
            for (Card playableBlueCard : collectPlayableBlueCards(game, player, context)) {
                HeadAction action = ActionInputService.getBlueAction(playableBlueCard);
                validActions.add(action);
                actionToBlueCard.put(action, playableBlueCard);
            }

            if (possibleTurns.contains(TurnType.UNMI_RT)) {
                validActions.add(ActionInputService.unmiAction());
            }

            if (!player.getHand().isEmpty() && (!soldCards || possibleTurns.contains(TurnType.GAME_END_CONFIRM))) {
                validActions.add(ActionInputService.enterSellingModeAction());
            }

            if (isHelion && player.getHeat() > 0 && !exchangedHelion) {
                validActions.add(ActionInputService.enterHelionModeAction());
            }

            Map<HeadAction, StandardProjectType> actionToStandardProject = new HashMap<>();
            for (StandardProjectType standardProjectType : List.of(StandardProjectType.FOREST, StandardProjectType.OCEAN, StandardProjectType.TEMPERATURE)) {
                String validationResult = standardProjectService.validateStandardProject(game, player, standardProjectType);
                if (validationResult != null) {
                    continue;
                }
                HeadAction standardAction = ActionInputService.standardProject(standardProjectType);
                actionToStandardProject.put(standardAction, standardProjectType);
                validActions.add(standardAction);
            }

            if (possibleTurns.contains(TurnType.INCREASE_TEMPERATURE)) {
                validActions.add(ActionInputService.convertHeatToTemperature());
            }

            if (possibleTurns.contains(TurnType.PLANT_FOREST)) {
                validActions.add(ActionInputService.convertPlantsToForest());
            }

            if (player.canBuildAny(List.of(BuildType.BLUE_RED_OR_MC, BuildType.BLUE_RED_OR_CARD))) {
                validActions.add(ActionInputService.takeBonusAction());
            }

            if (possibleTurns.contains(TurnType.SKIP_TURN)
                    || possibleTurns.contains(TurnType.GAME_END_CONFIRM) && !possibleTurns.contains(TurnType.INCREASE_TEMPERATURE) && !possibleTurns.contains(TurnType.PLANT_FOREST) && actionToStandardProject.isEmpty() && player.getHand().isEmpty()) {
                validActions.add(ActionInputService.passAction());
            }

            HeadAction bestTurn = predict(record, validActions);

            if (bestTurn == ActionInputService.enterSellingModeAction()) {
                doCardExchange(record, game, player);
                soldCards = true;
                exchangedHelion = false;
                continue;
            }

            if (bestTurn == ActionInputService.enterHelionModeAction()) {
                doHelionExchange(record, player);
                exchangedHelion = true;
                soldCards = false;
                continue;
            }

            if (bestTurn == ActionInputService.takeBonusAction()) {
                aiTurnService.pickExtraBonusTurnAsync(player);
                return;
            }

            if (bestTurn == ActionInputService.unmiAction()) {
                aiTurnService.unmiRtCorporationTurn(game, player);
                return;
            }

            if (bestTurn == ActionInputService.convertHeatToTemperature()) {
                aiTurnService.increaseTemperature(game, player);
                return;
            }

            if (bestTurn == ActionInputService.convertPlantsToForest()) {
                aiTurnService.plantForest(game, player);
                return;
            }

            if (bestTurn == ActionInputService.passAction()) {
                if (possibleTurns.contains(TurnType.GAME_END_CONFIRM)) {
                    aiTurnService.confirmGameEnd(game, player);
                    return;
                } else {
                    aiTurnService.skipTurn(player);
                    return;
                }
            }

            if (bestTurn.head() == ActionHead.BUILD) {
                Card cardToBuild = actionToBuildCard.get(bestTurn);

                doBuild(cardToBuild, record, inputGeneratorEffects, context);
                return;
            }

            if (actionToStandardProject.containsKey(bestTurn)) {
                aiTurnService.standardProjectTurn(game, player, actionToStandardProject.get(bestTurn));
                return;
            }

            if (actionToBlueCard.containsKey(bestTurn)) {
                Card blueCardToActivate = actionToBlueCard.get(bestTurn);
                executeBlueCardAction(game, player, blueCardToActivate, record, context);
                return;
            }

            break;
        }
        throw new IllegalStateException("Unreachable");

        //TODO make it play step by step with real player
    }


    public void executeBlueCardAction(MarsGame game, Player player, Card card, PolicyRecord record, TableContext context) {
        if (AiConstants.NO_PAYMENT_BLUE_ACTIONS.contains(card.getClass())) {
            aiTurnService.performBlueAction(game, player, card.getId(), Map.of());
            return;
        }

        Map<Integer, List<Integer>> input = new HashMap<>();

        if (card.getClass() == ConservedBiome.class) {
            List<Card> collectingCards = ResourceHelper.getAnyAvailableAnimalsAndMicrobes(context);
            Map<HeadAction, Card> actionToCard = new HashMap<>();
            collectingCards.forEach(c -> {
                if (c.getCollectableResource() == CardCollectableResource.ANIMAL) {
                    actionToCard.put(ActionInputService.getAnimalTargetAction(c), c);
                } else {
                    actionToCard.put(ActionInputService.getMicrobeTargetAction(c), c);
                }
            });
            record.conservedBiomeAction = true;
            HeadAction best = predict(record, new ArrayList<>(actionToCard.keySet()));
            record.conservedBiomeAction = false;
            input.put(InputFlag.CARD_CHOICE.getId(), List.of(actionToCard.get(best).getId()));
        } else if (card.getClass() == DecomposingFungus.class) {
            List<Card> microbeCards = ResourceHelper.getCardsWithAtLeastOneMicrobeOrAnimal(context);
            Map<HeadAction, Card> actionToCard = new HashMap<>();
            microbeCards.forEach(c -> {
                if (c.getCollectableResource() == CardCollectableResource.ANIMAL) {
                    actionToCard.put(ActionInputService.getAnimalTargetAction(c), c);
                } else {
                    actionToCard.put(ActionInputService.getMicrobeTargetAction(c), c);
                }
            });
            record.decomposingFungus = true;
            HeadAction best = predict(record, new ArrayList<>(actionToCard.keySet()));
            record.decomposingFungus = false;
            input.put(InputFlag.CARD_CHOICE.getId(), List.of(actionToCard.get(best).getId()));
        } else if (card.getClass() == ExtremeColdFungus.class) {
            List<Card> microbeCards = ResourceHelper.getAnyAvailableMicrobes(context);

            if (microbeCards.isEmpty()) {
                aiTurnService.performBlueAction(game, player, card.getId(), Map.of(InputFlag.EXTEME_COLD_FUNGUS_PICK_PLANT.getId(), List.of(0)));
                return;
            }
            Map<HeadAction, Card> actionToCard = new LinkedHashMap<>();
            microbeCards.forEach(c -> actionToCard.put(ActionInputService.getMicrobeTargetAction(c), c));
            List<HeadAction> validActions = new ArrayList<>(actionToCard.keySet());
            validActions.add(ActionInputService.takePlantTargetAction());
            record.extremeColdFungus = true;
            HeadAction best = predict(record, validActions);
            record.extremeColdFungus = false;
            if (best == ActionInputService.takePlantTargetAction()) {
                input.put(InputFlag.EXTEME_COLD_FUNGUS_PICK_PLANT.getId(), List.of(0));
            } else {
                input.put(InputFlag.EXTREME_COLD_FUNGUS_PUT_MICROBE.getId(), List.of(actionToCard.get(best).getId()));
            }
        } else if (card.getClass() == SymbioticFungus.class) {
            List<Card> microbeCards = ResourceHelper.getAnyAvailableMicrobes(context);
            Map<HeadAction, Card> actionToCard = new LinkedHashMap<>();
            microbeCards.forEach(c -> actionToCard.put(ActionInputService.getMicrobeTargetAction(c), c));
            record.microbePutCount = 1;
            HeadAction best = predict(record, new ArrayList<>(actionToCard.keySet()));
            record.microbePutCount = 0;
            input.put(InputFlag.CARD_CHOICE.getId(), List.of(actionToCard.get(best).getId()));
        } else if (card.getClass() == FarmingCoops.class || card.getClass() == MatterGenerator.class) {
            if (player.getHand().size() == 1) {
                input.put(InputFlag.CARD_CHOICE.getId(), List.of(player.getHand().getCards().getFirst()));
            } else {
                Map<HeadAction, Card> actionToCard = new HashMap<>();
                player.getHand().getCards().forEach(id -> {
                    Card c = cardService.getCard(id);
                    actionToCard.put(ActionInputService.sellDiscardCardAction(c), c);
                });
                if (card.getClass() == FarmingCoops.class) {
                    record.farmingCoops = true;
                }
                if (card.getClass() == MatterGenerator.class) {
                    record.matterGenerator = true;
                }
                HeadAction best = predict(record, new ArrayList<>(actionToCard.keySet()));

                record.farmingCoops = false;
                record.matterGenerator = false;

                input.put(InputFlag.CARD_CHOICE.getId(), List.of(actionToCard.get(best).getId()));
            }
        } else if (card.getClass() == GreenHouses.class) {
            if (player.getHeat() == 1) {
                input.put(InputFlag.DISCARD_HEAT.getId(), List.of(1));
            } else {
                Map<HeadAction, Integer> actionToCount = new HashMap<>();
                for (int i = 1; i <= 4 && i <= player.getHeat(); i++) {
                    actionToCount.put(ActionInputService.action1To4Count(i), i);
                }
                record.greenHouses = true;
                HeadAction best = predict(record, new ArrayList<>(actionToCount.keySet()));
                record.greenHouses = false;
                input.put(InputFlag.DISCARD_HEAT.getId(), List.of(actionToCount.get(best)));
            }
        } else if (card.getClass() == GhgProductionBacteria.class
                || card.getClass() == NitriteReductingBacteria.class
                || card.getClass() == RegolithEaters.class
                || card.getClass() == SelfReplicatingBacteria.class) {
            int microbesOnCard = player.getCardResourcesCount().get(card.getClass());
            int max = card.getCardMetadata().getActionsInputData().getFirst().getMax();
            if (microbesOnCard < max) {
                input.put(InputFlag.ADD_DISCARD_MICROBE.getId(), List.of(1));
            } else {
                if (card.getClass() == GhgProductionBacteria.class) record.ghgProduction = true;
                if (card.getClass() == NitriteReductingBacteria.class) record.nitriteReductingBacteria = true;
                if (card.getClass() == RegolithEaters.class) record.regolithEaters = true;
                if (card.getClass() == SelfReplicatingBacteria.class) record.selfReplicatingBacteria = true;
                HeadAction best = predict(record, List.of(
                        ActionInputService.actionTakeMicrobe(),
                        ActionInputService.actionUseMicrobe()
                ));
                record.ghgProduction = false;
                record.nitriteReductingBacteria = false;
                record.regolithEaters = false;
                record.selfReplicatingBacteria = false;
                input.put(InputFlag.ADD_DISCARD_MICROBE.getId(), List.of(best == ActionInputService.actionTakeMicrobe() ? 1 : max));
            }
        } else if (card.getClass() == PowerInfrastructure.class) {
            if (player.getHeat() == 1) {
                input.put(InputFlag.DISCARD_HEAT.getId(), List.of(1));
            } else {
                record.powerInfrastructure = true;
                int count = 0;
                while (record.heat[0] > 0) {
                    HeadAction best = predict(record, List.of(
                            ActionInputService.exchange1Heat(),
                            ActionInputService.passAction()
                    ));
                    if (best == ActionInputService.passAction()) break;
                    record.mc[0]++;
                    record.heat[0]--;
                    count++;
                }
                record.powerInfrastructure = false;
                input.put(InputFlag.DISCARD_HEAT.getId(), List.of(count));
            }
        } else if (card.getClass() == RedraftedContracts.class) {
            if (player.getHand().size() == 1) {
                input.put(InputFlag.CARD_CHOICE.getId(), List.of(player.getHand().getCards().getFirst()));
            } else {
                record.redraftedContracts = true;
                record.redraftedDiscarded = 0;
                List<Integer> cardsToDiscard = new ArrayList<>();

                Map<HeadAction, Card> actionToCard = new LinkedHashMap<>();
                player.getHand().getCards().forEach(id -> {
                    Card c = cardService.getCard(id);
                    actionToCard.put(ActionInputService.sellDiscardCardAction(c), c);
                });
                List<HeadAction> validActions = new ArrayList<>(actionToCard.keySet());

                while (record.handSize[0] > 0 && record.redraftedDiscarded < 3) {
                    if (record.redraftedDiscarded == 1) {
                        validActions.add(ActionInputService.passAction());
                    }

                    HeadAction best = predict(record, validActions);
                    if (best == ActionInputService.passAction()) break;

                    Card toDiscard = actionToCard.get(best);
                    cardsToDiscard.add(toDiscard.getId());
                    validActions.remove(best);
                    actionToCard.remove(best);
                    record.removeCardFromHand(toDiscard);
                    record.redraftedDiscarded++;
                }

                record.redraftedContracts = false;
                record.redraftedDiscarded = 0;
                input.put(InputFlag.CARD_CHOICE.getId(), cardsToDiscard);
            }
        } else if (card.getClass() == ExperimentalTechnology.class || card.getClass() == VirtualEmployeeDevelopment.class) {
            record.universalPhaseUpgradeCount = 1;
            generatePhaseUpgrade(record, input);
            record.universalPhaseUpgradeCount = 0;
        } else if (card.getClass() == FibrousCompositeMaterial.class) {
            int microbesOnCard = player.getCardResourcesCount().get(card.getClass());
            int max = card.getCardMetadata().getActionsInputData().getFirst().getMax();
            if (microbesOnCard < max) {
                input.put(InputFlag.ADD_DISCARD_MICROBE.getId(), List.of(1));
            } else {
                record.fibrousComposite = true;
                HeadAction best = predict(record, List.of(
                        ActionInputService.actionTakeMicrobe(),
                        ActionInputService.actionUseMicrobe()
                ));
                record.fibrousComposite = false;
                if (best == ActionInputService.actionTakeMicrobe()) {
                    input.put(InputFlag.ADD_DISCARD_MICROBE.getId(), List.of(1));
                } else {
                    input.put(InputFlag.ADD_DISCARD_MICROBE.getId(), List.of(max));
                    record.universalPhaseUpgradeCount = 1;
                    generatePhaseUpgrade(record, input);
                    record.universalPhaseUpgradeCount = 0;
                }
            }
        } else if (card.getClass() == ResearchGrant.class) {
            Map<Integer, List<Integer>> tagInput = generateDynamicTagInput(record, context);
            input.putAll(tagInput);
            for (InputGeneratorEffect effect : tagGeneratorEffects) {
                if (effect.isPresent(record, context, card, input)) {
                    effect.generateAndApply(record, context, input, card);
                }
            }
        }

        aiTurnService.performBlueAction(game, player, card.getId(), input);
    }

    private Map<Integer, List<Integer>> generateDynamicTagInput(PolicyRecord record, TableContext context) {
        List<Tag> researchGrantTags = context.getPlayer().getCardToTag().get(ResearchGrant.class);
        Map<HeadAction, Integer> actionToTag = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            Tag tagCandidate = Tag.values()[i];
            if (researchGrantTags.contains(tagCandidate)) {
                continue;
            }
            HeadAction possibleAction = ActionInputService.chooseTag(i);
            actionToTag.put(possibleAction, i);
        }
        record.choosingTag = true;
        HeadAction bestTagAction = predict(record, new ArrayList<>(actionToTag.keySet()));
        record.choosingTag = false;

        return Map.of(InputFlag.TAG_INPUT.getId(), List.of(actionToTag.get(bestTagAction)));
    }

    private boolean doImmediateUnconditionalActions(MarsGame game, Player player) {
        Deck activatedBlueCards = player.getActivatedBlueCards();

        Map<Class<?>, Card> availableCards = player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .filter(Card::isActiveCard)
                .filter(card -> !activatedBlueCards.containsCard(card.getId()))
                .collect(Collectors.toMap(Card::getClass, Function.identity()));

        for (Map.Entry<Class<?>, Card> cardEntry : availableCards.entrySet()) {
            if (AiConstants.NO_PAYMENT_BLUE_ACTIONS.contains(cardEntry.getKey())) {
                aiTurnService.performBlueAction(game, player, cardEntry.getValue().getId(), Map.of());
                return true;
            }
        }
        return false;
    }

    private List<Card> collectPlayableBlueCards(MarsGame game, Player player, TableContext tableContext) {
        List<Card> availableTurns = new ArrayList<>();

        Deck activatedBlueCards = player.getActivatedBlueCards();
        Deck activatedBlueCardsTwice = player.getActivatedBlueCardsTwice();
        boolean hasExtraActivation = player.getBlueActionExtraActivationsLeft() > 0;

        List<Card> availableCards = player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .filter(Card::isActiveCard)
                .filter(card ->
                        !activatedBlueCards.containsCard(card.getId()) ||
                                (hasExtraActivation && !activatedBlueCardsTwice.containsCard(card.getId()))
                )
                .toList();

        for (Card card : availableCards) {
            if (isPlayableCard(game, player, card, tableContext)) {
                availableTurns.add(card);
            }
        }

        return availableTurns;
    }

    @SuppressWarnings("unchecked")
    public boolean isPlayableCard(MarsGame game, Player player, Card card, TableContext tableContext) {
        ActionValidator<Card> validator = (ActionValidator<Card>) blueActionValidators.get(card.getClass());

        if (validator == null) {
            return true;
        }

        if (AiConstants.ACTIONS_WITHOUT_INPUT_PARAMS.contains(card.getClass())) {
            String validationResult = validator.validate(game, player);
            if (validationResult != null) {
                return false;
            }
        }

        CardMetadata metadata = card.getCardMetadata();

        List<ActionInputData> actionsInputData = metadata.getActionsInputData();
        if (actionsInputData.isEmpty()) {
            return checkActionWithoutInputData(metadata, player);
        }

        return checkActionWithInputData(metadata, actionsInputData.getFirst(), player, tableContext);
    }

    private boolean checkActionWithoutInputData(CardMetadata metadata, Player player) {
        CardAction action = metadata.getCardAction();

        if (action == CardAction.RESEARCH_GRANT) {
            List<Tag> cardTags = player.getCardToTag().get(ResearchGrant.class);
            return cardTags.stream().anyMatch(tag -> tag == Tag.DYNAMIC);
        }

        if (action == CardAction.EXPERIMENTAL_TECHNOLOGY) {
            return player.getTerraformingRating() > 0;
        }

        return true;
    }

    private boolean checkActionWithInputData(CardMetadata metadata, ActionInputData inputData, Player player, TableContext tableContext) {
        CardAction action = metadata.getCardAction();

        if (action == CardAction.DECOMPOSING_FUNGUS) {
            return hasCardsWithResourceAndCount(player);
        }

        if (action == CardAction.CONSERVED_BIOME) {
            return ResourceHelper.countAvailableAnimalsAndMicrobes(tableContext) > 0;
        }

        if (inputData.getType() == ActionInputDataType.DISCARD_CARD) {
            return !player.getHand().isEmpty();
        }

        if (action == CardAction.POWER_INFRASTRUCTURE || action == CardAction.GREEN_HOUSES) {
            return player.getHeat() > 0;
        }

        if (action == CardAction.SYMBIOTIC_FUNGUD) {
            return ResourceHelper.countPlayedMicrobeCards(tableContext) > 0;
        }

        return true;
    }

    private boolean hasCardsWithResourceAndCount(Player player) {
        return player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .filter(c -> c.getCollectableResource() == CardCollectableResource.MICROBE || c.getCollectableResource() == CardCollectableResource.ANIMAL)
                .anyMatch(c -> player.getCardResourcesCount().get(c.getClass()) > 0);
    }

}
