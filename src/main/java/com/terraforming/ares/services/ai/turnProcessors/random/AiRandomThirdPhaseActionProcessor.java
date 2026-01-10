package com.terraforming.ares.services.ai.turnProcessors.random;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.cards.blue.ResearchGrant;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.action.ActionInputData;
import com.terraforming.ares.model.action.ActionInputDataType;
import com.terraforming.ares.model.payments.Payment;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.CardValidationService;
import com.terraforming.ares.services.StandardProjectService;
import com.terraforming.ares.services.TerraformingService;
import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.ai.AiDiscoveryDecisionService;
import com.terraforming.ares.services.ai.AiEndgameService;
import com.terraforming.ares.services.ai.dto.ActionInputParamsResponse;
import com.terraforming.ares.services.ai.dto.AvailableTurn;
import com.terraforming.ares.services.ai.dto.AvailableTurnType;
import com.terraforming.ares.services.ai.dto.BuildContext;
import com.terraforming.ares.services.ai.turnProcessors.AiTurnService;
import com.terraforming.ares.validation.action.ActionValidator;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.terraforming.ares.services.ai.dto.AvailableTurnType.*;

@Component
public class AiRandomThirdPhaseActionProcessor {
    private final Map<Class<?>, ActionValidator<?>> blueActionValidators;
    private final CardService cardService;
    private final StandardProjectService standardProjectService;
    private final AiEndgameService aiEndgameService;
    private final AiTurnService aiTurnService;
    private final AiDiscoveryDecisionService aiDiscoveryDecisionService;
    private final TerraformingService terraformingService;
    private final AiRandomCardBuildParamsService aiRandomCardBuildParamsService;
    private final AiRandomPaymentService aiRandomPaymentService;
    private final CardValidationService cardValidationService;
    private final AiRandomSellCardsService aiRandomSellCardsService;
    private final Random random = new Random();

    public AiRandomThirdPhaseActionProcessor(List<ActionValidator<?>> validators,
                                             CardService cardService,
                                             StandardProjectService standardProjectService,
                                             AiEndgameService aiEndgameService,
                                             AiTurnService aiTurnService,
                                             AiDiscoveryDecisionService aiDiscoveryDecisionService,
                                             TerraformingService terraformingService,
                                             AiRandomCardBuildParamsService aiRandomCardBuildParamsService,
                                             AiRandomPaymentService aiRandomPaymentService,
                                             CardValidationService cardValidationService, AiRandomSellCardsService aiRandomSellCardsService) {
        blueActionValidators = validators.stream().collect(
                Collectors.toMap(
                        ActionValidator::getType,
                        Function.identity()
                )
        );
        this.cardService = cardService;
        this.standardProjectService = standardProjectService;
        this.aiEndgameService = aiEndgameService;
        this.aiTurnService = aiTurnService;
        this.aiDiscoveryDecisionService = aiDiscoveryDecisionService;
        this.terraformingService = terraformingService;
        this.aiRandomCardBuildParamsService = aiRandomCardBuildParamsService;
        this.aiRandomPaymentService = aiRandomPaymentService;
        this.cardValidationService = cardValidationService;
        this.aiRandomSellCardsService = aiRandomSellCardsService;
    }

    public boolean processTurn(MarsGame game, Player player, List<TurnType> possibleTurns) {
        aiRandomSellCardsService.sellCardsAsyncByChance(player);

        boolean builtAProject = buildProjectIfPossible(game, player);
        if (builtAProject) {
            return true;
        }

        List<AvailableTurn> availableTurns = getAvailableTurns(game, player, possibleTurns);
        if (performOneOfAvailableTurns(game, player, availableTurns)) {
            return true;
        }

        if (availableTurns.isEmpty()) {
            Card corporation = cardService.getCard(player.getSelectedCorporationCard());

            if (corporation.getCardMetadata().getCardAction() == CardAction.HELION_CORPORATION && player.getHeat() > 0) {//can do as async turn in 3rd phase
                convertHelionHeat(game, player);
            }
        }

        if (aiEndgameService.doFinalActionsIfGameFinished(game, player)) {
            return true;
        }

        if (aiEndgameService.isFinishingGame(game, player)) {
            return true;
        }

        aiTurnService.skipTurn(player);
        return true;
    }

    private boolean performOneOfAvailableTurns(MarsGame game, Player player, List<AvailableTurn> availableTurns) {
        if (availableTurns.isEmpty()) {
            return false;
        }

        AvailableTurn availableTurn = availableTurns.get(random.nextInt(availableTurns.size()));
        switch (availableTurn.getType()) {
            case UNMI_RT -> {
                aiTurnService.unmiRtCorporationTurn(game, player);
                return true;
            }
            case CONVERT_TEMPERATURE -> {
                aiTurnService.increaseTemperature(game, player);
                return true;
            }
            case CONVERT_PLANTS -> {
                aiTurnService.plantForest(game, player);
                return true;
            }
            case CONVERT_INFRASTRUCTURE -> {
                aiTurnService.increaseInfrastructure(player, game, Map.of());
                return true;
            }
            case STANDARD_PROJECT -> {
                List<StandardProjectType> availableStandardProjects = standardProjectService.getAvailableStandardProjects(game, player);
                player.setAiMadeStandardAction(player.getAiMadeStandardAction() + 1);
                aiTurnService.standardProjectTurn(game, player, availableStandardProjects.get(random.nextInt(availableStandardProjects.size())));
                return true;
            }
            case BLUE_ACTION -> {
                Card blueCard = cardService.getCard(availableTurn.getCard());
                ActionInputParamsResponse inputParams = getActionInputParams(game, player, blueCard);

                aiTurnService.performBlueAction(
                        game,
                        player,
                        blueCard.getId(),
                        inputParams.getInputParams()
                );
                return true;
            }
        }
        throw new IllegalStateException("Unreachable for " + availableTurn.getType());
    }

    private void convertHelionHeat(MarsGame game, Player player) {
        int heat = player.getHeat();
        int convert = 0;

        if (game.getPlanet().isTemperatureMax()) {
            // 🔥 Температура максимум — оставляем запас
            int keep = 6 + random.nextInt(5); // 6..10
            keep = Math.min(keep, heat);

            convert = heat - keep;

        } else {
            // 🌡 Температура не максимум — иногда немного конвертим
            if (heat > 1 && random.nextInt(10) <= 3) {
                convert = 1 + random.nextInt(Math.min(5, heat - 1));
            }
        }

        if (convert > 0) {
            player.setHeat(heat - convert);
            player.setMc(player.getMc() + convert);
        }
    }

    private List<AvailableTurn> getAvailableTurns(MarsGame game, Player player, List<TurnType> possibleTurns) {
        List<AvailableTurn> availableTurns = new ArrayList<>();
        List<AvailableTurn> blueCardsTurns = collectBlueCardsTurns(game, player);
        availableTurns.addAll(blueCardsTurns);
        availableTurns.addAll(convertPlantsHeatTurns(possibleTurns));
        boolean addedUnmi = false;
        if (possibleTurns.contains(TurnType.UNMI_RT) && player.getMc() >= 6) {
            availableTurns.add(new AvailableTurn(UNMI_RT));
            addedUnmi = true;
        }
        if (possibleTurns.contains(TurnType.STANDARD_PROJECT) && !(player.getAiMadeStandardAction() >= getStandardActionLimit(player)) && blueCardsTurns.isEmpty() && !addedUnmi && standardProjectService.canPerformAnyStandardProject(game, player)) {
            if (shouldAllowStandardProject(player)) {
                availableTurns.add(new AvailableTurn(STANDARD_PROJECT));
            } else {
                player.setAiMadeStandardAction(999);//skip for the rest of the phase
            }
        }

        return availableTurns;
    }

    private boolean shouldAllowStandardProject(Player player) {
        int mc = player.getMc();

        if (mc < 20) return false;          // early: почти никогда
        if (mc > 120) return true;          // late: почти всегда

        // линейная вероятность между 20 и 120
        double p = (mc - 20) / 100.0;       // от 0 до 1
        return random.nextDouble() < p;
    }


    private int getStandardActionLimit(Player player) {
        return player.getMc() / 100 + 1;
    }

    private boolean buildProjectIfPossible(MarsGame game, Player player) {
        if (!player.getBuilds().isEmpty() && player.getBuildContext() != null) {
            BuildContext buildContext = player.getBuildContext();
            player.setBuildContext(null);
            Card cardToBuild = cardService.getCard(buildContext.getCardId());
            aiTurnService.buildProject(game, player, cardToBuild.getId(), buildContext.getPayments(), buildContext.getInputParams());
            return true;
        }
        if (!player.getBuilds().isEmpty()) {
            List<Integer> cards = new ArrayList<>(player.getHand().getCards());
            Collections.shuffle(cards);

            return cards.stream().map(cardService::getCard).anyMatch(card -> {
                Map<Integer, List<Integer>> inputParams = aiRandomCardBuildParamsService.getInputParamsForBuild(game, player, card);
                if (inputParams == null) {
                    return false;
                }
                List<Payment> payments = aiRandomPaymentService.getCardPayments(game, player, card, inputParams);

                String errorMessage = cardValidationService.validateCard(player, game, card.getId(), payments, inputParams);
                if (errorMessage != null) {
                    return false;
                }
                aiTurnService.buildProject(game, player, card.getId(), payments, inputParams);
                return true;
            });
        }

        return false;
    }

    private List<AvailableTurn> collectBlueCardsTurns(MarsGame game, Player player) {
        List<AvailableTurn> availableTurns = new ArrayList<>();

        Deck activatedBlueCards = player.getActivatedBlueCards();
        Deck activatedBlueCardsTwice = player.getActivatedBlueCardsTwice();
        boolean hasExtraActivation = player.getBlueActionExtraActivationsLeft() > 0;

        List<Card> availableCards = player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .filter(Card::isActiveCard)
                .filter(card ->
                        !activatedBlueCards.containsCard(card.getId()) ||
                                (hasExtraActivation && !activatedBlueCardsTwice.containsCard(card.getId()) && !isPowerInfrastructureCard(card))
                )
                .toList();

        for (Card card : availableCards) {
            if (isUsablePlayAction(game, player, card)) {
                availableTurns.add(new AvailableTurn(AvailableTurnType.BLUE_ACTION, card.getId()));
            }
        }

        return availableTurns;
    }

    private boolean isPowerInfrastructureCard(Card card) {
        if (card.getCardMetadata() == null) {
            return false;
        }
        return card.getCardMetadata().getCardAction() == CardAction.POWER_INFRASTRUCTURE;
    }

    private List<AvailableTurn> convertPlantsHeatTurns(List<TurnType> possibleTurns) {
        List<AvailableTurn> availableTurns = new ArrayList<>(3);
        if (possibleTurns.contains(TurnType.INCREASE_TEMPERATURE)) {
            availableTurns.add(new AvailableTurn(CONVERT_TEMPERATURE));
        }
        if (possibleTurns.contains(TurnType.PLANT_FOREST)) {
            availableTurns.add(new AvailableTurn(CONVERT_PLANTS));
        }
        if (possibleTurns.contains(TurnType.INCREASE_INFRASTRUCTURE)) {
            availableTurns.add(new AvailableTurn(CONVERT_INFRASTRUCTURE));
        }
        return availableTurns;
    }

    @SuppressWarnings("unchecked")
    public boolean isUsablePlayAction(MarsGame game, Player player, Card card) {
        ActionValidator<Card> validator = (ActionValidator<Card>) blueActionValidators.get(card.getClass());

        if (validator == null) {
            return true;
        }

        if (AiConstants.ACTIONS_WITHOUT_INPUT_PARAMS.contains(card.getClass())) {
            return validator.validate(game, player) == null;
        }

        CardMetadata metadata = card.getCardMetadata();
        if (metadata == null) {
            throw new IllegalStateException("NOT REACHABLE");
        }

        List<ActionInputData> actionsInputData = metadata.getActionsInputData();
        if (actionsInputData.isEmpty()) {
            return checkActionWithoutInputData(metadata, player);
        }

        return checkActionWithInputData(metadata, actionsInputData.get(0), player);
    }

    private boolean checkActionWithoutInputData(CardMetadata metadata, Player player) {
        CardAction action = metadata.getCardAction();

        if (action == CardAction.RESEARCH_GRANT) {
            List<Tag> cardTags = player.getCardToTag().get(ResearchGrant.class);
            return cardTags.stream().anyMatch(tag -> tag == Tag.DYNAMIC);
        }

        if (action == CardAction.EXPERIMENTAL_TECHNOLOGY) {
            return player.getTerraformingRating() > 0
                    && player.getPhaseCards().stream().anyMatch(phase -> phase == 0);
        }

        return true;
    }

    private boolean checkActionWithInputData(CardMetadata metadata, ActionInputData inputData, Player player) {
        CardAction action = metadata.getCardAction();

        if (action == CardAction.DECOMPOSING_FUNGUS) {
            return hasCardsWithResourceAndCount(player, CardCollectableResource.MICROBE);
        }

        if (action == CardAction.CONSERVED_BIOME) {
            return hasCardsWithResource(player, CardCollectableResource.MICROBE, CardCollectableResource.ANIMAL);
        }

        if (inputData.getType() == ActionInputDataType.DISCARD_CARD) {
            return !player.getHand().isEmpty();
        }

        if (action == CardAction.POWER_INFRASTRUCTURE || action == CardAction.GREEN_HOUSES) {
            return player.getHeat() > 0;
        }

        if (action == CardAction.SYMBIOTIC_FUNGUD) {
            return hasCardsWithResource(player, CardCollectableResource.MICROBE);
        }

        return true;
    }

    private boolean hasCardsWithResource(Player player, CardCollectableResource... resources) {
        Set<CardCollectableResource> resourceSet = Set.of(resources);
        return player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .anyMatch(c -> resourceSet.contains(c.getCollectableResource()));
    }

    private boolean hasCardsWithResourceAndCount(Player player, CardCollectableResource resource) {
        return player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .filter(c -> c.getCollectableResource() == resource)
                .anyMatch(c -> player.getCardResourcesCount().get(c.getClass()) > 0);
    }

    @SuppressWarnings("unchecked")
    public ActionInputParamsResponse getActionInputParams(MarsGame game, Player player, Card blueCard) {
        ActionValidator<Card> validator = (ActionValidator<Card>) blueActionValidators.get(blueCard.getClass());

        if (validator == null || AiConstants.ACTIONS_WITHOUT_INPUT_PARAMS.contains(blueCard.getClass())) {
            return ActionInputParamsResponse.makeAction();
        }

        CardMetadata metadata = blueCard.getCardMetadata();
        if (metadata == null) {
            throw new IllegalStateException("Card with input has no metadata");
        }

        CardAction action = metadata.getCardAction();
        List<ActionInputData> actionsInputData = metadata.getActionsInputData();

        if (!actionsInputData.isEmpty()) {
            ActionInputData inputData = actionsInputData.get(0);
            ActionInputParamsResponse response = handleActionWithInputData(game, player, blueCard, action, inputData);
            if (response != null) {
                return response;
            }
        }

        return handleActionWithoutInputData(game, player, blueCard, action);
    }

    private ActionInputParamsResponse handleActionWithInputData(
            MarsGame game, Player player, Card blueCard, CardAction action, ActionInputData inputData) {

        switch (action) {
            case FIBROUS_COMPOSITE_MATERIAL:
                return handleFibrousCompositeMaterial(game, player, blueCard, inputData);
            case DECOMPOSING_FUNGUS:
                return handleDecomposingFungus(player);
            case CONSERVED_BIOME:
                return handleConservedBiome(player);
            case POWER_INFRASTRUCTURE:
                return handlePowerInfrastructure(game, player);
            case GREEN_HOUSES:
                return handleGreenHouses(player);
            case SYMBIOTIC_FUNGUD:
                return handleSymbioticFungud(player);
            case SELF_REPLICATING_BACTERIA:
                return handleSelfReplicatingBacteria(game, player, blueCard, action, inputData);
        }

        if (inputData.getType() == ActionInputDataType.DISCARD_CARD) {
            return handleDiscardCard(player, inputData);
        }

        if (inputData.getType() == ActionInputDataType.ADD_DISCARD_MICROBE) {
            return handleAddDiscardMicrobe(game, player, blueCard, action, inputData);
        }

        return null;
    }

    private ActionInputParamsResponse handleActionWithoutInputData(
            MarsGame game, Player player, Card blueCard, CardAction action) {

        return switch (action) {
            case EXTREME_COLD_FUNGUS -> handleExtremeColdFungus(player);
            case EXPERIMENTAL_TECHNOLOGY, VIRTUAL_EMPLOYEE_DEVELOPMENT -> handlePhaseUpgrade(game, player);
            case RESEARCH_GRANT -> handleResearchGrant(player, blueCard);
            default -> throw new IllegalStateException("Invalid blue action");
        };
    }

    private ActionInputParamsResponse handleFibrousCompositeMaterial(
            MarsGame game, Player player, Card blueCard, ActionInputData inputData) {

        Map<Integer, List<Integer>> result = new HashMap<>();

        boolean canUpgradePhase = player.getCardResourcesCount().get(blueCard.getClass()) >= inputData.getMax();
        boolean shouldUpgradePhase = canUpgradePhase && random.nextBoolean();

        List<Integer> microbeInput = shouldUpgradePhase ? List.of(inputData.getMax()) : List.of(1);
        result.put(InputFlag.ADD_DISCARD_MICROBE.getId(), microbeInput);

        if (shouldUpgradePhase) {
            int phaseChoice = aiDiscoveryDecisionService.choosePhaseUpgrade(game, player);
            result.put(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(phaseChoice));
        }

        return ActionInputParamsResponse.makeActionWithParams(result);
    }

    private ActionInputParamsResponse handleDecomposingFungus(Player player) {
        List<Card> cardsWithMicrobes = getCardsWithResourceAndCount(player, CardCollectableResource.MICROBE);
        Card randomCard = cardsWithMicrobes.get(random.nextInt(cardsWithMicrobes.size()));

        return ActionInputParamsResponse.makeActionWithParams(
                Map.of(InputFlag.CARD_CHOICE.getId(), List.of(randomCard.getId()))
        );
    }

    private ActionInputParamsResponse handleConservedBiome(Player player) {
        List<Card> eligibleCards = player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .filter(c -> c.getCollectableResource() == CardCollectableResource.MICROBE
                        || c.getCollectableResource() == CardCollectableResource.ANIMAL)
                .toList();

        Card randomCard = eligibleCards.get(random.nextInt(eligibleCards.size()));

        return ActionInputParamsResponse.makeActionWithParams(
                Map.of(InputFlag.CARD_CHOICE.getId(), List.of(randomCard.getId()))
        );
    }

    private ActionInputParamsResponse handleDiscardCard(Player player, ActionInputData inputData) {
        int maxCards = Math.min(random.nextInt(inputData.getMax()) + 1, player.getHand().size());

        List<Integer> availableCards = new ArrayList<>(player.getHand().getCards());
        List<Integer> cardsToDiscard = new ArrayList<>();

        for (int i = 0; i < maxCards; i++) {
            Integer card = availableCards.get(random.nextInt(availableCards.size()));
            cardsToDiscard.add(card);
            availableCards.remove(card);
        }

        return ActionInputParamsResponse.makeActionWithParams(
                Map.of(InputFlag.CARD_CHOICE.getId(), cardsToDiscard)
        );
    }

    private ActionInputParamsResponse handleAddDiscardMicrobe(
            MarsGame game, Player player, Card blueCard, CardAction action, ActionInputData inputData) {

        boolean mustCollectOnly = (action == CardAction.GHG_PRODUCTION && !terraformingService.canIncreaseTemperature(game))
                || (action == CardAction.NITRITE_REDUCTING && !terraformingService.canRevealOcean(game))
                || (action == CardAction.REGOLITH_EATERS && !terraformingService.canIncreaseOxygen(game));

        if (mustCollectOnly) {
            return ActionInputParamsResponse.makeActionWithParams(
                    Map.of(InputFlag.ADD_DISCARD_MICROBE.getId(), List.of(1))
            );
        }

        boolean canConvert = player.getCardResourcesCount().get(blueCard.getClass()) >= inputData.getMax();
        boolean shouldConvert = canConvert && random.nextBoolean();
        int microbeCount = shouldConvert ? inputData.getMax() : 1;

        return ActionInputParamsResponse.makeActionWithParams(
                Map.of(InputFlag.ADD_DISCARD_MICROBE.getId(), List.of(microbeCount))
        );
    }

    private ActionInputParamsResponse handleSelfReplicatingBacteria(MarsGame game, Player player, Card blueCard, CardAction action, ActionInputData inputData) {
        boolean canBuild = random.nextBoolean() && player.getCardResourcesCount().get(blueCard.getClass()) >= inputData.getMax();
        Optional<BuildContext> toBuild = canBuild ? potentialBuildForSelfReplicating(game, player.getUuid()) : Optional.empty();

        if (toBuild.isEmpty()) {
            return ActionInputParamsResponse.makeActionWithParams(Map.of(InputFlag.ADD_DISCARD_MICROBE.getId(), List.of(1)));
        }
        BuildContext buildContext = toBuild.get();
        player.setBuildContext(buildContext);

        return ActionInputParamsResponse.makeActionWithParams(Map.of(InputFlag.ADD_DISCARD_MICROBE.getId(), List.of(inputData.getMax())));
    }

    private Optional<BuildContext> potentialBuildForSelfReplicating(MarsGame originalGame, String playerUuid) {
        MarsGame game = new MarsGame(originalGame);
        Player player = game.getPlayerByUuid(playerUuid);
        player.getBuilds().add(new BuildDto(BuildType.GREEN_OR_BLUE, 25));

        return player.getHand().getCards().stream().map(cardService::getCard).sorted(Comparator.comparingInt(Card::getPrice).reversed()).map(card -> {
            Map<Integer, List<Integer>> inputParams = aiRandomCardBuildParamsService.getInputParamsForBuild(game, player, card);
            if (inputParams == null) {
                return null;
            }
            List<Payment> payments = aiRandomPaymentService.getCardPayments(game, player, card, inputParams);

            String errorMessage = cardValidationService.validateCard(player, game, card.getId(), payments, inputParams);
            if (errorMessage == null) {
                return new BuildContext(payments, inputParams, card.getId());
            } else {
                return null;
            }
        }).filter(Objects::nonNull).findFirst();
    }

    private ActionInputParamsResponse handlePowerInfrastructure(MarsGame game, Player player) {
        int heat = player.getHeat();
        int convert;

        if (game.getPlanet().isTemperatureMax()) {
            int keep = Math.min(6 + random.nextInt(5), heat);
            convert = heat - keep;
        } else {
            convert = (heat > 1 && random.nextInt(10) <= 2)
                    ? 1 + random.nextInt(Math.min(5, heat - 1))
                    : 0;
        }

        convert = Math.max(convert, 1);

        return ActionInputParamsResponse.makeActionWithParams(
                Map.of(InputFlag.DISCARD_HEAT.getId(), List.of(convert))
        );
    }

    private ActionInputParamsResponse handleGreenHouses(Player player) {
        int heatToDiscard = Math.min(random.nextInt(4) + 1, player.getHeat());

        return ActionInputParamsResponse.makeActionWithParams(
                Map.of(InputFlag.DISCARD_HEAT.getId(), List.of(heatToDiscard))
        );
    }

    private ActionInputParamsResponse handleSymbioticFungud(Player player) {
        List<Card> eligibleCards = getCardsWithResource(player, CardCollectableResource.MICROBE);
        Card randomCard = eligibleCards.get(random.nextInt(eligibleCards.size()));

        return ActionInputParamsResponse.makeActionWithParams(
                Map.of(InputFlag.CARD_CHOICE.getId(), List.of(randomCard.getId()))
        );
    }

    private ActionInputParamsResponse handleExtremeColdFungus(Player player) {
        List<Card> eligibleCards = getCardsWithResource(player, CardCollectableResource.MICROBE);
        boolean shouldTakeMicrobe = !eligibleCards.isEmpty() && random.nextBoolean();

        if (shouldTakeMicrobe) {
            Card randomCard = eligibleCards.get(random.nextInt(eligibleCards.size()));

            return ActionInputParamsResponse.makeActionWithParams(
                    Map.of(InputFlag.EXTREME_COLD_FUNGUS_PUT_MICROBE.getId(), List.of(randomCard.getId()))
            );
        } else {
            return ActionInputParamsResponse.makeActionWithParams(
                    Map.of(InputFlag.EXTEME_COLD_FUNGUS_PICK_PLANT.getId(), List.of(0))
            );
        }
    }

    private ActionInputParamsResponse handlePhaseUpgrade(MarsGame game, Player player) {
        int phaseChoice = aiDiscoveryDecisionService.choosePhaseUpgrade(game, player);

        return ActionInputParamsResponse.makeActionWithParams(
                Map.of(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(phaseChoice))
        );
    }

    private ActionInputParamsResponse handleResearchGrant(Player player, Card blueCard) {
        List<Tag> cardTags = player.getCardToTag().get(ResearchGrant.class);
        int tagToPut = aiDiscoveryDecisionService.chooseDynamicTagValue(player, cardTags);

        List<Card> playedCards = player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .collect(Collectors.toList());

        Map<Integer, List<Integer>> activeInputFromCards = aiRandomCardBuildParamsService.getActiveInputFromCards(
                player, playedCards, blueCard,
                Map.of(InputFlag.TAG_INPUT.getId(), List.of(tagToPut))
        );

        Map<Integer, List<Integer>> result = new HashMap<>();
        result.put(InputFlag.TAG_INPUT.getId(), List.of(tagToPut));
        result.putAll(activeInputFromCards);

        return ActionInputParamsResponse.makeActionWithParams(result);
    }

    private List<Card> getCardsWithResource(Player player, CardCollectableResource resource) {
        return player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .filter(c -> c.getCollectableResource() == resource)
                .toList();
    }

    private List<Card> getCardsWithResourceAndCount(Player player, CardCollectableResource resource) {
        return player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .filter(c -> c.getCollectableResource() == resource)
                .filter(c -> player.getCardResourcesCount().get(c.getClass()) > 0)
                .toList();
    }

}
