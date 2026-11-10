package com.terraforming.ares.services.agent;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stable identity of a card class for candidate features: 64 bits of SHA-256(class name), each bit written as +1 or -1.
 * A new card gets its own signature and does not move the signatures of the cards that already exist (a one-hot would).
 * Two classes must never share a signature; {@code OpeningDecisionsTest} checks that on the whole catalog.
 */
public final class CardSignature {
    public static final int BITS = 64;

    private static final ConcurrentHashMap<Class<?>, float[]> CACHE = new ConcurrentHashMap<>();

    private CardSignature() {
    }

    public static float[] of(Class<?> type) {
        return CACHE.computeIfAbsent(type, CardSignature::compute).clone();
    }

    private static float[] compute(Class<?> type) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(type.getName().getBytes(StandardCharsets.UTF_8));
            float[] bits = new float[BITS];
            for (int i = 0; i < BITS; i++) {
                int bit = (hash[i >>> 3] >> (i & 7)) & 1;
                bits[i] = bit == 1 ? 1f : -1f;
            }
            return bits;
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
