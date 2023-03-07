package com.terraforming.ares.services;

import com.terraforming.ares.mars.CrysisData;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.CrysisCard;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.StateType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by oleksii.nikitin
 * Creation date 04.03.2023
 */
@Service
@RequiredArgsConstructor
public class CrysisService {
    private static final List<Integer> ALL_PHASES = List.of(1, 2, 3, 4, 5);
    private final Random random = new Random();
    private final TerraformingService terraformingService;
    private final CardService cardService;


    public void reduceTokens(MarsGame game, CrysisCard card, int tokenNumber) {
        if (tokenNumber <= 0) {
            return;
        }

        if (game.getCrysisData().getCardToTokensCount().containsKey(card.getId())) {
            int tokenCount = game.getCrysisData().getCardToTokensCount().get(card.getId());
            tokenCount = Math.max(0, tokenCount - tokenNumber);
            game.getCrysisData().getCardToTokensCount().put(card.getId(), tokenCount);

            if (tokenCount == 0) {
                game.getCrysisData().getCardToTokensCount().remove(card.getId());
                game.getCrysisData().getOpenedCards().remove((Integer) card.getId());
            }
        }
    }

    public void forbidRandomNonPlayedPhase(MarsGame game, Player player) {
        final CrysisData crysisData = game.getCrysisData();

        Integer randomPhase = ALL_PHASES.get(random.nextInt(ALL_PHASES.size()));
        if (player.getChosenPhase() != null && player.getChosenPhase().equals(randomPhase)) {
            randomPhase++;
            randomPhase %= ALL_PHASES.size();
        }
        crysisData.getForbiddenPhases().put(player.getUuid(), randomPhase);
    }

    public void hideRandomOcean(MarsContext context) {
        final MarsGame game = context.getGame();
        List<Integer> openedOceans = new ArrayList<>();
        for (int i = 0; i < game.getPlanet().getOceans().size(); i++) {
            if (game.getPlanet().getOceans().get(i).isRevealed()) {
                openedOceans.add(i);
            }
        }
        if (openedOceans.isEmpty()) {
            game.setStateType(StateType.GAME_END, context, "No more oceans to flip down");
            return;
        }

        terraformingService.hideOcean(game, openedOceans.get(random.nextInt(openedOceans.size())));
    }
}
