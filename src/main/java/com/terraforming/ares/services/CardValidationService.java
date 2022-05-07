package com.terraforming.ares.services;

import com.terraforming.ares.model.*;
import com.terraforming.ares.model.parameters.ParameterColor;
import com.terraforming.ares.model.payments.Payment;
import com.terraforming.ares.validation.action.ActionValidator;
import org.springframework.stereotype.Service;

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

    public CardValidationService(CardService cardService,
                                 PaymentValidationService paymentValidationService,
                                 SpecialEffectsService specialEffectsService,
                                 List<ActionValidator<?>> validators) {
        this.cardService = cardService;
        this.paymentValidationService = paymentValidationService;
        this.specialEffectsService = specialEffectsService;

        blueActionValidators = validators.stream().collect(
                Collectors.toMap(
                        ActionValidator::getType,
                        Function.identity()
                )
        );
    }

    public String validateCard(Player player, Planet planet, int cardId, List<Payment> payments, Map<Integer, Integer> inputParameters) {
        ProjectCard projectCard = cardService.getProjectCard(cardId);
        if (projectCard == null) {
            return "Card doesn't exist " + cardId;
        }

        if (!player.getHand().getCards().contains(cardId)) {
            return "Can't build a project that you don't have";
        }


        boolean playerMayAmplifyGlobalRequirement = specialEffectsService.ownsSpecialEffect(player, SpecialEffect.AMPLIFY_GLOBAL_REQUIREMENT);

        //TODO requirements should be set at the beginning of phase
        return validateOxygen(planet, projectCard, playerMayAmplifyGlobalRequirement)
                .or(() -> validateTemperature(planet, projectCard, playerMayAmplifyGlobalRequirement))
                .or(() -> validateOceans(planet, projectCard))
                .or(() -> validateTags(player, projectCard))
                .or(() -> validatePayments(projectCard, player, payments))
                .or(() -> validateInputParameters(projectCard, player, inputParameters))
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    public String validateBlueAction(Player player, Planet planet, int cardId, List<Integer> inputParameters) {
        ProjectCard projectCard = cardService.getProjectCard(cardId);
        if (projectCard == null) {
            return "Card doesn't exist " + cardId;
        }

        if (projectCard.getColor() != CardColor.BLUE || !projectCard.isActiveCard()) {
            return "Selected card doesn't contain an action";
        }

        if (!player.getPlayed().getCards().contains(cardId)) {
            return "Can't play an action of a card that you haven't built";
        }

        if (player.getActivatedBlueCards().containsCard(cardId)) {
            if (player.getChosenPhase() != 3) {
                return "Can't play an action twice if you didn't choose phase 3";
            }
            if (player.isActivatedBlueActionTwice()) {
                return "Can't play an action that was already played and double action already performed";
            }
        }

        ActionValidator<ProjectCard> validator = (ActionValidator<ProjectCard>) blueActionValidators.get(projectCard.getClass());

        if (validator != null) {
            return validator.validate(planet, player, inputParameters);
        } else {
            return null;
        }
    }

    private Optional<String> validateOxygen(Planet planet, ProjectCard card, boolean playerMayAmplifyGlobalRequirement) {
        if (planet.isValidOxygen(
                playerMayAmplifyGlobalRequirement ? amplifyRequirement(card.getOxygenRequirement()) : card.getOxygenRequirement()
        )) {
            return Optional.empty();
        } else {
            return Optional.of("Oxygen requirement not met");
        }
    }

    private Optional<String> validateTemperature(Planet planet, ProjectCard card, boolean playerMayAmplifyGlobalRequirement) {
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
        int maxRequirement = initialRequirement.stream().mapToInt(Enum::ordinal).min().orElse(ParameterColor.WHITE.ordinal());

        if (minRequirement > 0) {
            resultRequirement.add(ParameterColor.values()[minRequirement - 1]);
        }
        if (maxRequirement < ParameterColor.WHITE.ordinal()) {
            resultRequirement.add(ParameterColor.values()[maxRequirement + 1]);
        }

        return resultRequirement;
    }

    private Optional<String> validateOceans(Planet planet, ProjectCard card) {
        if (planet.isValidNumberOfOceans(card.getOceanRequirement())) {
            return Optional.empty();
        } else {
            return Optional.of("Ocean requirement not met");
        }
    }

    private Optional<String> validatePayments(ProjectCard card, Player player, List<Payment> payments) {
        return Optional.ofNullable(paymentValidationService.validate(card, player, payments));
    }

    private Optional<String> validateInputParameters(ProjectCard card, Player player, Map<Integer, Integer> inputParams) {
        return player.getPlayed()
                .getCards()
                .stream()
                .map(cardService::getProjectCard)
                .map(ProjectCard::getProjectInputValidator)
                .filter(Objects::nonNull)
                .map(validator -> validator.validate(card, player, inputParams))
                .filter(Objects::nonNull)
                .findAny();
    }

    private Optional<String> validateTags(Player player, ProjectCard projectCard) {
        List<Integer> cards = player.getPlayed().getCards();

        List<Tag> tagRequirements = new LinkedList<>(projectCard.getTagRequirements());

        if (tagRequirements.isEmpty()) {
            return Optional.empty();
        }

        for (Integer card : cards) {
            ProjectCard builtProject = cardService.getProjectCard(card);

            tagRequirements.removeAll(builtProject.getTags());

            if (tagRequirements.isEmpty()) {
                return Optional.empty();
            }
        }

        return Optional.of("Project tag requirements not met");
    }

}
