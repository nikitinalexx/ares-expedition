package com.terraforming.ares.services.ai.network2;

import com.terraforming.ares.cards.blue.BacterialAggregates;
import com.terraforming.ares.cards.green.PrivateInvestorBeach;
import com.terraforming.ares.cards.red.CeosFavoriteProject;
import com.terraforming.ares.cards.red.SyntheticCatastrophe;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.milestones.Milestone;
import com.terraforming.ares.model.parameters.OceanRequirement;
import com.terraforming.ares.model.parameters.ParameterColor;
import com.terraforming.ares.model.payments.MegacreditsPayment;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.SpecialEffectsService;
import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.ai.advanced.AdvancedAiDataCollectionService;
import com.terraforming.ares.services.ai.turnProcessors.AiTurnService;
import com.terraforming.ares.services.ai.turnProcessors.AiUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class Network2ProjectBuildService {
    private final CardService cardService;

    public List<Card> getCardsWithStrongRestrictions(Player player, List<Card> cards) {
        return cards.stream().filter(card -> {
            if (card.getClass() == CeosFavoriteProject.class && (player.getCardResourcesCount().isEmpty() || player.getCardResourcesCount().size() == 1 && player.getCardResourcesCount().getOrDefault(BacterialAggregates.class, 0) > AiConstants.BACTERIAL_AGGREGATES_COUNT_FOR_MICROBE_PUT)) {
                return true;
            }

            if (card.getCardMetadata().getCardAction() == CardAction.SYNTHETIC_CATASTROPHE) {
                List<Card> redCards = player.getPlayed().getCards().stream().map(cardService::getCard).filter(c -> c.getColor() == CardColor.RED).toList();
                return redCards.isEmpty();
            }

            return false;
        }).toList();
    }

}
