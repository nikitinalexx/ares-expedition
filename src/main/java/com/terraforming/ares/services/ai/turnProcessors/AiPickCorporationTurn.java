package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.mars.MarsGame;
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

    private static final List<Integer> CORPORATION_PRIORITY = List.of(
            10006, /* Tharsis */
            10015, /* MayNi */
            10104, 10011, /* Saturn */
            10105, 10012, /* Zetacell */
            10102, 10009, /* Phobolog */
            10017, /* Interplanetary */
            10003, /* LaunchStar */
            10002, /* DevTechs */
            10007, /* Credicor */
            10005, /* Teractor */
            10107, 10014, /* Inventrix */
            10004, /* Thorgate */
            10103, 10010, /* Mining */
            10106, 10013, /* Ecoline */
            10001, /* Celestior */
            10101, 10008, /* Arclight */
            10108, 10016, /* UNMI */
            10100, 10000 /* Helion */
    );

    @Override
    public TurnType getType() {
        return TurnType.PICK_CORPORATION;
    }

    @Override
    public boolean processTurn(MarsGame game, Player player) {
        LinkedList<Integer> corporations = player.getCorporations().getCards();
        //int selectedCorporationId = corporations.stream().min(Comparator.comparingInt(corporation -> CORPORATION_PRIORITY.indexOf(corporation))).orElseThrow();
        int selectedCorporationId = corporations.get(random.nextInt(corporations.size()));
        aiTurnService.chooseCorporationTurn(game, ChooseCorporationRequest.builder()
                .playerUuid(player.getUuid())
                .corporationId(selectedCorporationId)
                .build());

        return true;
    }

}
