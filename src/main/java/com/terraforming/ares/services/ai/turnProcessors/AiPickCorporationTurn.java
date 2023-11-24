package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.request.ChooseCorporationRequest;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.AiDiscoveryDecisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Created by oleksii.nikitin
 * Creation date 23.11.2022
 */
@Component
@RequiredArgsConstructor
public class AiPickCorporationTurn implements AiTurnProcessor {
    private final Random random = new Random();
    private final AiTurnService aiTurnService;
    private final CardService cardService;
    private final AiDiscoveryDecisionService aiDiscoveryDecisionService;


    @Override
    public TurnType getType() {
        return TurnType.PICK_CORPORATION;
    }

    @Override
    public boolean processTurn(MarsGame game, Player player) {
        LinkedList<Integer> corporations = player.getCorporations().getCards();
        int selectedCorporationId;
        if (Constants.AI_CORP_RANDOM) {
            selectedCorporationId = corporations.get(random.nextInt(corporations.size()));
        } else {
//            selectedCorporationId = corporations.stream().min(Comparator.comparingInt(Constants.CORPORATION_PRIORITY::indexOf)).orElseThrow();
        }
        Card corporationCard = cardService.getCard(selectedCorporationId);
        CardAction corporationCardAction = corporationCard.getCardMetadata().getCardAction();

        aiTurnService.chooseCorporationTurn(game, ChooseCorporationRequest.builder()
                .playerUuid(player.getUuid())
                .corporationId(selectedCorporationId)
                .inputParams(aiDiscoveryDecisionService.getCorporationInput(game, player, corporationCardAction))
                .build());

        return true;
    }

}
