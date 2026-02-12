package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.ai.AiExperimentalTurn;
import com.terraforming.ares.model.request.ChooseCorporationRequest;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.AiDiscoveryDecisionService;
import com.terraforming.ares.services.ai.DeepNetwork;
import com.terraforming.ares.services.ai.TestAiService;
import com.terraforming.ares.services.ai.network2.Network2CorporationAndMulliganService;
import com.terraforming.ares.services.ai.network2.Network2CorporationInputService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by oleksii.nikitin
 * Creation date 23.11.2022
 */
@Component
@RequiredArgsConstructor
public class AiPickCorporationTurn implements AiTurnProcessor {
    private final AiTurnService aiTurnService;
    private final CardService cardService;
    private final TestAiService testAiService;
    private final DeepNetwork deepNetwork;
    private final AiDiscoveryDecisionService aiDiscoveryDecisionService;
    private final Network2CorporationAndMulliganService network2CorporationAndMulliganService;
    private final Network2CorporationInputService network2CorporationInputService;


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
                selectedCorporationId = corporations.get(ThreadLocalRandom.current().nextInt(corporations.size()));
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

        if (player.getDifficulty().EXPERIMENTAL_TURN == AiExperimentalTurn.EXPERIMENT) {
            Network2CorporationAndMulliganService.CorpEvaluation corporation = network2CorporationAndMulliganService.chooseCorporation(game, player.getUuid());

            selectedCorporationId = corporation.corporationId();
            Map<Integer, List<Integer>> corporationInput = network2CorporationInputService.getCorporationInput(game, player, cardService.getCard(selectedCorporationId).getCardMetadata().getCardAction(), corporation.inputDecisions());

            aiTurnService.chooseCorporationTurn(game, ChooseCorporationRequest.builder()
                    .playerUuid(player.getUuid())
                    .corporationId(selectedCorporationId)
                    .inputParams(corporationInput)
                    .build());

            return true;
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
