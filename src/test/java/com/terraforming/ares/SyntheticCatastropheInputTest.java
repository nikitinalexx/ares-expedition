package com.terraforming.ares;

import com.terraforming.ares.cards.red.SyntheticCatastrophe;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.PlayerDifficulty;
import com.terraforming.ares.services.GameService;
import com.terraforming.ares.services.MarsContextProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Synthetic Catastrophe must not crash the game on the input its own AI produces.
 * <p>
 * Why: a 2 000-game measurement of the champion against NETWORK died at game 1 408 with
 * `NullPointerException: Cannot invoke Card.getTags() because "card" is null` - the AI's build-params service answers
 * {@code SKIP_ACTION} when there is no red card to return, {@code getCard(SKIP_ACTION)} is null, and the card passed that
 * null to {@code countCardTagsWithDynamic}. The projection path builds cards without the validator, so a legal input of the
 * AI's own making took down the whole run. Both shapes are pinned here: the skip, and an id that is not in the catalog.
 */
class SyntheticCatastropheInputTest extends EngineTestBase {

    @Autowired
    GameService gameService;
    @Autowired
    MarsContextProvider marsContextProvider;

    private MarsGame game() {
        return gameService.createNewSimulation(GameParameters.builder()
                .playerNames(new ArrayList<>(List.of("A", "B")))
                .computers(List.of(PlayerDifficulty.AGENT, PlayerDifficulty.AGENT))
                .mulligan(true)
                .expansion(Expansion.BASE).expansion(Expansion.DISCOVERY).expansion(Expansion.BUFFED_CORPORATION)
                .dummyHand(true).build());
    }

    private void effect(MarsGame game, int targetCardId) {
        new SyntheticCatastrophe(1).postProjectBuiltEffect(
                marsContextProvider.provide(game), new SyntheticCatastrophe(1), new HashMap<>(
                        Map.of(InputFlag.SYNTHETIC_CATASTROPHE_CARD.getId(), List.of(targetCardId))));
    }

    @Test
    void the_skip_answer_plays_the_card_with_no_effect() {
        MarsGame game = game();
        assertDoesNotThrow(() -> effect(game, InputFlag.SKIP_ACTION.getId()),
                "the AI answers SKIP_ACTION when there is no red card: the card must do nothing, not throw");
    }

    @Test
    void an_id_outside_the_catalog_does_not_throw() {
        MarsGame game = game();
        assertDoesNotThrow(() -> effect(game, Integer.MAX_VALUE),
                "an id that getCard does not know has no tags to revert; it must not reach countCardTagsWithDynamic");
    }
}
