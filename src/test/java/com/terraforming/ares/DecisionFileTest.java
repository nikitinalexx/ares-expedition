package com.terraforming.ares;

import com.terraforming.ares.services.agent.DecisionFile;
import com.terraforming.ares.services.agent.DecisionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DecisionFileTest {
    @TempDir
    Path dir;

    private static DecisionFile.Schema schema(String layout) {
        return new DecisionFile.Schema(DecisionType.PICK_PHASE, 7, 5, 3, layout);
    }

    private static DecisionFile.Row row(int seed, int candidates, int chosen) {
        float[] obs = new float[7];
        for (int i = 0; i < obs.length; i++) {
            obs[i] = seed + i * 0.25f;
        }
        int[] ids = new int[candidates];
        float[][] feat = new float[candidates][3];
        for (int i = 0; i < candidates; i++) {
            ids[i] = i + 1;
            for (int f = 0; f < 3; f++) {
                feat[i][f] = seed * 100 + i * 10 + f;
            }
        }
        return new DecisionFile.Row(obs, candidates, ids, feat, chosen, seed % 2, seed % 2, 20 + seed, seed % 3 / 2f, seed - 5, 1000L + seed, seed);
    }

    @Test
    void whatIsWrittenIsReadBackExactly() throws IOException {
        Path file = dir.resolve("a.ares");
        DecisionFile.Schema schema = schema("layout \"quoted\" \\ with specials");
        try (DecisionFile.Writer writer = new DecisionFile.Writer(file, schema)) {
            writer.write(row(1, 5, 4));
            writer.write(row(2, 4, 0));
            writer.write(row(3, 1, 0));
        }

        DecisionFile.Content content = DecisionFile.read(file);
        assertEquals(schema, content.schema());
        assertEquals(3, content.rows().size());

        DecisionFile.Row second = content.rows().get(1);
        DecisionFile.Row expected = row(2, 4, 0);
        assertArrayEquals(expected.observation(), second.observation());
        assertEquals(4, second.candidateCount());
        assertArrayEquals(new int[]{1, 2, 3, 4, 0}, second.candidateIds(), "padded with zeros");
        assertArrayEquals(expected.candidateFeatures()[3], second.candidateFeatures()[3]);
        assertArrayEquals(new float[3], second.candidateFeatures()[4], "padding is zero");
        assertEquals(expected.outcome(), second.outcome());
        assertEquals(expected.margin(), second.margin());
        assertEquals(expected.gameId(), second.gameId());
        assertEquals(expected.round(), second.round());
        assertEquals(expected.step(), second.step());
        assertEquals(expected.seat(), second.seat());
        assertEquals(expected.context(), second.context());
    }

    @Test
    void recordSizeMatchesWhatIsActuallyWritten() throws IOException {
        Path file = dir.resolve("size.ares");
        DecisionFile.Schema schema = schema("x");
        try (DecisionFile.Writer writer = new DecisionFile.Writer(file, schema)) {
            writer.write(row(1, 5, 0));
            writer.write(row(2, 2, 1));
        }
        long headerSize = 4 + 2 + 4 + schema.toJson().getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        assertEquals(headerSize + 2L * schema.recordSize(), Files.size(file));
    }

    @Test
    void schemaHashDependsOnTheObservationLayout() {
        assertNotEquals(schema("encoder v1").hash(), schema("encoder v2").hash());
        assertEquals(schema("encoder v1").hash(), schema("encoder v1").hash());
        assertNotEquals(schema("a").hash(), new DecisionFile.Schema(DecisionType.PICK_PHASE, 8, 5, 3, "a").hash());
    }

    @Test
    void invalidRowsAreRejected() throws IOException {
        try (DecisionFile.Writer writer = new DecisionFile.Writer(dir.resolve("bad.ares"), schema("x"))) {
            assertThrows(IllegalArgumentException.class, () -> writer.write(row(1, 3, 3)), "chosen out of range");
            assertThrows(IllegalArgumentException.class, () -> writer.write(row(1, 6, 0)), "too many candidates");
            DecisionFile.Row wrongSize = new DecisionFile.Row(new float[6], 1, new int[]{1}, new float[][]{new float[3]}, 0, 0, 0, 0, 0f, 0f, 0, 0);
            assertThrows(IllegalArgumentException.class, () -> writer.write(wrongSize), "observation of the wrong size");
        }
    }

    @Test
    void aTruncatedFileIsDetected() throws IOException {
        Path file = dir.resolve("cut.ares");
        try (DecisionFile.Writer writer = new DecisionFile.Writer(file, schema("x"))) {
            writer.write(row(1, 5, 0));
        }
        byte[] bytes = Files.readAllBytes(file);
        Files.write(file, java.util.Arrays.copyOf(bytes, bytes.length - 3));
        assertThrows(IOException.class, () -> DecisionFile.read(file));
    }
}
