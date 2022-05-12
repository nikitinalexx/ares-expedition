package com.terraforming.ares;

import com.terraforming.ares.controllers.GameController;
import com.terraforming.ares.controllers.PlayController;
import com.terraforming.ares.dto.CardDto;
import com.terraforming.ares.dto.PlayerUuidsDto;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.payments.MegacreditsPayment;
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
    void test() {
        List<String> players = gameDto.getPlayers();

        assertEquals(2, players.size());

        for (String playerUuid : players) {
            assertNotNull(playerUuid);
            assertEquals("TURN", playController.getNextAction(playerUuid));
            assertEquals(Collections.singletonList(TurnType.PICK_CORPORATION), playController.getPossibleTurns(playerUuid));

            //3,2,7,4
            assertEquals(2, gameController.getGameByPlayerUuid(playerUuid).getPlayer().getCorporations().size());
        }

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            playController.chooseCorporation(firstPlayer, -5);
        });

        assertEquals("Can't pick corporation that is not in your choice deck", exception.getMessage());

        gameProcessorService.asyncUpdate();

        playController.chooseCorporation(
                firstPlayer,
                gameController.getGameByPlayerUuid(firstPlayer).getPlayer().getCorporations().get(0).getId()
        );

        //before all players complete corporation pick, the corporationId is null
        assertNull(gameController.getGameByPlayerUuid(firstPlayer).getPlayer().getCorporationId());

        assertEquals("WAIT", playController.getNextAction(firstPlayer));

        exception = assertThrows(IllegalStateException.class, () -> {
            playController.chooseCorporation(firstPlayer, 1);
        });

        assertEquals("Incorrect game state for a turn PICK_CORPORATION", exception.getMessage());

        //second player hasn't chosen corporation yet, the process does nothing
        gameProcessorService.asyncUpdate();

        assertNull(gameController.getGameByPlayerUuid(firstPlayer).getPlayer().getCorporationId());

        playController.chooseCorporation(
                secondPlayer,
                gameController.getGameByPlayerUuid(secondPlayer).getPlayer().getCorporations().get(0).getId()
        );

        gameProcessorService.asyncUpdate();

        assertNotNull(gameController.getGameByPlayerUuid(firstPlayer).getPlayer().getCorporationId());
        assertNotNull(gameController.getGameByPlayerUuid(secondPlayer).getPlayer().getCorporationId());

        for (String player : players) {
            assertEquals("TURN", playController.getNextAction(player));
            assertEquals(Collections.singletonList(TurnType.PICK_PHASE), playController.getPossibleTurns(player));

            playController.choosePhase(player, 1);

            assertEquals("WAIT", playController.getNextAction(player));
        }

        gameProcessorService.asyncUpdate();

        assertEquals(1, gameController.getGameByPlayerUuid(firstPlayer).getPlayer().getPhase());

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
                        .stream()
                        .limit(1)
                        .map(CardDto::getId)
                        .collect(Collectors.toList())
        );

        assertEquals("WAIT", playController.getNextAction(firstPlayer));
        assertEquals("TURN", playController.getNextAction(secondPlayer));
        assertEquals(9, gameController.getGameByPlayerUuid(secondPlayer).getPlayer().getHand().size());

        exception = assertThrows(IllegalStateException.class, () -> {
            playController.buildGreenProject(
                    secondPlayer,
                    gameController.getGameByPlayerUuid(secondPlayer).getPlayer().getHand().get(0).getId(),
                    Collections.singletonList(new MegacreditsPayment(10)),
                    Collections.emptyMap()
            );
        });

        assertEquals("Total payment provided is more than needed", exception.getMessage());

        playController.buildGreenProject(
                secondPlayer,
                gameController.getGameByPlayerUuid(secondPlayer).getPlayer().getHand().get(0).getId(),
                Collections.singletonList(new MegacreditsPayment(5)),
                Collections.emptyMap()
        );
    }

}
