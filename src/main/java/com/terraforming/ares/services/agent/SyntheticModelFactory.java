package com.terraforming.ares.services.agent;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Random;

/**
 * Writes models with the <b>current</b> encoder schema and random weights, one per decision type, in the exact format of
 * {@code ml/phase_model.py} ({@code kind=phase_mlp}, v1). Purpose: measure and exercise the real inference path when no
 * trained model is at hand — the forward-pass cost, the memory/GC behaviour, the whole game with a neural policy.
 * <p>
 * The weights are noise, so the choices are meaningless: this is an instrument, never a player. The schema hash is
 * computed from {@link DecisionType} itself, so {@link NeuralPolicy} accepts the models (a model trained on an older
 * encoder is still refused — correctly).
 */
public final class SyntheticModelFactory {
    private static final int KIND_VERSION = 1;
    private static final float CLIP = 5f;

    private SyntheticModelFactory() {
    }

    /** Model files of every decision type, in a directory per type. */
    public static Map<DecisionType, Path> writeAll(Path root, int h1, int h2, long seed) throws IOException {
        Map<DecisionType, Path> result = new EnumMap<>(DecisionType.class);
        long s = seed;
        for (DecisionType type : DecisionType.values()) {
            Path dir = root.resolve(type.name());
            result.put(type, write(type, dir, h1, h2, s++));
        }
        return result;
    }

    /** @return the directory with {@code model.json} and {@code weights.bin} */
    public static Path write(DecisionType type, Path dir, int h1, int h2, long seed) throws IOException {
        DecisionFile.Schema schema = NeuralPolicy.schema(type);
        int obs = schema.obsSize();
        int cand = schema.candidateFeatures();
        Random random = new Random(seed);

        ByteBuffer weights = ByteBuffer.allocate(4 * (2 * obs + h1 * obs + h1 + h1 * cand + h2 * h1 + 2 * h2 + 1))
                .order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < obs; i++) {
            weights.putFloat(0.2f + 0.1f * (float) random.nextGaussian());                 // mean
        }
        for (int i = 0; i < obs; i++) {
            weights.putFloat(0.5f + 0.3f * Math.abs((float) random.nextGaussian()));       // std (> 0)
        }
        gaussian(weights, h1 * obs, 0.3 / Math.sqrt(obs), random);
        gaussian(weights, h1, 0.1, random);
        gaussian(weights, h1 * cand, 0.8 / Math.sqrt(cand), random);
        gaussian(weights, h2 * h1, 0.5 / Math.sqrt(h1), random);
        gaussian(weights, h2, 0.1, random);
        gaussian(weights, h2, 0.8, random);
        weights.putFloat(0.1f);
        byte[] raw = weights.array();

        Files.createDirectories(dir);
        Files.write(dir.resolve("weights.bin"), raw);
        String json = "{"
                + "\"kind\":\"phase_mlp\",\"version\":" + KIND_VERSION
                + ",\"decisionType\":\"" + type + "\""
                + ",\"schemaHash\":\"" + schema.hash() + "\""
                + ",\"obsSize\":" + obs
                + ",\"candFeatures\":" + cand
                + ",\"hidden\":[" + h1 + "," + h2 + "]"
                + ",\"clip\":" + CLIP
                + ",\"weightsCount\":" + (raw.length / 4)
                + ",\"weightsSha256\":\"" + sha256(raw) + "\""
                + "}";
        Files.writeString(dir.resolve("model.json"), json, StandardCharsets.UTF_8);
        return dir;
    }

    private static void gaussian(ByteBuffer sink, int count, double scale, Random random) {
        for (int i = 0; i < count; i++) {
            sink.putFloat((float) (scale * random.nextGaussian()));
        }
    }

    private static String sha256(byte[] raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
