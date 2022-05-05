package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.PlayerContext;
import com.terraforming.ares.model.ProjectCard;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.turn.PerformBlueActionTurn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.processors.action.BlueActionCardProcessor;
import com.terraforming.ares.services.DeckService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
public class BlueActionProcessor implements TurnProcessor<PerformBlueActionTurn> {
    private final DeckService deckService;
    private final Map<Class<?>, BlueActionCardProcessor<?>> blueActionProcessors;

    public BlueActionProcessor(DeckService deckService, List<BlueActionCardProcessor<?>> processors) {
        this.deckService = deckService;

        blueActionProcessors = processors.stream().collect(
                Collectors.toMap(
                        BlueActionCardProcessor::getType,
                        Function.identity()
                )
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public TurnResponse processTurn(PerformBlueActionTurn turn, MarsGame game) {
        PlayerContext player = game.getPlayerContexts().get(turn.getPlayerUuid());

        ProjectCard projectCard = deckService.getProjectCard(turn.getProjectId());

        BlueActionCardProcessor<ProjectCard> blueActionCardProcessor = (BlueActionCardProcessor<ProjectCard>) blueActionProcessors.get(projectCard.getClass());

        return blueActionCardProcessor.process(game, player);
    }

    @Override
    public TurnType getType() {
        return TurnType.PERFORM_BLUE_ACTION;
    }

}
