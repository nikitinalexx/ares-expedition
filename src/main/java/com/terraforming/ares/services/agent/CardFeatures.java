package com.terraforming.ares.services.agent;

import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.Tag;

/**
 * Shared description of a card as a candidate: class signature, colour, tags, project price, blue action, stop flag.
 * Mulligan and the card-set decisions use the same prefix so a card looks the same wherever it is offered.
 */
public final class CardFeatures {
    public static final int STOP_ID = 0xFFFF;
    public static final int SIGNATURE = CardSignature.BITS;
    public static final int COLORS = 4;
    public static final int TAGS = 11;
    /**
     * signature, colour, tags, price/50, active, stop, printed points/10.
     * <p>
     * The points slot was DOCUMENTED in every decision's layout string from the start and never written: the card menus
     * described a card by its identity, colour, tags, price and "is a blue action", but not by what it scores. "Which card
     * to keep in the draft" and "which card to build" were blind to the only property that pays directly (found in the
     * per-decision revision, 2026-11-15).
     */
    public static final int LENGTH = SIGNATURE + COLORS + TAGS + 2 + 1 + 1;

    static {
        if (CardColor.values().length != COLORS) {
            throw new ExceptionInInitializerError("CardColor changed: update the card candidate layout");
        }
        if (Tag.values().length != TAGS) {
            throw new ExceptionInInitializerError("Tag changed: update the card candidate layout");
        }
    }

    private CardFeatures() {
    }

    /**
     * @param stop the "take none of these" candidate; {@code card} is null in that case and every slot except the
     *             stop flag is zero
     */
    public static float[] of(Card card, boolean stop) {
        float[] features = new float[LENGTH];
        int pos = 0;
        if (!stop) {
            float[] signature = CardSignature.of(card.getClass());
            System.arraycopy(signature, 0, features, pos, SIGNATURE);
        }
        pos += SIGNATURE;
        if (!stop) {
            features[pos + card.getColor().ordinal()] = 1f;
        }
        pos += COLORS;
        if (!stop) {
            for (Tag tag : card.getTags()) {
                features[pos + tag.ordinal()] = 1f;
            }
        }
        pos += TAGS;
        if (!stop) {
            features[pos] = card.getPrice() / 50f;
            features[pos + 1] = card.isActiveCard() ? 1f : 0f;
        }
        pos += 2;
        features[pos] = stop ? 1f : 0f;
        pos += 1;
        // LAST, deliberately: candidate features are only ever APPENDED, so a converted model can be given a zero weight
        // for the new slot (ml/remap_model.py --pad-candidates). Inserting it before `stop` shifted stop's weight into the
        // points slot and cost 3.8 points of win rate in the parity check - measured, 2026-11-15.
        if (!stop) {
            features[pos] = card.getWinningPoints() / 10f;
        }
        return features;
    }
}
