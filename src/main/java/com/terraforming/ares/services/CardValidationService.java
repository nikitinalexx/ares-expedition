package com.terraforming.ares.services;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.parameters.ParameterColor;
import com.terraforming.ares.model.payments.Payment;
import com.terraforming.ares.validation.action.ActionValidator;
import com.terraforming.ares.validation.input.OnBuiltEffectValidator;
import com.terraforming.ares.validation.input.crysis.ImmediateEffectValidator;
import com.terraforming.ares.validation.input.crysis.PersistentEffectValidator;
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
    private final Map<Class<?>, ImmediateEffectValidator<?>> crysisImmediateEffectValidators;
    private final Map<Class<?>, PersistentEffectValidator<?>> crysisPersistentEffectValidators;

    public CardValidationService(CardService cardService,
                                 PaymentValidationService paymentValidationService,
                                 SpecialEffectsService specialEffectsService,
                                 List<ActionValidator<?>> validators,
                                 List<OnBuiltEffectValidator<?>> onBuiltEffectValidators,
                                 List<ImmediateEffectValidator<?>> immediateEffectValidators,
                                 List<PersistentEffectValidator<?>> persistentEffectValidators) {
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

        this.crysisImmediateEffectValidators = immediateEffectValidators.stream().collect(
                Collectors.toMap(
                        ImmediateEffectValidator::getType,
                        Function.identity()
                )
        );

        this.crysisPersistentEffectValidators = persistentEffectValidators.stream().collect(
                Collectors.toMap(
                        PersistentEffectValidator::getType,
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
                .or(() -> validateInfrastructure(game.getPlanetAtTheStartOfThePhase(), card, playerMayAmplifyGlobalRequirement || builtSpecialDesignLastTurn))
                .or(() -> validateOceans(game.getPlanetAtTheStartOfThePhase(), card))
                .or(() -> validateTags(player, card))
                .or(() -> validatePayments(game, card, player, payments, inputParameters))
                .or(() -> validateInputParameters(game, card, player, inputParameters))
                .orElse(null);
    }

    public String validateCorporation(Player player, MarsGame game, int cardId, Map<Integer, List<Integer>> inputParameters) {
        Card card = cardService.getCard(cardId);

        return validateInputParameters(game, card, player, inputParameters).orElse(null);
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
            if (player.getBlueActionExtraActivationsLeft() < 1) {
                return "Can't play an action that was already played and extra actions already performed";
            }
            if (player.getActivatedBlueCardsTwice().containsCard(cardId)) {
                return "Action already performed twice";
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

    private Optional<String> validateInfrastructure(Planet planet, Card card, boolean playerMayAmplifyGlobalRequirement) {
        if (planet.isValidInfrastructure(
                playerMayAmplifyGlobalRequirement ? amplifyRequirement(card.getInfrastructureRequirement()) : card.getInfrastructureRequirement()
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

    private Optional<String> validatePayments(MarsGame game, Card card, Player player, List<Payment> payments, Map<Integer, List<Integer>> inputParameters) {
        return Optional.ofNullable(paymentValidationService.validate(game, card, player, payments, inputParameters));
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

    public String validateCrisisImmediateEffect(MarsGame game, Player player, Integer cardId, Map<Integer, List<Integer>> inputParams) {
        final CrysisCard crysisCard = cardService.getCrysisCard(cardId);

        return crysisImmediateEffectValidators.get(crysisCard.getClass()).validate(game, crysisCard, player, inputParams);
    }

    public String validateCrisisPersistentEffect(MarsGame game, Player player, Integer cardId, Map<Integer, List<Integer>> inputParams) {
        final CrysisCard crysisCard = cardService.getCrysisCard(cardId);

        return crysisPersistentEffectValidators.get(crysisCard.getClass()).validate(game, crysisCard, player, inputParams);
    }
}
