package com.terraforming.ares.services;

import com.terraforming.ares.model.*;
import com.terraforming.ares.model.payments.Payment;
import com.terraforming.ares.model.payments.PaymentType;
import com.terraforming.ares.validation.payment.PaymentValidator;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by oleksii.nikitin
 * Creation date 03.05.2022
 */
@Service
public class PaymentValidationService {
    private final Map<PaymentType, PaymentValidator> validators;
    private final SpecialEffectsService specialEffectsService;

    public PaymentValidationService(SpecialEffectsService specialEffectsService, List<PaymentValidator> paymentValidators) {
        this.specialEffectsService = specialEffectsService;
        validators = paymentValidators.stream().collect(Collectors.toMap(
                PaymentValidator::getType, Function.identity()
        ));
    }

    public String validate(ProjectCard projectCard, Player player, List<Payment> payments) {
        for (Payment payment : payments) {
            PaymentValidator paymentValidator = validators.get(payment.getType());
            if (paymentValidator == null) {
                throw new IllegalStateException("Payment validator not found for type " + payment.getType());
            }
            String validationResult = paymentValidator.validate(player, payment);
            if (StringUtils.hasLength(validationResult)) {
                return validationResult;
            }
        }

        if (player.isCanBuildAnotherGreenWith9Discount() && projectCard.getPrice() > 9) {
            return "Can only build a second building with print price of 9 or less";
        }


        int discount = getDiscount(projectCard, player);
        int discountedPrice = Math.max(0, projectCard.getPrice() - discount);

        int totalPayment = payments.stream().mapToInt(Payment::getTotalValue).sum();

        if (totalPayment < discountedPrice) {
            return "Total payment is not enough to cover the project price";
        } else if (totalPayment > discountedPrice) {
            return "Total payment provided is more than needed";
        } else {
            return null;
        }
    }

    private int getDiscount(ProjectCard projectCard, Player player) {
        int discount = 0;

        List<Tag> tags = projectCard.getTags();

        boolean playerOwnsAdvancedAlloys = specialEffectsService.ownsSpecialEffect(player, SpecialEffect.ADVANCED_ALLOYS);

        if (tags.contains(Tag.BUILDING) && player.getSteelIncome() != 0) {
            discount += player.getSteelIncome() * (2 + (playerOwnsAdvancedAlloys ? 1 : 0));
        }

        if (tags.contains(Tag.SPACE) && player.getTitaniumIncome() != 0) {
            discount += player.getTitaniumIncome() * (3 + (playerOwnsAdvancedAlloys ? 1 : 0));
        }

        if (projectCard.getColor() == CardColor.GREEN && player.getChosenPhase() == 1) {
            discount += 3;
        }

        if (specialEffectsService.ownsSpecialEffect(player, SpecialEffect.EARTH_CATAPULT_DISCOUNT_2)) {
            discount += 2;
        }

        if (projectCard.getTags().contains(Tag.ENERGY) &&
                specialEffectsService.ownsSpecialEffect(player, SpecialEffect.ENERGY_SUBSIDIES_DISCOUNT_4)) {
            discount += 4;
        }

        if (specialEffectsService.ownsSpecialEffect(player, SpecialEffect.INTERPLANETARY_CONFERENCE)) {
            if (projectCard.getTags().contains(Tag.EARTH)) {
                discount += 3;
            }
            if (projectCard.getTags().contains(Tag.JUPITER)) {
                discount += 3;
            }
        }

        if (projectCard.getTags().contains(Tag.EVENT) &&
                specialEffectsService.ownsSpecialEffect(player, SpecialEffect.MEDIA_GROUP)) {
            discount += 5;
        }

        if (specialEffectsService.ownsSpecialEffect(player, SpecialEffect.RESEARCH_OUTPOST_DISCOUNT_1)) {
            discount += 1;
        }

        if (player.isBuiltWorkCrewsLastTurn()) {
            discount += 11;
        }

        if (projectCard.getTags().contains(Tag.SPACE) &&
                specialEffectsService.ownsSpecialEffect(player, SpecialEffect.SPACE_DISCOUNT_3)) {
            discount += 3;
        }

        if (player.isCanBuildAnotherGreenWith9Discount() && projectCard.getPrice() <= 9) {
            discount += 9;
        }

        //TODO add other types of discount

        return discount;
    }
}
