package com.terraforming.ares.services.ai;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.StandardProjectType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.SpecialEffectsService;
import com.terraforming.ares.services.StandardProjectService;
import com.terraforming.ares.services.ai.turnProcessors.AiTurnService;
import com.terraforming.ares.services.policyai.PolicyCollectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AiEndgameService {
    private final AiTurnService aiTurnService;
    private final StandardProjectService standardProjectService;
    private final TestAiService testAiService;
    private final CardService cardService;
    private final SpecialEffectsService specialEffectsService;
    private final PolicyCollectService policyCollectService;

    public boolean doFinalActionsIfGameFinished(MarsGame game, Player player) {
        if (!game.gameEndCondition()) {
            return false;
        }

        if (!player.getHand().isEmpty()) {
            Card corporation = cardService.getCard(player.getSelectedCorporationCard());
            if (corporation.getCardMetadata().getCardAction() == CardAction.HELION_CORPORATION && player.getHeat() > 0) {
                player.setMc(player.getMc() + player.getHeat());
                player.setHeat(0);
            }
            aiTurnService.sellCards(player, game, new ArrayList<>(player.getHand().getCards()));
            return true;
        }

        switch (player.getDifficulty().THIRD_PHASE_ACTION) {
            case SMART, RANDOM -> {
                List<StandardProjectType> availableStandardProjects = standardProjectService.getAvailableStandardProjects(game, player);
                if (!availableStandardProjects.isEmpty()) {
                    performRandomStandardProject(game, player, availableStandardProjects);
                    return true;
                }
            }
            case NETWORK -> {
                StandardProjectType type = null;
                float bestState = -1;

                for (StandardProjectType standardProjectType : List.of(StandardProjectType.FOREST, StandardProjectType.OCEAN, StandardProjectType.TEMPERATURE, StandardProjectType.INFRASTRUCTURE)) {
                    String validationResult = standardProjectService.validateStandardProject(game, player, standardProjectType);
                    if (validationResult == null) {
                        float projectedChance = testAiService.projectPlayStandardAction(game, player.getUuid(), standardProjectType);
                        if (projectedChance > bestState) {
                            type = standardProjectType;
                            bestState = projectedChance;
                        }
                    }
                }

                if (type != null) {
                    aiTurnService.standardProjectTurn(game, player, type);
                    return true;
                }
            }
        }

        aiTurnService.confirmGameEnd(game, player);
        return true;
    }

    public boolean isFinishingGame(MarsGame game, Player player) {
        if (!game.gameEndCondition() && canFinishGame(game, player)) {
            if (!player.getHand().isEmpty()) {
                policyCollectService.sellCards(game, player, player.getHand().getCards().stream().map(cardService::getCard).collect(Collectors.toList()));
                aiTurnService.sellCards(player, game, new ArrayList<>(player.getHand().getCards()));
            }
            while (game.getPlanet().temperatureLeft() > 0) {
                finalizeStandardProject(game, player, StandardProjectType.TEMPERATURE);
            }
            while (game.getPlanet().oxygenLeft() > 0) {
                finalizeStandardProject(game, player, StandardProjectType.FOREST);
            }
            while (game.getPlanet().oceansLeft() > 0) {
                finalizeStandardProject(game, player, StandardProjectType.OCEAN);
            }
            while (game.getPlanet().infrastructureLeft() > 0) {
                aiTurnService.standardProjectTurn(game, player, StandardProjectType.INFRASTRUCTURE);
            }
            return true;
        }
        return false;
    }

    private void finalizeStandardProject(MarsGame game, Player player, StandardProjectType standardProjectType) {
        policyCollectService.standardProjectTurn(game, player, standardProjectType);
        aiTurnService.standardProjectTurn(game, player, standardProjectType);
    }

    public boolean canFinishGame(MarsGame game, Player player) {
        int mc = player.getMc();
        mc += player.getHand().size() * specialEffectsService.getCardPrice(player);

        mc -= game.getPlanet().oceansLeft() * standardProjectService.getProjectPrice(player, StandardProjectType.OCEAN);

        if (mc < 0) {
            return false;
        }

        mc -= game.getPlanet().temperatureLeft() * standardProjectService.getProjectPrice(player, StandardProjectType.TEMPERATURE);

        if (mc < 0) {
            return false;
        }

        mc -= game.getPlanet().oxygenLeft() * standardProjectService.getProjectPrice(player, StandardProjectType.FOREST);

        if (mc < 0) {
            return false;
        }

        mc -= game.getPlanet().infrastructureLeft() * standardProjectService.getProjectPrice(player, StandardProjectType.INFRASTRUCTURE);

        return mc >= 0;
    }

    private void performRandomStandardProject(MarsGame game, Player player, List<StandardProjectType> standardProjects) {
        int indexToPick = standardProjects.size() == 1 ? 0 : ThreadLocalRandom.current().nextInt(standardProjects.size());
        aiTurnService.standardProjectTurn(game, player, standardProjects.get(indexToPick));
    }

}
