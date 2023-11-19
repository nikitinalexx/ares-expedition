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
        int selectedCorporationId = corporations.stream().min(Comparator.comparingInt(Constants.CORPORATION_PRIORITY::indexOf)).orElseThrow();
        Card corporationCard = cardService.getCard(selectedCorporationId);
        CardAction corporationCardAction = corporationCard.getCardMetadata().getCardAction();

        Map<Integer, List<Integer>> inputParams = new HashMap<>();

        if (corporationCardAction == CardAction.HYPERION_SYSTEMS_CORPORATION) {
            inputParams.put(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(aiDiscoveryDecisionService.choosePhaseUpgrade(game, player, Constants.PERFORM_BLUE_ACTION_PHASE)));
        } else if (corporationCardAction == CardAction.APOLLO_CORPORATION) {
            inputParams.put(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(aiDiscoveryDecisionService.choosePhaseUpgrade(game, player, Constants.BUILD_BLUE_RED_PROJECTS_PHASE)));
        } else if (corporationCardAction == CardAction.EXOCORP_CORPORATION) {
            inputParams.put(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(aiDiscoveryDecisionService.choosePhaseUpgrade(game, player, Constants.DRAFT_CARDS_PHASE)));
        } else if (corporationCardAction == CardAction.AUSTELLAR_CORPORATION) {
            inputParams.put(InputFlag.AUSTELLAR_CORPORATION_MILESTONE.getId(), List.of(aiDiscoveryDecisionService.chooseAustellarCorporationMilestone(game)));
            inputParams.put(InputFlag.TAG_INPUT.getId(), List.of(aiDiscoveryDecisionService.chooseDynamicTagValue(List.of())));
        } else if (corporationCardAction == CardAction.MODPRO_CORPORATION) {
            inputParams.put(InputFlag.TAG_INPUT.getId(), List.of(aiDiscoveryDecisionService.chooseModProTagValue()));
        } else if (corporationCardAction == CardAction.NEBU_LABS_CORPORATION) {
            inputParams.put(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(aiDiscoveryDecisionService.choosePhaseUpgrade(game, player)));
        }

        aiTurnService.chooseCorporationTurn(game, ChooseCorporationRequest.builder()
                .playerUuid(player.getUuid())
                .corporationId(selectedCorporationId)
                .inputParams(inputParams)
                .build());

        return true;
    }

}
