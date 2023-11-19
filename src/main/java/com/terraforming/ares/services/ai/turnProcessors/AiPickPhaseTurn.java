package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.ai.AiTurnChoice;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.CardValidationService;
import com.terraforming.ares.services.DraftCardsService;
import com.terraforming.ares.services.SpecialEffectsService;
import com.terraforming.ares.services.ai.*;
import com.terraforming.ares.services.ai.dto.BuildProjectPrediction;
import com.terraforming.ares.services.ai.dto.PhaseChoiceProjection;
import com.terraforming.ares.services.ai.helpers.AiCardActionHelper;
import com.terraforming.ares.services.ai.helpers.AiCardBuildParamsHelper;
import com.terraforming.ares.services.ai.helpers.AiPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by oleksii.nikitin
 * Creation date 23.11.2022
 */
@Component
@RequiredArgsConstructor
public class AiPickPhaseTurn implements AiTurnProcessor {
    private final Random random = new Random();
    private final AiTurnService aiTurnService;
    private final CardService cardService;
    private final AiCardActionHelper aiCardActionHelper;
    private final CardValidationService cardValidationService;
    private final AiPaymentService aiPaymentHelper;
    private final AiCardBuildParamsHelper aiCardParamsHelper;
    private final SpecialEffectsService specialEffectsService;
    private final DraftCardsService draftCardsService;
    private final ICardValueService cardValueService;
    private final TestAiService testAiService;
    private final DeepNetwork deepNetwork;
    private final AiThirdPhaseProjectionService aiThirdPhaseProjectionService;


    @Override
    public TurnType getType() {
        return TurnType.PICK_PHASE;
    }

    @Override
    public boolean processTurn(MarsGame game, Player player) {
        Integer previousChosenPhase = player.getPreviousChosenPhase();

        List<Integer> possiblePhases = new ArrayList<>();

        if (player.isFirstBot() && Constants.PICK_PHASE_PLAYER_1 == AiTurnChoice.NETWORK || player.isSecondBot() && Constants.PICK_PHASE_PLAYER_2 == AiTurnChoice.NETWORK) {
            PhaseChoiceProjection projection = PhaseChoiceProjection.SKIP_PHASE;
            if (previousChosenPhase == null || previousChosenPhase != 1 && previousChosenPhase != 2) {
                projection = projection.applyIfBetter(chooseBetweenFirstAndSecondPhaseAi(game, player));
            } else {
                projection = projection.applyIfBetter(mayPlayPhaseOneAi(game, player));
                projection = projection.applyIfBetter(mayPlayPhaseTwoAi(game, player));
            }

            projection = projection.applyIfBetter(mayPlayPhaseThree(game, player));

            projection = projection.applyIfBetter(mayPlayPhaseFourAi(game, player));

            if (player.isFirstBot()) {
                projection = projection.applyIfBetter(mayPlayPhaseFiveAi(game, player));
            } else {
                if (mayPlayPhaseFive(game, player)) {
                    possiblePhases.add(5);
                }
            }

            if (projection.isPickPhase()) {
                possiblePhases.add(projection.getPhase());
            }
        } else {
            if (!(player.isFirstBot() && Constants.PICK_PHASE_PLAYER_1 == AiTurnChoice.RANDOM || player.isSecondBot() && Constants.PICK_PHASE_PLAYER_2 == AiTurnChoice.RANDOM)
                    && (previousChosenPhase == null || previousChosenPhase != 1 && previousChosenPhase != 2)) {
                int phase = chooseBetweenFirstAndSecondPhase(game, player);
                if (phase != 0) {
                    possiblePhases.add(phase);
                }
            } else {
                if (mayPlayPhaseOne(game, player)) {
                    possiblePhases.add(1);
                }

                if (mayPlayPhaseTwo(game, player)) {
                    possiblePhases.add(2);
                }
            }

            if (mayPlayPhaseThree(game, player).isPickPhase()) {
                possiblePhases.add(3);
            }

            if (mayPlayPhaseFour(game, player)) {
                possiblePhases.add(4);
            }

            if (mayPlayPhaseFive(game, player)) {
                possiblePhases.add(5);
            }
        }


        int chosenPhase;

        if (possiblePhases.isEmpty()) {
            if (RandomBotHelper.isRandomBot(player)) {
                chosenPhase = random.nextInt(previousChosenPhase != null ? 4 : 5) + 1;
                if (previousChosenPhase != null && chosenPhase == previousChosenPhase) {
                    chosenPhase++;
                }
            } else {
                if (previousChosenPhase == null) {
                    chosenPhase = 5;
                } else {
                    if (previousChosenPhase != 4 && previousChosenPhase != 5) {
                        int ratio = random.nextInt(100);
                        if (ratio < 40) {
                            chosenPhase = 4;
                        } else {
                            chosenPhase = 5;
                        }
                    } else {
                        chosenPhase = (previousChosenPhase == 4) ? 5 : 4;
                    }
                }
            }
        } else {
            chosenPhase = possiblePhases.get(random.nextInt(possiblePhases.size()));
        }

        if (player.isFirstBot()) {
            Constants.FIRST_PLAYER_PHASES.compute(chosenPhase, (key, value) -> {
                if (value == null) {
                    value = 0;
                }
                return value + 1;
            });
        }
        if (player.isSecondBot()) {
            Constants.SECOND_PLAYER_PHASES.compute(chosenPhase, (key, value) -> {
                if (value == null) {
                    value = 0;
                }
                return value + 1;
            });
        }

        aiTurnService.choosePhaseTurn(player, chosenPhase);

        return true;
    }

    private PhaseChoiceProjection chooseBetweenFirstAndSecondPhaseAi(MarsGame game, Player player) {
        BuildProjectPrediction prediction = testAiService.getBestCardToBuild(game, player, Set.of(CardColor.GREEN, CardColor.RED, CardColor.BLUE));
        if (!prediction.isCanBuild()) {
            return PhaseChoiceProjection.SKIP_PHASE;
        }
        return PhaseChoiceProjection.builder().pickPhase(true).phase(prediction.getCard() == null ? 2 : prediction.getCard().getColor() == CardColor.GREEN ? 1 : 2).chance(prediction.getExpectedValue()).build();
    }

    private int chooseBetweenFirstAndSecondPhase(MarsGame game, Player player) {
        Player copy = new Player(player);

        List<Card> playableCards = player.getHand()
                .getCards()
                .stream()
                .map(cardService::getCard)
                .filter(card ->
                {
                    if (card.getColor() == CardColor.GREEN) {
                        copy.setBuilds(List.of(new BuildDto(BuildType.GREEN, 3)));
                    } else {
                        copy.setBuilds(List.of(new BuildDto(BuildType.BLUE_RED)));
                    }
                    String errorMessage = cardValidationService.validateCard(
                            copy, game, card.getId(),
                            aiPaymentHelper.getCardPayments(game, copy, card),
                            aiCardParamsHelper.getInputParamsForValidation(game, copy, card)
                    );
                    return errorMessage == null;
                }).collect(Collectors.toList());

        if (playableCards.isEmpty()) {
            return 0;
        }

        Card bestCard = cardValueService.getBestCardToBuild(game, player, playableCards, game.getTurns(), true);
        if (bestCard == null) {
            return 0;
        }

        return (bestCard.getColor() == CardColor.GREEN ? 1 : 2);
    }

    private PhaseChoiceProjection mayPlayPhaseOneAi(MarsGame game, Player player) {
        if (player.getPreviousChosenPhase() != null && player.getPreviousChosenPhase() == 1) {
            return PhaseChoiceProjection.SKIP_PHASE;
        }

        BuildProjectPrediction prediction = testAiService.getBestCardToBuild(game, player, Set.of(CardColor.GREEN));

        return PhaseChoiceProjection.builder().pickPhase(prediction.isCanBuild()).phase(1).chance(prediction.getExpectedValue()).build();
    }

    private PhaseChoiceProjection mayPlayPhaseTwoAi(MarsGame game, Player player) {
        if (player.getPreviousChosenPhase() != null && player.getPreviousChosenPhase() == 2) {
            return PhaseChoiceProjection.SKIP_PHASE;
        }

        BuildProjectPrediction prediction = testAiService.getBestCardToBuild(game, player, Set.of(CardColor.RED, CardColor.BLUE));

        return PhaseChoiceProjection.builder().pickPhase(prediction.isCanBuild()).phase(2).chance(prediction.getExpectedValue()).build();
    }

    private boolean mayPlayPhaseOne(MarsGame game, Player player) {
        if (player.getPreviousChosenPhase() != null && player.getPreviousChosenPhase() == 1) {
            return false;
        }

        List<Card> playableCards = player.getHand()
                .getCards()
                .stream()
                .map(cardService::getCard)
                .filter(card -> card.getColor() == CardColor.GREEN)
                .filter(card ->
                {
                    Player copy = new Player(player);
                    copy.setBuilds(List.of(new BuildDto(BuildType.GREEN, 3)));
                    String errorMessage = cardValidationService.validateCard(
                            copy, game, card.getId(),
                            aiPaymentHelper.getCardPayments(game, copy, card),
                            aiCardParamsHelper.getInputParamsForValidation(game, copy, card)
                    );
                    return errorMessage == null;
                })
                .collect(Collectors.toList());

        if (player.isFirstBot() && Constants.PICK_PHASE_PLAYER_1 == AiTurnChoice.RANDOM || player.isSecondBot() && Constants.PICK_PHASE_PLAYER_2 == AiTurnChoice.RANDOM) {
            return !playableCards.isEmpty();
        }

        return cardValueService.getBestCardToBuild(game, player, playableCards, game.getTurns(), true) != null;
    }

    private boolean mayPlayPhaseTwo(MarsGame game, Player player) {
        if (player.getPreviousChosenPhase() != null && player.getPreviousChosenPhase() == 2) {
            return false;
        }

        List<Card> playableCards = player.getHand()
                .getCards()
                .stream()
                .map(cardService::getCard)
                .filter(card -> card.getColor() == CardColor.BLUE || card.getColor() == CardColor.RED)
                .filter(card ->
                {
                    Player copy = new Player(player);
                    copy.setBuilds(List.of(new BuildDto(BuildType.BLUE_RED)));
                    String errorMessage = cardValidationService.validateCard(
                            copy, game, card.getId(),
                            aiPaymentHelper.getCardPayments(game, copy, card),
                            aiCardParamsHelper.getInputParamsForValidation(game, copy, card)
                    );
                    return errorMessage == null;
                })
                .collect(Collectors.toList());

        if (player.isFirstBot() && Constants.PICK_PHASE_PLAYER_1 == AiTurnChoice.RANDOM || player.isSecondBot() && Constants.PICK_PHASE_PLAYER_2 == AiTurnChoice.RANDOM) {
            return !playableCards.isEmpty();
        }

        return cardValueService.getBestCardToBuild(game, player, playableCards, game.getTurns(), true) != null;
    }

    private PhaseChoiceProjection mayPlayPhaseFourAi(MarsGame game, Player player) {
        if (player.getPreviousChosenPhase() != null && player.getPreviousChosenPhase() == 4) {
            return PhaseChoiceProjection.SKIP_PHASE;
        }

        float stateBeforeIncome = deepNetwork.testState(game, player);
        float stateAfterIncome = testAiService.projectPlayPhase4(game, player);

        if (Constants.LOG_NET_COMPARISON) {
            System.out.println("Deep network state income before " + stateBeforeIncome + ". After " + stateAfterIncome);
        }

        //*1 Ratio: 0.4560819462227913
        //*1.05 Ratio: 0.4270063694267516
        //TODO remove 1.05 when phase 5 is picked smart
        if (stateAfterIncome > stateBeforeIncome * 1.05) {
            return PhaseChoiceProjection.builder().pickPhase(true).phase(4).chance(stateAfterIncome).build();
        } else {
            return PhaseChoiceProjection.SKIP_PHASE;
        }
    }

    private PhaseChoiceProjection mayPlayPhaseFiveAi(MarsGame game, Player player) {
        if (player.getPreviousChosenPhase() != null && player.getPreviousChosenPhase() == 5) {
            return PhaseChoiceProjection.SKIP_PHASE;
        }

        float stateBeforePhase = deepNetwork.testState(game, player);
        float stateAfterPhase = testAiService.projectPlayPhase5(game, player);

        if (Constants.LOG_NET_COMPARISON) {
            System.out.println("Deep network state phase 5 before " + stateBeforePhase + ". After " + stateAfterPhase);
        }

        if (stateAfterPhase > stateBeforePhase) {
            return PhaseChoiceProjection.builder().pickPhase(true).phase(5).chance(stateAfterPhase).build();
        } else {
            return PhaseChoiceProjection.SKIP_PHASE;
        }
    }

    private boolean mayPlayPhaseFour(MarsGame game, Player player) {
        if (player.getPreviousChosenPhase() != null && player.getPreviousChosenPhase() == 4) {
            return false;
        }

        if (RandomBotHelper.isRandomBot(player)) {
            if (player.getMc() <= 5) {//when running really low unable to do anything
                return true;
            }

            return random.nextInt(5) == 0;
        }

        if (player.getMc() <= 5 && player.getCardIncome() >= 3) {//when running really low unable to do anything
            return true;
        }

        //when have a good income
        if (player.getMcIncome() > 20 || (player.getHeatIncome() > 15 && !game.getPlanet().isTemperatureMax()) || player.getPlantsIncome() > 5) {
            return true;
        }

        //when need and can fill restructured resources
        if (specialEffectsService.ownsSpecialEffect(player, SpecialEffect.RESTRUCTURED_RESOURCES)
                && player.getPlantsIncome() > 0
                && player.getPlants() == 0) {
            return true;
        }

        List<Player> otherPlayers = getOtherPlayers(game, player);

        if (calcTotalIncome(game, player) / 2 > getPlayerMaxIncome(game, otherPlayers)) {
            return true;
        }

        return false;
    }

    private int getPlayerMaxIncome(MarsGame game, List<Player> players) {
        return players.stream().mapToInt(p -> calcTotalIncome(game, p)).max().orElse(Integer.MAX_VALUE);
    }

    private List<Player> getOtherPlayers(MarsGame game, Player player) {
        return game.getPlayerUuidToPlayer()
                .values()
                .stream()
                .filter(p -> !p.getUuid().equals(player.getUuid()))
                .collect(Collectors.toList());
    }

    private int calcTotalIncome(MarsGame game, Player player) {
        return player.getMcIncome() + player.getTerraformingRating() +
                player.getSteelIncome() * 2 +
                player.getTitaniumIncome() * 3 +
                player.getPlantsIncome() * (game.getPlanet().isOxygenMax() ? 1 : 2) +
                player.getCardIncome() * 4 +
                player.getHeatIncome() * (game.getPlanet().isTemperatureMax() ? 1 : 2);
    }

    private boolean mayPlayPhaseThreeSmart(MarsGame game, Player player) {
        List<Card> activeCards = player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .filter(Card::isActiveCard).collect(Collectors.toList());

        Set<CardAction> cardActions = activeCards.stream()
                .map(Card::getCardMetadata)
                .filter(Objects::nonNull)
                .map(CardMetadata::getCardAction)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (cardActions.contains(CardAction.AI_CENTRAL)) {
            return true;
        }

        int requiredActiveCardsCount = 5;
        if (cardActions.contains(CardAction.SCREENING_TECHNOLOGY)) {
            requiredActiveCardsCount--;
        }
        if (cardActions.contains(CardAction.CIRCUIT_BOARD)) {
            requiredActiveCardsCount--;
        }

        if ((cardActions.contains(CardAction.EXTREME_COLD_FUNGUS) || cardActions.contains(CardAction.SYMBIOTIC_FUNGUD))
                && (cardActions.contains(CardAction.GHG_PRODUCTION)
                || cardActions.contains(CardAction.NITRITE_REDUCTING)
                || cardActions.contains(CardAction.REGOLITH_EATERS)
                || cardActions.contains(CardAction.SELF_REPLICATING_BACTERIA))) {
            requiredActiveCardsCount--;
        }

        if (cardActions.contains(CardAction.ASSET_LIQUIDATION)) {
            requiredActiveCardsCount++;
        }

        if (cardActions.contains(CardAction.PROGRESSIVE_POLICIES)
                && cardService.countPlayedTags(player, Set.of(Tag.EVENT)) >= 4
                && player.getMc() >= 15) {
            requiredActiveCardsCount--;
        }

        if (cardActions.contains(CardAction.DEVELOPED_INFRASTRUCTURE)
                && cardService.countPlayedCards(player, Set.of(CardColor.BLUE)) >= 5
                && player.getMc() >= 15) {
            requiredActiveCardsCount--;
        }

        if ((cardActions.contains(CardAction.STEELWORKS) && player.getHeat() >= 8 || cardActions.contains(CardAction.IRON_WORKS) && player.getHeat() >= 12)
                && !game.getPlanet().isOxygenMax()) {
            requiredActiveCardsCount--;
        }

        if (cardActions.contains(CardAction.CARETAKER_CONTRACT) && player.getHeat() >= 16) {
            requiredActiveCardsCount--;
        }

        if (cardActions.contains(CardAction.SOLAR_PUNK)) {
            if (player.getTitaniumIncome() < 3) {
                requiredActiveCardsCount++;
            } else {
                int solarpunkCost = Math.max(0, 15 - player.getTitaniumIncome() * 2) * 2;
                if (player.getMc() - 5 < solarpunkCost) {
                    requiredActiveCardsCount++;
                } else {
                    requiredActiveCardsCount--;
                }
            }
        }

        if (player.getHeat() >= 40 && !game.getPlanet().isTemperatureMax() || player.getPlants() >= 32 && !game.getPlanet().isOxygenMax()) {
            return true;
        }

        if (player.getMc() > 200) {
            return true;
        }

        requiredActiveCardsCount = Math.max(0, requiredActiveCardsCount);

        return activeCards
                .stream()
                .filter(card -> aiCardActionHelper.isSmartPlayAction(game, player, card))
                .limit(requiredActiveCardsCount)
                .count() == requiredActiveCardsCount;
    }

    private boolean mayPlayPhaseThreeRandom(MarsGame game, Player player) {
        return player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .filter(Card::isActiveCard)
                .filter(card -> aiCardActionHelper.isSmartPlayAction(game, player, card))
                .limit(3)
                .count() == 3;
    }

    private PhaseChoiceProjection mayPlayPhaseThreeAi(MarsGame game, Player player) {
        return aiThirdPhaseProjectionService.projectThirdPhase(game, player);
    }

    private PhaseChoiceProjection mayPlayPhaseThree(MarsGame game, Player player) {
        if (player.getPreviousChosenPhase() != null && player.getPreviousChosenPhase() == 3) {
            return PhaseChoiceProjection.SKIP_PHASE;
        }

        switch (player.isFirstBot() ?  Constants.THIRD_PHASE_ACTIONS_PLAYER_1 : Constants.THIRD_PHASE_ACTIONS_PLAYER_2) {
            case RANDOM:
                return PhaseChoiceProjection.builder().pickPhase(mayPlayPhaseThreeRandom(game, player)).phase(3).build();
            case SMART:
                return PhaseChoiceProjection.builder().pickPhase(mayPlayPhaseThreeSmart(game, player)).phase(3).build();
            case NETWORK:
                return mayPlayPhaseThreeAi(game, player);
            default:
                throw new IllegalStateException("AI can't do third phase");
        }
    }

    private boolean mayPlayPhaseFive(MarsGame game, Player player) {
        if (player.getPreviousChosenPhase() != null && player.getPreviousChosenPhase() == 5) {
            return false;
        }

        if (RandomBotHelper.isRandomBot(player)) {
            return random.nextInt(5) == 0;
        }

        if (draftCardsService.countExtraCardsToTake(player) >= 2 || draftCardsService.countExtraCardsToDraft(player) >= 2) {
            return true;
        }

        boolean hasCardsToBuild = player.getHand()
                .getCards()
                .stream()
                .map(cardService::getCard)
                .filter(card -> card.getColor() == CardColor.GREEN)
                .filter(card ->
                {
                    Player copy = new Player(player);
                    copy.setBuilds(List.of(new BuildDto(BuildType.GREEN, 3)));
                    String errorMessage = cardValidationService.validateCard(
                            copy, game, card.getId(),
                            aiPaymentHelper.getCardPayments(game, copy, card),
                            aiCardParamsHelper.getInputParamsForValidation(game, copy, card)
                    );
                    return errorMessage == null;
                })
                .limit(2)
                .count() == 2;

        if (!hasCardsToBuild) {
            hasCardsToBuild = player.getHand()
                    .getCards()
                    .stream()
                    .map(cardService::getCard)
                    .filter(card -> card.getColor() == CardColor.BLUE || card.getColor() == CardColor.RED)
                    .filter(card ->
                    {
                        Player copy = new Player(player);
                        copy.setBuilds(List.of(new BuildDto(BuildType.BLUE_RED, 0)));
                        String errorMessage = cardValidationService.validateCard(
                                copy, game, card.getId(),
                                aiPaymentHelper.getCardPayments(game, copy, card),
                                aiCardParamsHelper.getInputParamsForValidation(game, copy, card)
                        );
                        return errorMessage == null;
                    })
                    .limit(2)
                    .count() == 2;
        }

        if (!hasCardsToBuild) {
            return true;
        }

        return false;
    }


}
