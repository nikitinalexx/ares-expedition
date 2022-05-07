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

        int discount = getDiscount(projectCard, player);
        int discountedPrice = projectCard.getPrice() - discount;

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


        //TODO add other types of discount

        return discount;
    }
}
