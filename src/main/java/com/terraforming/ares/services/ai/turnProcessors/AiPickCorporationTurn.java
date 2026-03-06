package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.cards.corporations.MayNiProductionsCorporation;
import com.terraforming.ares.cards.corporations.ZetacellCorporation;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.ai.AiExperimentalTurn;
import com.terraforming.ares.model.request.ChooseCorporationRequest;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.AiDiscoveryDecisionService;
import com.terraforming.ares.services.ai.DeepNetwork;
import com.terraforming.ares.services.ai.TestAiService;
import com.terraforming.ares.services.ai.network2.Network2CorporationAndMulliganService;
import com.terraforming.ares.services.ai.network2.Network2CorporationInputService;
import com.terraforming.ares.services.policyai.PolicyCollectService;
import com.terraforming.ares.services.policyai.service.PolicyDecisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
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
    private final PolicyCollectService policyCollectService;
    private final PolicyDecisionService policyDecisionService;


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

        if (player.getDifficulty().EXPERIMENTAL_TURN == AiExperimentalTurn.POLICY) {
            selectedCorporationId = policyDecisionService.pickCorporation(game, player);
            Card corporation = cardService.getCard(selectedCorporationId);
            CardAction cardAction = cardService.getCard(selectedCorporationId).getCardMetadata().getCardAction();

            Map<Integer, List<Integer>> corporationInput = Map.of();


            if (cardAction == CardAction.SULTIRA_CORPORATION) {
                corporationInput = Map.of(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(policyDecisionService.sultiraCorporationPhaseChoice(game, player, corporation)));
            } else if (cardAction == CardAction.APOLLO_CORPORATION) {
                corporationInput = Map.of(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(policyDecisionService.apolloCorporationPhaseChoice(game, player, corporation)));
            } else if (cardAction == CardAction.HYPERION_SYSTEMS_CORPORATION) {
                corporationInput = Map.of(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(policyDecisionService.hyperionCorporationPhaseChoice(game, player, corporation)));
            } else if (cardAction == CardAction.EXOCORP_CORPORATION) {
                corporationInput = Map.of(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(policyDecisionService.exocorpCorporationPhaseChoice(game, player, corporation)));
            } else if (cardAction == CardAction.NEBU_LABS_CORPORATION) {
                corporationInput = Map.of(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(policyDecisionService.nebulabsCorporationPhaseChoice(game, player, corporation)));
            } else if (cardAction == CardAction.MODPRO_CORPORATION) {
                corporationInput = Map.of(InputFlag.TAG_INPUT.getId(), List.of(policyDecisionService.modproCorporationPhaseChoice(game, player, corporation)));
            } else if (cardAction == CardAction.AUSTELLAR_CORPORATION) {
                corporationInput = policyDecisionService.austellarTagAndMilestoneChoice(game, player, corporation);
            }

            aiTurnService.chooseCorporationTurn(game, ChooseCorporationRequest.builder()
                    .playerUuid(player.getUuid())
                    .corporationId(selectedCorporationId)
                    .inputParams(corporationInput)
                    .build());

            return true;
        }

        if (player.getDifficulty().EXPERIMENTAL_TURN == AiExperimentalTurn.EXPERIMENT) {
            Network2CorporationAndMulliganService.CorpEvaluation corpEvaluation = network2CorporationAndMulliganService.chooseCorporation(game, player.getUuid());

            selectedCorporationId = corpEvaluation.corporationId();
            Card corporation = cardService.getCard(selectedCorporationId);

            final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
            Player anotherPlayer = players.get(0) == player ? players.get(1) : players.get(0);


            CardAction cardAction = cardService.getCard(selectedCorporationId).getCardMetadata().getCardAction();

            Map<Integer, List<Integer>> corporationInput = network2CorporationInputService.getCorporationInput(game, player, cardAction, corpEvaluation.inputDecisions());

            if (cardAction == CardAction.SULTIRA_CORPORATION) {
                policyCollectService.sultiraCorporationPhaseChoice(game, List.of(player, anotherPlayer), corporation, corporationInput.get(InputFlag.PHASE_UPGRADE_CARD.getId()).getFirst());
            } else if (cardAction == CardAction.APOLLO_CORPORATION) {
                policyCollectService.apolloCorporationPhaseChoice(game, List.of(player, anotherPlayer), corporation, corporationInput.get(InputFlag.PHASE_UPGRADE_CARD.getId()).getFirst());
            } else if (cardAction == CardAction.HYPERION_SYSTEMS_CORPORATION) {
                policyCollectService.hyperionCorporationChoice(game, List.of(player, anotherPlayer), corporation, corporationInput.get(InputFlag.PHASE_UPGRADE_CARD.getId()).getFirst());
            } else if (cardAction == CardAction.EXOCORP_CORPORATION) {
                policyCollectService.exocorpCorporationChoice(game, List.of(player, anotherPlayer), corporation, corporationInput.get(InputFlag.PHASE_UPGRADE_CARD.getId()).getFirst());
            } else if (cardAction == CardAction.NEBU_LABS_CORPORATION) {
                policyCollectService.nebulabsCorporationChoice(game, List.of(player, anotherPlayer), corporation, corporationInput.get(InputFlag.PHASE_UPGRADE_CARD.getId()).getFirst());
            } else if (cardAction == CardAction.MODPRO_CORPORATION) {
                policyCollectService.modProCorporationChoice(game, List.of(player, anotherPlayer), corporation, corporationInput.get(InputFlag.TAG_INPUT.getId()).getFirst());
            } else if (cardAction == CardAction.AUSTELLAR_CORPORATION) {
                policyCollectService.austellarTagAndMilestoneChoice(game, List.of(player, anotherPlayer), corporation, corporationInput.get(InputFlag.AUSTELLAR_CORPORATION_MILESTONE.getId()).getFirst(), corporationInput.get(InputFlag.TAG_INPUT.getId()).getFirst());
            }

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
