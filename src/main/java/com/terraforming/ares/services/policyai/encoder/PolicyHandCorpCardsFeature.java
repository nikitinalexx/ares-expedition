package com.terraforming.ares.services.policyai.encoder;

import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;

import java.util.LinkedList;

public class PolicyHandCorpCardsFeature implements PolicyFeatureBlock {

    @Override
    public void encode(TableContext ctx, PolicyRecord dto) {
        Player player = ctx.getPlayer();
        CardService cardService = ctx.getCardService();

        if (player.getSelectedCorporationCard() == null) {

            LinkedList<Integer> cards = player.getCorporations().getCards();
            int corporationsMask = 0;

            for (Integer corporationId : cards) {
                Card corporationCard = cardService.getCard(corporationId);

                CardAction action = corporationCard.getCardMetadata().getCardAction();

                int index = AiConstants.CORP_INDEX.get(action);
                corporationsMask |= (1 << index);
            }

            dto.corporationMask[0] = corporationsMask;
        }
    }

}
