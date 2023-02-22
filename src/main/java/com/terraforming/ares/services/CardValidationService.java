package com.terraforming.ares.services;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.parameters.ParameterColor;
import com.terraforming.ares.model.payments.Payment;
import com.terraforming.ares.validation.action.ActionValidator;
import com.terraforming.ares.validation.input.OnBuiltEffectValidator;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
@Service
public class CardValidationService {
    private final CardService cardService;
    private final PaymentValidationService paymentValidationService;
    private final SpecialEffectsService specialEffectsService;
    private final Map<Class<?>, ActionValidator<?>> blueActionValidators;
    private final Map<Class<?>, OnBuiltEffectValidator<?>> onBuiltEffectValidators;

    public CardValidationService(CardService cardService,
                                 PaymentValidationService paymentValidationService,
                                 SpecialEffectsService specialEffectsService,
                                 List<ActionValidator<?>> validators,
                                 List<OnBuiltEffectValidator<?>> onBuiltEffectValidators) {
        this.cardService = cardService;
        this.paymentValidationService = paymentValidationService;
        this.specialEffectsService = specialEffectsService;

        blueActionValidators = validators.stream().collect(
                Collectors.toMap(
                        ActionValidator::getType,
                        Function.identity()
                )
        );

        this.onBuiltEffectValidators = onBuiltEffectValidators.stream().collect(
                Collectors.toMap(
                        OnBuiltEffectValidator::getType,
                        Function.identity()
                )
        );
    }

    public String validateCard(Player player, MarsGame game, int cardId, List<Payment> payments, Map<Integer, List<Integer>> inputParameters) {
        Card card = cardService.getCard(cardId);
        if (card == null) {
            return "Card doesn't exist " + cardId;
        }

        if (!player.getHand().getCards().contains(cardId)) {
            return "Can't build a project that you don't have";
        }


        boolean playerMayAmplifyGlobalRequirement = specialEffectsService.ownsSpecialEffect(player, SpecialEffect.AMPLIFY_GLOBAL_REQUIREMENT);
        boolean builtSpecialDesignLastTurn = player.isBuiltSpecialDesignLastTurn();

        return validateOxygen(game.getPlanetAtTheStartOfThePhase(), card, playerMayAmplifyGlobalRequirement || builtSpecialDesignLastTurn)
                .or(() -> validateTemperature(game.getPlanetAtTheStartOfThePhase(), card, playerMayAmplifyGlobalRequirement || builtSpecialDesignLastTurn))
                .or(() -> validateOceans(game.getPlanetAtTheStartOfThePhase(), card))
                .or(() -> validateTags(player, card))
                .or(() -> validatePayments(card, player, payments, inputParameters))
                .or(() -> validateInputParameters(game, card, player, inputParameters))
                .or(() -> validateCustomCards(card, player))
                .orElse(null);
    }

    public String validateCorporation(Player player, MarsGame game, int cardId, Map<Integer, List<Integer>> inputParameters) {
        Card card = cardService.getCard(cardId);

        return validateInputParameters(game, card, player, inputParameters).orElse(null);
    }

    private Optional<String> validateCustomCards(Card card, Player player) {
        boolean canBuildAnotherGreenWith9Discount = player.isCanBuildAnotherGreenWith9Discount();
        boolean canBuildAnotherGreenWithPrice12 = player.isCanBuildAnotherGreenWithPrice12();
        boolean mayNiDiscount = player.isMayNiDiscount();

        return Optional.ofNullable(
                card.getColor() == CardColor.GREEN
                        && canBuildAnotherGreenWith9Discount
                        && !canBuildAnotherGreenWithPrice12
                        && card.getPrice() >= 10 ? "You may only build a card with a price of 9 or less" : null
        ).or(() -> Optional.ofNullable(
                (mayNiDiscount || player.getCanBuildInFirstPhase() == 1 && canBuildAnotherGreenWithPrice12)
                        && card.getPrice() > 12 ? "You may only build a card with a price of 12 or less" : null
        ));
    }

    @SuppressWarnings("unchecked")
    public String validateBlueAction(Player player, MarsGame game, int cardId, Map<Integer, List<Integer>> inputParameters) {
        Card card = cardService.getCard(cardId);
        if (card == null) {
            return "card doesn't exist " + cardId;
        }

        if (!card.isActiveCard()) {
            return "Selected card doesn't contain an action";
        }

        if (!player.getPlayed().getCards().contains(cardId)) {
            return "Can't play an action of a card that you haven't built";
        }

        if (player.getActivatedBlueCards().containsCard(cardId)) {
            if (player.getChosenPhase() != 3) {
                return "Can't play an action twice if you didn't choose phase 3";
            }
            if (player.getBlueActionExtraActivationsLeft() < 1) {
                return "Can't play an action that was already played and extra actions already performed";
            }
        }

        ActionValidator<Card> validator = (ActionValidator<Card>) blueActionValidators.get(card.getClass());

        if (validator != null) {
            return validator.validate(game, player, inputParameters);
        } else {
            return null;
        }
    }

    private Optional<String> validateOxygen(Planet planet, Card card, boolean playerMayAmplifyGlobalRequirement) {
        if (planet.isValidOxygen(
                playerMayAmplifyGlobalRequirement ? amplifyRequirement(card.getOxygenRequirement()) : card.getOxygenRequirement()
        )) {
            return Optional.empty();
        } else {
            return Optional.of("Oxygen requirement not met");
        }
    }

    private Optional<String> validateTemperature(Planet planet, Card card, boolean playerMayAmplifyGlobalRequirement) {
        if (planet.isValidTemperatute(
                playerMayAmplifyGlobalRequirement ? amplifyRequirement(card.getTemperatureRequirement()) : card.getTemperatureRequirement()
        )) {
            return Optional.empty();
        } else {
            return Optional.of("Temperature requirement not met");
        }
    }

    private List<ParameterColor> amplifyRequirement(List<ParameterColor> initialRequirement) {
        List<ParameterColor> resultRequirement = new ArrayList<>(initialRequirement);
        int minRequirement = initialRequirement.stream().mapToInt(Enum::ordinal).min().orElse(0);
        int maxRequirement = initialRequirement.stream().mapToInt(Enum::ordinal).max().orElse(ParameterColor.W.ordinal());

        if (minRequirement > 0) {
            resultRequirement.add(ParameterColor.values()[minRequirement - 1]);
        }
        if (maxRequirement < ParameterColor.W.ordinal()) {
            resultRequirement.add(ParameterColor.values()[maxRequirement + 1]);
        }

        return resultRequirement;
    }

    private Optional<String> validateOceans(Planet planet, Card card) {
        if (planet.isValidNumberOfOceans(card.getOceanRequirement())) {
            return Optional.empty();
        } else {
            return Optional.of("Ocean requirement not met");
        }
    }

    private Optional<String> validatePayments(Card card, Player player, List<Payment> payments, Map<Integer, List<Integer>> inputParameters) {
        return Optional.ofNullable(paymentValidationService.validate(card, player, payments, inputParameters));
    }

    private Optional<String> validateInputParameters(MarsGame game, Card card, Player player, Map<Integer, List<Integer>> inputParams) {
        Optional<String> validationResult = Optional.empty();

        if (card.onBuiltEffectApplicableToItself()) {
            validationResult = validationResult.or(
                    () -> Optional.ofNullable(onBuiltEffectValidators.get(card.getClass()))
                            .map(validator -> validator.validate(game, card, player, CollectionUtils.isEmpty(inputParams) ? Map.of() : inputParams))
            );
        }

        return validationResult.or(() ->
                player.getPlayed()
                        .getCards()
                        .stream()
                        .map(cardService::getCard)
                        .filter(Card::onBuiltEffectApplicableToOther)
                        .map(c -> onBuiltEffectValidators.get(c.getClass()))
                        .filter(Objects::nonNull)
                        .map(validator -> validator.validate(game, card, player, inputParams))
                        .filter(Objects::nonNull)
                        .findAny()
        );
    }

    private Optional<String> validateTags(Player player, Card inputCard) {
        List<Integer> cards = player.getPlayed().getCards();

        List<Tag> tagRequirements = new LinkedList<>(inputCard.getTagRequirements());

        if (tagRequirements.isEmpty()) {
            return Optional.empty();
        }

        for (Integer card : cards) {
            Card builtProject = cardService.getCard(card);

            for (Tag tag : builtProject.getTags()) {
                tagRequirements.remove(tag);
            }

            if (tagRequirements.isEmpty()) {
                return Optional.empty();
            }
        }

        for (List<Tag> dynamicTags : player.getCardToTag().values()) {
            for (Tag dynamicTag : dynamicTags) {
                tagRequirements.remove(dynamicTag);
            }
        }

        if (tagRequirements.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of("Project tag requirements not met");
    }

}
