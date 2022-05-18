package com.terraforming.ares.validation.payment;

import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.payments.Payment;
import com.terraforming.ares.model.payments.PaymentType;
import com.terraforming.ares.services.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created by oleksii.nikitin
 * Creation date 03.05.2022
 */
@Component
@RequiredArgsConstructor
public class HeatPaymentValidator implements PaymentValidator {
    private final CardService cardService;

    @Override
    public PaymentType getType() {
        return PaymentType.HEAT;
    }

    @Override
    public String validate(Card card, Player player, Payment payment) {
        Card corporationCard = cardService.getCard(player.getSelectedCorporationCard());
        CardAction cardAction = corporationCard.getCardMetadata().getCardAction();

        if (cardAction != CardAction.HELION_CORPORATION) {
            return "Only helion corporation may pay with heat";
        }

        if (player.getHeat() < payment.getValue() + card.heatSpendOnBuild()) {
            return "Not enough HEAT to build the project";
        }
        return null;
    }
}
