package com.terraforming.ares.services.agent;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Binary file with the decisions of ONE decision type (little endian, fixed size records).
 * <pre>
 *   header:  "ARES" | u16 version | u32 schemaLength | schema (UTF-8 JSON)
 *   record:  float16 observation[obsSize] | u8 candidateCount | u16 candidateIds[maxCandidates]
 *            | float16 candidateFeatures[maxCandidates * candidateFeatures] | u8 chosen | u8 seat | u8 context | u16 round
 *            | float32 outcome | float32 margin | u64 gameId | u16 step
 * </pre>
 * Version history:
 * <ul>
 *   <li>2 — candidate ids as u8, candidate features as float32</li>
 *   <li>3 — candidate ids as u16 (card ids), features still float32</li>
 *   <li>4 — candidate features as float16 (same range as observations; halves the dominant part of each row)</li>
 * </ul>
 * Readers accept 2–4. Writers always emit the current {@link #VERSION}. The chosen index stays a byte
 * (at most 255 candidates). {@code context}: who played everything the agent does not decide (0 SMART, 1 RANDOM).
 * Observation and candidate features are float16 (normalized ~[0, 2]); values that do not fit are rejected.
 * Candidates are zero-padded to {@code maxCandidates}. Schema hash covers the observation layout so a trainer
 * can refuse files from a different encoder.
 */
public final class DecisionFile {
    public static final int VERSION = 4;
    public static final int VERSION_CAND_F16 = 4;
    public static final int VERSION_IDS_U16 = 3;
    private static final int LEGACY_VERSION = 2;
    private static final byte[] MAGIC = "ARES".getBytes(StandardCharsets.US_ASCII);

    /**
     * Describes the content of a file.
     *
     * @param observationLayout human readable description of the observation, part of the schema hash
     */
    public record Schema(DecisionType type, int obsSize, int maxCandidates, int candidateFeatures, String observationLayout) {
        public String hash() {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                String text = type + "|" + obsSize + "|" + maxCandidates + "|" + candidateFeatures + "|" + observationLayout;
                return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8))).substring(0, 16);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        public String toJson() {
            return "{\"type\":\"" + type + "\",\"obsSize\":" + obsSize + ",\"maxCandidates\":" + maxCandidates
                    + ",\"candidateFeatures\":" + candidateFeatures + ",\"obsDtype\":\"f2\",\"candDtype\":\"f2\",\"hash\":\"" + hash()
                    + "\",\"observationLayout\":\"" + observationLayout.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
        }

        public int recordSize() {
            return bytesPerRecord(this, VERSION);
        }
    }

    public static int bytesPerRecord(Schema schema, int version) {
        int idBytes = version >= VERSION_IDS_U16 ? 2 : 1;
        int featBytes = version >= VERSION_CAND_F16 ? 2 : 4;
        return 2 * schema.obsSize() + 1 + idBytes * schema.maxCandidates()
                + featBytes * schema.maxCandidates() * schema.candidateFeatures()
                + 1 + 1 + 1 + 2 + 4 + 4 + 8 + 2;
    }

    /**
     * One decision with its final result.
     */
    public record Row(float[] observation, int candidateCount, int[] candidateIds, float[][] candidateFeatures, int chosen,
                      int seat, int context, int round, float outcome, float margin, long gameId, int step) {
    }

    private DecisionFile() {
    }

    public static final class Writer implements Closeable {
        private final Schema schema;
        private final OutputStream out;

        public Writer(Path path, Schema schema) throws IOException {
            if (schema.maxCandidates() > 255) {
                throw new IllegalArgumentException("The chosen index is one byte, maxCandidates is " + schema.maxCandidates());
            }
            this.schema = schema;
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            this.out = new BufferedOutputStream(Files.newOutputStream(path), 1 << 20);

            byte[] json = schema.toJson().getBytes(StandardCharsets.UTF_8);
            ByteBuffer header = ByteBuffer.allocate(4 + 2 + 4 + json.length).order(ByteOrder.LITTLE_ENDIAN);
            header.put(MAGIC).putShort((short) VERSION).putInt(json.length).put(json);
            out.write(header.array());
        }

        public synchronized void write(Row row) throws IOException {
            writeEncoded(encodeRow(schema, row));
        }

        /** Already packed by {@link DecisionFile#encodeRow} — the writer thread only pushes bytes to disk. */
        public synchronized void writeEncoded(byte[] encoded) throws IOException {
            out.write(encoded);
        }

        @Override
        public synchronized void close() throws IOException {
            out.close();
        }
    }

    public record Content(Schema schema, String schemaJson, List<Row> rows) {
    }

    public static Content read(Path path) throws IOException {
        try (InputStream in = new BufferedInputStream(Files.newInputStream(path), 1 << 20)) {
            DataInputStream data = new DataInputStream(in);
            byte[] magic = data.readNBytes(4);
            if (!java.util.Arrays.equals(magic, MAGIC)) {
                throw new IOException("Not an ARES decision file: " + path);
            }
            ByteBuffer small = ByteBuffer.wrap(data.readNBytes(6)).order(ByteOrder.LITTLE_ENDIAN);
            int version = small.getShort();
            int jsonLength = small.getInt();
            if (version < LEGACY_VERSION || version > VERSION) {
                throw new IOException("Unsupported version " + version);
            }
            String json = new String(data.readNBytes(jsonLength), StandardCharsets.UTF_8);
            Schema schema = parseSchema(json);

            List<Row> rows = new ArrayList<>();
            byte[] raw = new byte[bytesPerRecord(schema, version)];
            while (true) {
                int read = data.readNBytes(raw, 0, raw.length);
                if (read == 0) {
                    break;
                }
                if (read != raw.length) {
                    throw new IOException("Truncated record: " + read + " of " + raw.length + " bytes");
                }
                rows.add(parseRow(ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN), schema, version));
            }
            return new Content(schema, json, rows);
        }
    }

    /**
     * Packs one row on the caller thread (virtual threads in self-play). The dedicated writer only appends the bytes.
     */
    public static byte[] encodeRow(Schema schema, Row row) {
        if (row.observation().length != schema.obsSize()) {
            throw new IllegalArgumentException("Observation has " + row.observation().length + " values, schema says " + schema.obsSize());
        }
        if (row.candidateCount() < 1 || row.candidateCount() > schema.maxCandidates()) {
            throw new IllegalArgumentException("Candidate count " + row.candidateCount() + " is out of 1.." + schema.maxCandidates());
        }
        if (row.chosen() < 0 || row.chosen() >= row.candidateCount()) {
            throw new IllegalArgumentException("Chosen " + row.chosen() + " is not one of " + row.candidateCount() + " candidates");
        }
        ByteBuffer packed = ByteBuffer.allocate(schema.recordSize()).order(ByteOrder.LITTLE_ENDIAN);
        for (float v : row.observation()) {
            packed.putShort(toHalf(v, "observation"));
        }
        packed.put((byte) row.candidateCount());
        for (int i = 0; i < schema.maxCandidates(); i++) {
            int id = i < row.candidateCount() ? row.candidateIds()[i] : 0;
            if (id < 0 || id > 0xFFFF) {
                throw new IllegalArgumentException("Candidate id " + id + " does not fit into 16 bits");
            }
            packed.putShort((short) id);
        }
        for (int i = 0; i < schema.maxCandidates(); i++) {
            for (int f = 0; f < schema.candidateFeatures(); f++) {
                float v = i < row.candidateCount() ? row.candidateFeatures()[i][f] : 0f;
                packed.putShort(toHalf(v, "candidate feature"));
            }
        }
        packed.put((byte) row.chosen()).put((byte) row.seat()).put((byte) row.context()).putShort((short) row.round());
        packed.putFloat(row.outcome()).putFloat(row.margin());
        packed.putLong(row.gameId()).putShort((short) row.step());
        byte[] bytes = new byte[packed.position()];
        packed.flip();
        packed.get(bytes);
        return bytes;
    }

    /**
     * float16 can hold up to 65504; normalized features are around [0, 2], anything huge or not finite is a bug in the encoder.
     */
    private static short toHalf(float v, String what) {
        if (!Float.isFinite(v) || Math.abs(v) > 60000f) {
            throw new IllegalArgumentException(what + " value " + v + " can not be stored as float16");
        }
        return Float.floatToFloat16(v);
    }

    private static Row parseRow(ByteBuffer b, Schema schema, int version) {
        float[] observation = new float[schema.obsSize()];
        for (int i = 0; i < observation.length; i++) {
            observation[i] = Float.float16ToFloat(b.getShort());
        }
        int count = b.get() & 0xFF;
        int[] ids = new int[schema.maxCandidates()];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = version >= VERSION_IDS_U16 ? (b.getShort() & 0xFFFF) : (b.get() & 0xFF);
        }
        float[][] features = new float[schema.maxCandidates()][schema.candidateFeatures()];
        for (int i = 0; i < features.length; i++) {
            for (int f = 0; f < schema.candidateFeatures(); f++) {
                features[i][f] = version >= VERSION_CAND_F16 ? Float.float16ToFloat(b.getShort()) : b.getFloat();
            }
        }
        int chosen = b.get() & 0xFF;
        int seat = b.get() & 0xFF;
        int context = b.get() & 0xFF;
        int round = b.getShort() & 0xFFFF;
        float outcome = b.getFloat();
        float margin = b.getFloat();
        long gameId = b.getLong();
        int step = b.getShort() & 0xFFFF;
        return new Row(observation, count, ids, features, chosen, seat, context, round, outcome, margin, gameId, step);
    }

    private static Schema parseSchema(String json) {
        return new Schema(
                DecisionType.valueOf(string(json, "type")),
                (int) number(json, "obsSize"),
                (int) number(json, "maxCandidates"),
                (int) number(json, "candidateFeatures"),
                string(json, "observationLayout").replace("\\\"", "\"").replace("\\\\", "\\"));
    }

    private static String string(String json, String key) {
        String marker = "\"" + key + "\":\"";
        int start = json.indexOf(marker) + marker.length();
        StringBuilder value = new StringBuilder();
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\') {
                value.append(c).append(json.charAt(++i));
            } else if (c == '"') {
                break;
            } else {
                value.append(c);
            }
        }
        return value.toString();
    }

    private static double number(String json, String key) {
        String marker = "\"" + key + "\":";
        int start = json.indexOf(marker) + marker.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.')) {
            end++;
        }
        return Double.parseDouble(json.substring(start, end));
    }
}
