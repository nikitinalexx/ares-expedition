package com.terraforming.ares.services;

import com.terraforming.ares.mars.MarsGame;
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
    private final CardService cardService;
    private final BuildService buildService;
    private final DiscountService discountService;

    public PaymentValidationService(SpecialEffectsService specialEffectsService,
                                    List<PaymentValidator> paymentValidators,
                                    CardService cardService,
                                    BuildService buildService,
                                    DiscountService discountService) {
        this.specialEffectsService = specialEffectsService;
        this.cardService = cardService;
        this.buildService = buildService;
        this.discountService = discountService;
        validators = paymentValidators.stream().collect(Collectors.toMap(
                PaymentValidator::getType, Function.identity()
        ));
    }

    public int forestPriceInPlants(Player player) {
        int plantsCost = Constants.FOREST_PLANT_COST;

        if (specialEffectsService.ownsSpecialEffect(player, SpecialEffect.ECOLINE_DISCOUNT)) {
            plantsCost--;
        }

        return plantsCost;
    }

    public String validate(MarsGame game, Card card, Player player, List<Payment> payments, Map<Integer, List<Integer>> inputParameters) {
        for (Payment payment : payments) {
            PaymentValidator paymentValidator = validators.get(payment.getType());
            if (paymentValidator == null) {
                throw new IllegalStateException("Payment validator not found for type " + payment.getType());
            }
            String validationResult = paymentValidator.validate(card, player, payment);
            if (StringUtils.hasLength(validationResult)) {
                return validationResult;
            }
        }

        if (card.getColor() == CardColor.GREEN
                && player.getBuilds().stream()
                .noneMatch(build -> build.getType().isGreen()
                        && (build.getPriceLimit() == 0 || build.getPriceLimit() >= card.getPrice()))) {
            int maximumPrice = player.getBuilds().stream().filter(build -> build.getType().isGreen())
                    .findFirst().orElseThrow().getPriceLimit();
            return String.format("Can only build a building with print price of %s or less", maximumPrice);
        }

        if ((card.getColor() == CardColor.BLUE || card.getColor() == CardColor.RED)
                && player.getBuilds().stream()
                .noneMatch(build -> build.getType().isBlueRed()
                        && (build.getPriceLimit() == 0 || build.getPriceLimit() >= card.getPrice()))) {
            int maximumPrice = player.getBuilds().stream().filter(build -> build.getType().isBlueRed())
                    .findFirst().orElseThrow().getPriceLimit();
            return String.format("Can only build a building with print price of %s or less", maximumPrice);
        }

        int discount = discountService.getDiscount(game, card, player, inputParameters);
        discount += payments.stream().mapToInt(Payment::getDiscount).sum();

        final BuildDto mostOptimalBuild = buildService.findMostOptimalBuild(card, player, discount);

        discount += mostOptimalBuild.getExtraDiscount();

        int discountedPrice = Math.max(0, card.getPrice() - discount);

        int totalPayment = payments.stream().mapToInt(Payment::getValue).sum();

        if (totalPayment < discountedPrice) {
            return "Total payment is not enough to cover the project price";
        } else if (totalPayment > discountedPrice) {
            return "Total payment provided is more than needed";
        } else {
            return null;
        }
    }

}
