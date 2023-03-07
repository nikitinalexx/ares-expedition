package com.terraforming.ares.validation.input;

import com.terraforming.ares.cards.green.SlashAndBurnAgriculture;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.WinPointsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 20.02.2023
 */
@Component
@RequiredArgsConstructor
public class SlashAndBurnAgricultureOnBuiltEffectValidator implements OnBuiltEffectValidator<SlashAndBurnAgriculture> {
    private final WinPointsService winPointsService;

    @Override
    public Class<SlashAndBurnAgriculture> getType() {
        return SlashAndBurnAgriculture.class;
    }

    @Override
    public String validate(MarsGame game, Card card, Player player, Map<Integer, List<Integer>> input) {
        if (game.isCrysis() && winPointsService.countCrysisWinPoints(game) < 1) {
            return "Not enough Victory Points to build Slash and Burn";
        }
        return null;
    }
}
