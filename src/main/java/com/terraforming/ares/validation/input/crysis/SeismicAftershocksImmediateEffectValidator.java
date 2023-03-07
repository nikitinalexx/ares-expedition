package com.terraforming.ares.validation.input.crysis;

import com.terraforming.ares.cards.crysis.SeismicAftershocks;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.CrysisCard;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 03.03.2023
 */
@Component
@RequiredArgsConstructor
public class SeismicAftershocksImmediateEffectValidator implements ImmediateEffectValidator<SeismicAftershocks> {
    private static final String ERROR_MESSAGE = "Seismic Aftershock expects input with choice of a Green card or Ocean flip";
    private final CardService cardService;

    @Override
    public Class<SeismicAftershocks> getType() {
        return SeismicAftershocks.class;
    }

    @Override
    public String validate(MarsGame game, CrysisCard card, Player player, Map<Integer, List<Integer>> input) {
        if (CollectionUtils.isEmpty(input) || !input.containsKey(InputFlag.CRYSIS_INPUT_FLAG.getId())) {
            return ERROR_MESSAGE;
        }

        final List<Integer> crysisInput = input.get(InputFlag.CRYSIS_INPUT_FLAG.getId());
        if (CollectionUtils.isEmpty(crysisInput)) {
            return ERROR_MESSAGE;
        }
        int choiceOption = crysisInput.get(0);

        if (choiceOption != InputFlag.CRYSIS_INPUT_OPTION_1.getId() && choiceOption != InputFlag.CRYSIS_INPUT_OPTION_2.getId()) {
            return ERROR_MESSAGE;
        }

        if (choiceOption == InputFlag.CRYSIS_INPUT_OPTION_1.getId()
                && (crysisInput.size() < 2
                || !player.getPlayed().containsCard(crysisInput.get(1))
                || cardService.getCard(crysisInput.get(1)).getColor() != CardColor.GREEN)) {
            return ERROR_MESSAGE;
        }

        return null;
    }
}
