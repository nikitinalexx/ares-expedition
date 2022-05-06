package com.terraforming.ares.cards.blue;

import com.terraforming.ares.controllers.PlayController;
import com.terraforming.ares.factories.PlanetFactory;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Deck;
import com.terraforming.ares.model.PlayerContext;
import com.terraforming.ares.model.StateType;
import com.terraforming.ares.model.payments.MegacreditsPayment;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.repositories.GameRepository;
import com.terraforming.ares.services.GameProcessorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

import static com.terraforming.ares.model.Constants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by oleksii.nikitin
 * Creation date 06.05.2022
 */
@SpringBootTest
class AssetLiquidationFlowTest {

    @Autowired
    private PlayController playController;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private GameProcessorService gameProcessorService;

    @Autowired
    private PlanetFactory planetFactory;

    private List<PlayerContext> players;
    private PlayerContext firstPlayer;
    private PlayerContext secondPlayer;

    @BeforeEach
    public void setUp() {
        MarsGame marsGame = new MarsGame(
                2,
                3,
                buildProjectsDeck(),
                buildCorporationsDeck(),
                planetFactory.createMars(null)
        );
        marsGame.setStateType(StateType.PICK_STAGE);

        players = new ArrayList<>(marsGame.getPlayerContexts().values());
        firstPlayer = players.get(0);
        secondPlayer = players.get(1);

        if (!firstPlayer.getHand().containsCard(ASSET_LIQUIDATION_CARD_ID)) {
            PlayerContext temp = firstPlayer;
            firstPlayer = secondPlayer;
            secondPlayer = temp;
        }

        gameRepository.save(marsGame);
    }


    @Test
    void test() {
        firstPlayer.setMc(100);
        secondPlayer.setMc(100);

        assertEquals(0, firstPlayer.getCanBuildInSecondStage());
        assertEquals(0, secondPlayer.getCanBuildInSecondStage());

        assertTrue(
                players.stream()
                        .map(PlayerContext::getUuid)
                        .map(playController::getNextAction)
                        .allMatch("TURN"::equals)
        );

        playController.chooseStage(firstPlayer.getUuid(), 2);
        playController.chooseStage(secondPlayer.getUuid(), 3);

        gameProcessorService.asyncUpdate();

        assertEquals(2, firstPlayer.getCanBuildInSecondStage());
        assertEquals(1, secondPlayer.getCanBuildInSecondStage());

        assertTrue(
                players.stream()
                        .map(PlayerContext::getUuid)
                        .map(playController::getPossibleTurns)
                        .allMatch(
                                Arrays.asList(
                                        TurnType.BUILD_BLUE_RED_PROJECT,
                                        TurnType.SELL_CARDS,
                                        TurnType.SKIP_TURN
                                )::equals
                        )
        );

        playController.buildBlueRedProject(firstPlayer.getUuid(), ASSET_LIQUIDATION_CARD_ID, Collections.emptyList(), Collections.emptyMap());
        playController.buildBlueRedProject(secondPlayer.getUuid(), ADAPTATION_TECHNOLOGY_CARD_ID, Collections.singletonList(new MegacreditsPayment(12)), Collections.emptyMap());

        gameProcessorService.asyncUpdate();

        assertFalse(playController.getPossibleTurns(firstPlayer.getUuid()).isEmpty());
        assertTrue(playController.getPossibleTurns(secondPlayer.getUuid()).isEmpty());

        assertEquals(2, firstPlayer.getCanBuildInSecondStage());
        assertEquals(0, secondPlayer.getCanBuildInSecondStage());

        playController.buildBlueRedProject(firstPlayer.getUuid(), ASSEMBLY_LINES_CARD_ID, Collections.singletonList(new MegacreditsPayment(13)), Collections.emptyMap());

        gameProcessorService.asyncUpdate();

        assertEquals(1, firstPlayer.getCanBuildInSecondStage());

        playController.buildBlueRedProject(firstPlayer.getUuid(), ARTIFICIAL_JUNGLE_CARD_ID, Collections.singletonList(new MegacreditsPayment(5)), Collections.emptyMap());

        gameProcessorService.asyncUpdate();

        assertEquals(0, firstPlayer.getCanBuildInSecondStage());

        assertTrue(
                players.stream()
                        .map(PlayerContext::getUuid)
                        .map(playController::getPossibleTurns)
                        .allMatch(
                                Arrays.asList(
                                        TurnType.PERFORM_BLUE_ACTION,
                                        TurnType.SELL_CARDS,
                                        TurnType.SKIP_TURN
                                )::equals
                        )
        );
    }

    private Deck buildProjectsDeck() {
        return Deck.builder().cards(
                new LinkedList<>(
                        Arrays.asList(
                                ADAPTATION_TECHNOLOGY_CARD_ID,
                                ADVANCED_ALLOYS_CARD_ID,
                                ADVANCED_SCREENING_TECHNOLOGY_CARD_ID,
                                ARTIFICIAL_JUNGLE_CARD_ID,
                                ASSEMBLY_LINES_CARD_ID,
                                ASSET_LIQUIDATION_CARD_ID
                        )
                )
        ).build();
    }

    private Deck buildCorporationsDeck() {
        return Deck.builder().cards(
                new LinkedList<>(
                        Arrays.asList(
                                1, 2, 3, 4
                        )
                )
        ).build();
    }
}
