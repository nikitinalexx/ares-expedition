package com.terraforming.ares.services;

import com.terraforming.ares.EngineTestBase;
import com.terraforming.ares.model.Card;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Why this test exists: the flat id -> card table replaced an eight-map lookup that ran millions of times per simulated game. A table that silently
 * loses an id would not crash - it would make a card invisible to the observation (a zero bit) and quietly weaken the player, which is the failure
 * class this project keeps paying for. So the fast path is compared against the original lookup over the whole id range.
 */
class CardServiceTest extends EngineTestBase {

    @Autowired
    CardService cardService;

    @Test
    void theFlatTableAgreesWithTheOriginalLookup() {
        for (int id = 0; id <= 11_000; id++) {
            Card fast = cardService.getCard(id);
            Card slow = cardService.slowGetCard(id);
            if (slow == null) {
                assertNull(fast, "the flat table invented a card for id " + id);
            } else {
                assertSame(slow, fast, "the flat table disagrees with the original lookup at id " + id);
            }
        }
    }

    @Test
    void knownCardsAreFoundAndUnknownOnesAreNull() {
        assertNotNull(cardService.getCard(1), "project card 1 must exist");
        assertNotNull(cardService.getCard(10001), "corporation 10001 must exist");
        assertNull(cardService.getCard(999_999), "an id outside the table must stay null");
    }

    @Test
    void repeatedLookupsReturnTheSameInstance() {
        assertSame(cardService.getCard(1), cardService.getCard(1));
    }

    @Test
    void everyProjectAndCorporationIdResolves() {
        int found = 0;
        for (int id = 1; id <= 11_000; id++) {
            Card card = cardService.getCard(id);
            if (card != null) {
                found++;
                assertEquals(card.getClass(), cardService.slowGetCard(id).getClass());
            }
        }
        // the playable pool is a few hundred project cards plus the corporations; this is a floor, not a norm
        org.junit.jupiter.api.Assertions.assertTrue(found > 300, "only " + found + " cards resolved");
    }
}
