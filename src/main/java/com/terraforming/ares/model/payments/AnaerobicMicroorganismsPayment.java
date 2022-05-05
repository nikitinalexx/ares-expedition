package com.terraforming.ares.model.payments;

import com.terraforming.ares.cards.blue.AnaerobicMicroorganisms;
import com.terraforming.ares.model.PlayerContext;
import com.terraforming.ares.model.ProjectCard;
import com.terraforming.ares.services.DeckService;

/**
 * Created by oleksii.nikitin
 * Creation date 03.05.2022
 */
public class AnaerobicMicroorganismsPayment extends GenericPayment {

    public AnaerobicMicroorganismsPayment(int value) {
        super(value);
    }

    @Override
    public PaymentType getType() {
        return PaymentType.ANAEROBIC_MICROORGANISMS;
    }

    @Override
    public int getTotalValue() {
        return getValue() * 5;
    }

    @Override
    public void pay(DeckService deckService, PlayerContext player) {
        for (Integer playerCardId : player.getPlayed().getCards()) {
            ProjectCard projectCard = deckService.getProjectCard(playerCardId);
            if (projectCard instanceof AnaerobicMicroorganisms) {
                Integer resources = player.getCardIdToResourcesCount().get(playerCardId);
                if (resources == null || resources < 2) {
                    throw new IllegalStateException("Invalid payment: Anaerobic Microorganisms < 2");
                }
                player.getCardIdToResourcesCount().put(playerCardId, resources - 2);
            }
        }
    }


}
