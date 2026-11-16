package com.terraforming.ares.services.agent;

import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.Tag;

/**
 * Shared description of a card as a candidate: one-hot identity plus the stop flag.
 * Mulligan and the card-set decisions use the same prefix so a card looks the same wherever it is offered.
 */
public final class CardFeatures {
    public static final int STOP_ID = 0xFFFF;
    /**
     * Identity is a ONE-HOT over the playable cards, one bit per card class - not a hash. A hash of the class name (+1/-1
     * per bit) can only be learned by summing 64 arbitrary weights into one effect, which is slow and noisy at our data
     * volume; one bit per card lets the first layer read "this card" directly, exactly as the observation already does for
     * the table and the hand (owner, 2026-11-16: "no hashes anywhere, one bit per card is many times easier to learn").
     */
    public static final int IDENTITY = PlayableCards.ALL.size();
    /** Kept as an alias so the older code that speaks of a "signature" still compiles; it is the one-hot width now. */
    public static final int SIGNATURE = IDENTITY;
    private static final java.util.Map<Class<?>, Integer> CARD_INDEX = index();
    public static final int COLORS = 4;
    public static final int TAGS = 11;
    /**
     * Identity (one-hot over the playable cards) and the stop flag. Colour, tags, price, "is a blue action" and the
     * printed points are NOT written: with one bit per card they are a fixed function of the identity, so the first layer
     * learns them from that bit directly (owner, 2026-11-16) - writing them as well was extra parameters and extra noise
     * on every candidate.
     */
    public static final int LENGTH = IDENTITY + 1;

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

    private static java.util.Map<Class<?>, Integer> index() {
        java.util.Map<Class<?>, Integer> map = new java.util.HashMap<>();
        for (int i = 0; i < PlayableCards.ALL.size(); i++) {
            map.put(PlayableCards.ALL.get(i), i);
        }
        return map;
    }

    /**
     * @param stop the "take none of these" candidate; {@code card} is null in that case and every slot except the
     *             stop flag is zero
     */
    public static float[] of(Card card, boolean stop) {
        float[] features = new float[LENGTH];
        if (stop) {
            features[IDENTITY] = 1f;
            return features;
        }
        Integer index = CARD_INDEX.get(card.getClass());
        if (index != null) {
            features[index] = 1f;
        }
        return features;
    }

}
