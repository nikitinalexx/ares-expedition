package services;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Deck;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardFactory;
import com.terraforming.ares.services.CardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by oleksii.nikitin
 * Creation date 23.05.2022
 */
@ExtendWith(MockitoExtension.class)
class TestCardService {
    private CardService cardService;

    @Mock
    CardFactory cardFactory;

    @BeforeEach
    public void setUp() {
        cardService = Mockito.spy(new CardService(cardFactory, null));

        Mockito.doReturn(
                Deck.builder()
                        .cards(new LinkedList<>(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)))
                        .build()
        ).when(cardService).createProjectsDeck(List.of(Expansion.BASE));
    }

    @Test
    void testNoCardsLeftInGame() {
        MarsGame game = new MarsGame(
                List.of("a", "b"),
                2,
                Deck.builder().cards(new LinkedList<>(List.of(1, 2, 3, 4, 5))).build(),
                Deck.builder().cards(new LinkedList<>(List.of(100, 101, 102, 103))).build(),
                null,
                false,
                null,
                null,
                null,
                List.of(Expansion.BASE),
                false,
                null
        );

        List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());

        players.get(0).getPlayed().addCards(List.of(6));
        players.get(1).getPlayed().addCards(List.of(7));

        List<Integer> cards = cardService.dealCards(game, 3);

        assertEquals(List.of(5, 8, 9), cards);
    }

}
