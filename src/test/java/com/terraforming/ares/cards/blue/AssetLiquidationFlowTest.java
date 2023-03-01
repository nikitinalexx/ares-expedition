package com.terraforming.ares.cards.blue;

import com.terraforming.ares.controllers.PlayController;
import com.terraforming.ares.factories.PlanetFactory;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Deck;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.StateType;
import com.terraforming.ares.model.payments.MegacreditsPayment;
import com.terraforming.ares.model.request.BuildProjectRequest;
import com.terraforming.ares.model.request.ChoosePhaseRequest;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.repositories.caching.CachingGameRepository;
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
    private CachingGameRepository gameRepository;

    @Autowired
    private GameProcessorService gameProcessorService;

    @Autowired
    private PlanetFactory planetFactory;

    private List<Player> players;
    private Player firstPlayer;
    private Player secondPlayer;

    @BeforeEach
    public void setUp() {
        MarsGame marsGame = new MarsGame(
                List.of("First", "Second"),
                3,
                buildProjectsDeck(),
                buildCorporationsDeck(),
                planetFactory.createMars(null),
                false,
                null,
                null,
                null,
                List.of(Expansion.BASE),
                false,
                null
        );
        marsGame.setStateType(StateType.PICK_PHASE, null);

        players = new ArrayList<>(marsGame.getPlayerUuidToPlayer().values());
        firstPlayer = players.get(0);
        secondPlayer = players.get(1);

        if (!firstPlayer.getHand().containsCard(ASSET_LIQUIDATION_CARD_ID)) {
            Player temp = firstPlayer;
            firstPlayer = secondPlayer;
            secondPlayer = temp;
        }

        gameRepository.newGame(marsGame);
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
