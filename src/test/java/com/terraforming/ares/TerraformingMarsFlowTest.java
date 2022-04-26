package com.terraforming.ares;

import com.terraforming.ares.controllers.GameController;
import com.terraforming.ares.controllers.PlayController;
import com.terraforming.ares.dto.GameDto;
import com.terraforming.ares.dto.PlayerUuidsDto;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.GameProcessorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by oleksii.nikitin
 * Creation date 26.04.2022
 */
@SpringBootTest
public class TerraformingMarsFlowTest {
    @Autowired
    private GameController gameController;

    @Autowired
    private PlayController playController;

    @Autowired
    private GameProcessorService gameProcessorService;

    @Test
    public void testGame() {
        PlayerUuidsDto gameDto = gameController.startNewGame(GameParameters.builder().playersCount(2).build());

        List<String> players = gameDto.getPlayers();

        assertEquals(2, players.size());

        for (String playerUuid : players) {
            assertNotNull(playerUuid);
            assertEquals("TURN", playController.getNextAction(playerUuid));
            assertEquals(Collections.singletonList(TurnType.PICK_CORPORATION), playController.getPossibleTurns(playerUuid));

            //3,2,7,4
            assertEquals(2, gameController.getGameByPlayerUuid(playerUuid).getPlayer().getCorporationsChoice().getCards().size());
        }

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            playController.chooseCorporation(players.get(0), 1);
        });

        assertEquals("Can't pick corporation that is not in your choice deck", exception.getMessage());

    }

}
