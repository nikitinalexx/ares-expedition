package com.terraforming.ares.validation;

import com.terraforming.ares.cards.blue.AnaerobicMicroorganisms;
import com.terraforming.ares.model.PlayerContext;
import com.terraforming.ares.model.ProjectCard;
import com.terraforming.ares.model.payments.Payment;
import com.terraforming.ares.model.payments.PaymentType;
import com.terraforming.ares.services.DeckService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created by oleksii.nikitin
 * Creation date 03.05.2022
 */
@Component
@RequiredArgsConstructor
public class AnaerobicMicroorganismsPaymentValidator implements PaymentValidator {
    private final DeckService deckService;

    @Override
    public PaymentType getType() {
        return PaymentType.ANAEROBIC_MICROORGANISMS;
    }

    @Override
    public String validate(PlayerContext player, Payment payment) {
        if (payment.getValue() != 2) {
            return "Invalid payment: Anaerobic Microorganisms can be paid only with a value of 2";
        }

        for (Integer playerCardId : player.getPlayed().getCards()) {
            ProjectCard projectCard = deckService.getProjectCard(playerCardId);
            if (projectCard instanceof AnaerobicMicroorganisms) {
                Integer resources = player.getCardIdToResourcesCount().get(playerCardId);
                if (resources == null || resources < 2) {
                    return "Invalid payment: Anaerobic Microorganisms < 2";
                }
            }
        }
        return null;
    }
}
