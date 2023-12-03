package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.request.ChooseCorporationRequest;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.AiDiscoveryDecisionService;
import com.terraforming.ares.services.ai.AiPickCardProjectionService;
import com.terraforming.ares.services.ai.DeepNetwork;
import com.terraforming.ares.services.ai.TestAiService;
import com.terraforming.ares.services.ai.dto.CardProjection;
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
    private final TestAiService testAiService;
    private final DeepNetwork deepNetwork;
    private final AiPickCardProjectionService aiPickCardProjectionService;
    private final AiDiscoveryDecisionService aiDiscoveryDecisionService;


    @Override
    public TurnType getType() {
        return TurnType.PICK_CORPORATION;
    }

    @Override
    public boolean processTurn(MarsGame game, Player player) {
        List<Integer> corporations = player.getCorporations().getCards();
        int selectedCorporationId = corporations.get(0);

        switch (player.getDifficulty().CARDS_PICK) {
            case RANDOM:
            case FILE_VALUE:
                selectedCorporationId = corporations.get(random.nextInt(corporations.size()));
                break;
            case NETWORK_PROJECTION:
                MarsGame gameAfterOpponentPlay = testAiService.projectOpponentCorporationBuildExperiment(game, player);

                float bestCorporationChoice = 0;

                for (Integer corporation : player.getCorporations().getCards()) {
                    MarsGame projectionAfterCorporation = testAiService.projectPlayerBuildCorporationExperiment(gameAfterOpponentPlay, player, corporation);

                    float afterCorporationChance = deepNetwork.testState(projectionAfterCorporation, projectionAfterCorporation.getPlayerByUuid(player.getUuid()));

                    if (afterCorporationChance > bestCorporationChoice) {
                        bestCorporationChoice = afterCorporationChance;
                        selectedCorporationId = corporation;
                    }
                }

                break;
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
