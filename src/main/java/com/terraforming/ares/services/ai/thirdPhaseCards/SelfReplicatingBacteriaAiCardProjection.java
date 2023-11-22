package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.blue.SelfReplicatingBacteria;
import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.BuildDto;
import com.terraforming.ares.model.BuildType;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.ai.DeepNetwork;
import com.terraforming.ares.services.ai.ProjectionStrategy;
import com.terraforming.ares.services.ai.dto.BuildProjectPrediction;
import com.terraforming.ares.services.ai.helpers.AiCardBuildParamsService;
import com.terraforming.ares.services.ai.helpers.AiPaymentService;
import com.terraforming.ares.services.ai.turnProcessors.AiBuildProjectService;
import com.terraforming.ares.services.ai.turnProcessors.AiTurnService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SelfReplicatingBacteriaAiCardProjection<T extends Card> implements AiCardProjection<SelfReplicatingBacteria> {
    private final DeepNetwork deepNetwork;
    private final AiBuildProjectService aiBuildProjectService;
    private final AiCardBuildParamsService aiCardBuildParamsService;
    private final AiTurnService aiTurnService;
    private final AiPaymentService aiPaymentService;

    @Override
    public Class<SelfReplicatingBacteria> getType() {
        return SelfReplicatingBacteria.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference diff, MarsGame game, Player player, Card card) {
        if (player.getCardResourcesCount().get(SelfReplicatingBacteria.class) < 5) {
            player.getCardResourcesCount().put(SelfReplicatingBacteria.class, player.getCardResourcesCount().get(SelfReplicatingBacteria.class) + 1);
            return new MarsGameRowDifference();
        }

        player.getCardResourcesCount().put(SelfReplicatingBacteria.class, player.getCardResourcesCount().get(SelfReplicatingBacteria.class) + 1);
        float stateIfPutResource = deepNetwork.testState(game, player);
        player.getCardResourcesCount().put(SelfReplicatingBacteria.class, player.getCardResourcesCount().get(SelfReplicatingBacteria.class) - 1);

        MarsGame gameCopy = new MarsGame(game);
        Player playerCopy = gameCopy.getPlayerByUuid(player.getUuid());

        playerCopy.getCardResourcesCount().put(SelfReplicatingBacteria.class, playerCopy.getCardResourcesCount().get(SelfReplicatingBacteria.class) - 5);
        playerCopy.getBuilds().add(new BuildDto(BuildType.GREEN_OR_BLUE, 25));

        BuildProjectPrediction bestProjectToBuild = aiBuildProjectService.getBestProjectToBuild(
                gameCopy, playerCopy, null, ProjectionStrategy.FROM_PHASE
        );

        if (!bestProjectToBuild.isCanBuild()) {
            player.getCardResourcesCount().put(SelfReplicatingBacteria.class, player.getCardResourcesCount().get(SelfReplicatingBacteria.class) + 1);
            return new MarsGameRowDifference();
        }

        float stateIfUseResource = bestProjectToBuild.getExpectedValue();

        if (stateIfPutResource > stateIfUseResource) {
            player.getCardResourcesCount().put(SelfReplicatingBacteria.class, player.getCardResourcesCount().get(SelfReplicatingBacteria.class) + 1);
        } else {
            player.getCardResourcesCount().put(SelfReplicatingBacteria.class, player.getCardResourcesCount().get(SelfReplicatingBacteria.class) - 5);
            player.getBuilds().add(new BuildDto(BuildType.GREEN_OR_BLUE, 25));
            buildProject(game, player, bestProjectToBuild.getCard());
        }
        return new MarsGameRowDifference();
    }

    private void buildProject(MarsGame game, Player player, Card cardToBuild) {
        Map<Integer, List<Integer>> inputParams = aiCardBuildParamsService.getInputParamsForBuild(game, player, cardToBuild);
        aiTurnService.buildProject(
                game, player, cardToBuild.getId(), aiPaymentService.getCardPayments(game, player, cardToBuild, inputParams),
                inputParams
        );
    }

}
