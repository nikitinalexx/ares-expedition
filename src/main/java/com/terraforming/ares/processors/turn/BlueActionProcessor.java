package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.turn.PerformBlueActionTurn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.processors.action.BlueActionCardProcessor;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.CrysisService;
import com.terraforming.ares.services.SpecialEffectsService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
    private final CardService cardService;
    private final SpecialEffectsService specialEffectsService;
    private final CrysisService crysisService;
    private final Map<Class<?>, BlueActionCardProcessor<?>> blueActionProcessors;

    public BlueActionProcessor(CardService cardService,
                               SpecialEffectsService specialEffectsService,
                               CrysisService crysisService,
                               List<BlueActionCardProcessor<?>> processors) {
        this.cardService = cardService;
        this.specialEffectsService = specialEffectsService;
        this.crysisService = crysisService;

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
        Player player = game.getPlayerUuidToPlayer().get(turn.getPlayerUuid());

        Card projectCard = cardService.getCard(turn.getProjectId());

        BlueActionCardProcessor<ProjectCard> blueActionCardProcessor = (BlueActionCardProcessor<ProjectCard>) blueActionProcessors.get(projectCard.getClass());

        TurnResponse response = blueActionCardProcessor.process(game, player, projectCard, turn.getInputParams());

        if (specialEffectsService.ownsSpecialEffect(player, SpecialEffect.ASSEMBLY_LINES)) {
            player.setMc(player.getMc() + 1);
        }
        if (player.getActivatedBlueCards().containsCard(projectCard.getId())) {
            player.setBlueActionExtraActivationsLeft(player.getBlueActionExtraActivationsLeft() - 1);
            player.getActivatedBlueCardsTwice().addCard(projectCard.getId());
        } else {
            player.getActivatedBlueCards().addCard(projectCard.getId());
        }

        if (game.isCrysis()) {
            new ArrayList<>(game.getCrysisData().getOpenedCards())
                    .stream()
                    .map(cardService::getCrysisCard)
                    .filter(card -> card.getSpecialCrysisGoal() == SpecialCrysisGoal.ACTION_IN_THIRD_PHASE)
                    .forEach(card -> crysisService.reduceTokens(game, card, 1));
        }

        return response;
    }

    @Override
    public TurnType getType() {
        return TurnType.PERFORM_BLUE_ACTION;
    }

}
