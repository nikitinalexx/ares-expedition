package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.request.ChooseCorporationRequest;
import com.terraforming.ares.model.turn.TurnType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Created by oleksii.nikitin
 * Creation date 23.11.2022
 */
@Component
@RequiredArgsConstructor
public class AiPickCorporationTurn implements AiTurnProcessor {
    private final Random random = new Random();
    private final AiTurnService aiTurnService;



    @Override
    public TurnType getType() {
        return TurnType.PICK_CORPORATION;
    }

    @Override
    public boolean processTurn(MarsGame game, Player player) {
        LinkedList<Integer> corporations = player.getCorporations().getCards();
        int selectedCorporationId = corporations.stream().min(Comparator.comparingInt(Constants.CORPORATION_PRIORITY::indexOf)).orElseThrow();
        aiTurnService.chooseCorporationTurn(game, ChooseCorporationRequest.builder()
                .playerUuid(player.getUuid())
                .corporationId(selectedCorporationId)
                .build());

        return true;
    }

}
