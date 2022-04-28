package com.terraforming.ares;

import com.terraforming.ares.controllers.GameController;
import com.terraforming.ares.controllers.PlayController;
import com.terraforming.ares.dto.PlayerUuidsDto;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.GameProcessorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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

    String firstPlayer;
    String secondPlayer;

    PlayerUuidsDto gameDto;

    @BeforeEach
    public void setUp() {
        gameDto = gameController.startNewGame(GameParameters.builder().playersCount(2).build());

        firstPlayer = gameDto.getPlayers().get(0);
        secondPlayer = gameDto.getPlayers().get(1);
    }

    @Test
    public void testGame() {
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
            playController.chooseCorporation(firstPlayer, -5);
        });

        assertEquals("Can't pick corporation that is not in your choice deck", exception.getMessage());

        gameProcessorService.process();

        playController.chooseCorporation(
                firstPlayer,
                gameController.getGameByPlayerUuid(firstPlayer).getPlayer().getCorporationsChoice().getCards().get(0)
        );

        //before all players complete corporation pick, the corporationId is null
        assertNull(gameController.getGameByPlayerUuid(firstPlayer).getPlayer().getCorporationId());

        assertEquals("WAIT", playController.getNextAction(firstPlayer));

        exception = assertThrows(IllegalStateException.class, () -> {
            playController.chooseCorporation(firstPlayer, 1);
        });

        assertEquals("Incorrent game state for corporation pick", exception.getMessage());

        //second player hasn't chosen corporation yet, the process does nothing
        gameProcessorService.process();

        assertNull(gameController.getGameByPlayerUuid(firstPlayer).getPlayer().getCorporationId());

        playController.chooseCorporation(
                secondPlayer,
                gameController.getGameByPlayerUuid(secondPlayer).getPlayer().getCorporationsChoice().getCards().get(0)
        );

        gameProcessorService.process();

        assertNotNull(gameController.getGameByPlayerUuid(firstPlayer).getPlayer().getCorporationId());
        assertNotNull(gameController.getGameByPlayerUuid(secondPlayer).getPlayer().getCorporationId());

        for (String player : players) {
            assertEquals("TURN", playController.getNextAction(player));
            assertEquals(Collections.singletonList(TurnType.PICK_STAGE), playController.getPossibleTurns(player));

            playController.chooseStage(player, 1);

            assertEquals("WAIT", playController.getNextAction(player));
        }

        gameProcessorService.process();

        assertEquals(1, gameController.getGameByPlayerUuid(firstPlayer).getPlayer().getStage());

        for (String player : players) {
            assertEquals("TURN", playController.getNextAction(player));
            assertEquals(Arrays.asList(TurnType.BUILD_GREEN_PROJECT, TurnType.SELL_CARDS, TurnType.SKIP_TURN), playController.getPossibleTurns(player));
        }

        playController.skipTurn(firstPlayer);
        assertEquals("WAIT", playController.getNextAction(firstPlayer));

        playController.sellCards(
                secondPlayer,
                gameController.getGameByPlayerUuid(secondPlayer)
                        .getPlayer()
                        .getHand()
                        .getCards()
                        .stream()
                        .limit(1)
                        .collect(Collectors.toList()
                        )
        );

        assertEquals("WAIT", playController.getNextAction(firstPlayer));
        assertEquals("TURN", playController.getNextAction(secondPlayer));
        assertEquals(9, gameController.getGameByPlayerUuid(secondPlayer).getPlayer().getHand().getCards().size());
    }

}
