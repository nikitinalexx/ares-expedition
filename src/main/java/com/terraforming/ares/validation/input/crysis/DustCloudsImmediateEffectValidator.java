package com.terraforming.ares.validation.input.crysis;

import com.terraforming.ares.cards.crysis.DustClouds;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.CrysisCard;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 03.03.2023
 */
@Component
public class DustCloudsImmediateEffectValidator implements ImmediateEffectValidator<DustClouds> {
    private static final String ERROR_MESSAGE = "Dust Clouds expects input with a card choice";

    @Override
    public Class<DustClouds> getType() {
        return DustClouds.class;
    }

    @Override
    public String validate(MarsGame game, CrysisCard card, Player player, Map<Integer, List<Integer>> input) {
        if (player.getHand().getCards().isEmpty()) {
            return null;
        }
        if (CollectionUtils.isEmpty(input) || !input.containsKey(InputFlag.CRYSIS_INPUT_FLAG.getId())) {
            return ERROR_MESSAGE;
        }

        final List<Integer> crysisInput = input.get(InputFlag.CRYSIS_INPUT_FLAG.getId());
        if (CollectionUtils.isEmpty(crysisInput)) {
            return ERROR_MESSAGE;
        }
        int cardToDiscardId = crysisInput.get(0);
        if (!player.getHand().containsCard(cardToDiscardId)) {
            return ERROR_MESSAGE;
        }

        return null;
    }
}
