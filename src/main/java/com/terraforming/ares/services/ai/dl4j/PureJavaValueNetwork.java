package com.terraforming.ares.services.ai.dl4j;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.stream.IntStream;

/**
 * Dependency-free (no ND4J / DL4J / CUDA) forward pass of the value network trained in the
 * {@code deep} project (DL4J ComputationGraph saved with ModelSerializer).
 * <p>
 * Architecture (see configuration.json inside the zip):
 * <pre>
 *   tableInput(T) -> tableDense(T->H1, LeakyReLU 0.01) \
 *                                                       merge(concat) -> hidden(H1+H2->H3, LeakyReLU) -> out(H3->1, sigmoid)
 *   handInput(H)  -> handDense(H->H2, LeakyReLU 0.01)  /
 * </pre>
 * Dropout in {@code hidden} is a no-op at inference time.
 * <p>
 * Weights are read straight from {@code coefficients.bin} (ND4J binary format: shape buffer + float data buffer,
 * big-endian). DL4J stores the flattened parameters as [W(f-order, nIn x nOut), b(nOut)] per layer in topological
 * order: tableDense, handDense, hidden, out. In f-order the weights of one output neuron are contiguous, which is
 * transposed here to input-major so the inner loop (axpy over outputs) is auto-vectorizable and zero inputs can be skipped.
 * <p>
 * Thread-safe: instances are immutable after construction.
 */
public final class PureJavaValueNetwork implements ValueNetwork {
    private static final float LEAKY_ALPHA = 0.01f;
    /**
     * The trained weights contain ~4% subnormal floats (and ~70% below 1e-6, an effect of L2 regularisation).
     * Arithmetic on subnormals is 20-50x slower on x86, so such weights are flushed to zero on load.
     * Effect on the output is below 1e-6 (verified against DL4J).
     */
    private static final float WEIGHT_FLUSH_THRESHOLD = 1e-8f;

    private final int tableIn;
    private final int handIn;
    private final int h1;
    private final int h2;
    private final int h3;

    // input-major: w[i * nOut + j]
    private final float[] wTable, bTable;
    private final float[] wHand, bHand;
    private final float[] wHidden, bHidden;
    private final float[] wOut;
    private final float bOut;

    private PureJavaValueNetwork(int tableIn, int handIn, int h1, int h2, int h3, float[] params) {
        this.tableIn = tableIn;
        this.handIn = handIn;
        this.h1 = h1;
        this.h2 = h2;
        this.h3 = h3;

        int pos = 0;
        wTable = transpose(params, pos, tableIn, h1);
        pos += tableIn * h1;
        bTable = copy(params, pos, h1);
        pos += h1;

        wHand = transpose(params, pos, handIn, h2);
        pos += handIn * h2;
        bHand = copy(params, pos, h2);
        pos += h2;

        wHidden = transpose(params, pos, h1 + h2, h3);
        pos += (h1 + h2) * h3;
        bHidden = copy(params, pos, h3);
        pos += h3;

        wOut = copy(params, pos, h3); // nOut == 1, so f-order == plain vector
        pos += h3;
        bOut = params[pos++];

        if (pos != params.length) {
            throw new IllegalStateException("Unexpected parameter count: used " + pos + " of " + params.length);
        }
    }

    /**
     * Loads a model zip saved by DL4J ModelSerializer. Layer sizes are passed explicitly and validated against the
     * parameter count, so a model with a different architecture fails fast instead of silently producing garbage.
     */
    public static PureJavaValueNetwork load(File zip, int tableIn, int handIn, int h1, int h2, int h3) throws IOException {
        try (ZipFile zf = new ZipFile(zip)) {
            ZipEntry entry = zf.getEntry("coefficients.bin");
            if (entry == null) {
                throw new IOException("coefficients.bin not found in " + zip);
            }
            try (InputStream in = zf.getInputStream(entry);
                 DataInputStream dis = new DataInputStream(new BufferedInputStream(in, 1 << 16))) {
                skipBuffer(dis); // shape info
                float[] params = readFloatBuffer(dis);

                long expected = (long) tableIn * h1 + h1
                        + (long) handIn * h2 + h2
                        + (long) (h1 + h2) * h3 + h3
                        + h3 + 1;
                if (params.length != expected) {
                    throw new IOException("Model " + zip + " has " + params.length + " params, expected " + expected
                            + " for architecture table=" + tableIn + " hand=" + handIn + " dense=" + h1 + "/" + h2 + "/" + h3);
                }
                return new PureJavaValueNetwork(tableIn, handIn, h1, h2, h3, params);
            }
        }
    }

    // ND4J DataBuffer.write: UTF(allocationMode), long length, UTF(dataType), then `length` elements
    private static void skipBuffer(DataInputStream dis) throws IOException {
        dis.readUTF();
        long length = dis.readLong();
        String type = dis.readUTF();
        int elemSize = switch (type) {
            case "LONG", "DOUBLE" -> 8;
            case "INT", "FLOAT" -> 4;
            default -> throw new IOException("Unsupported shape buffer type " + type);
        };
        long toSkip = length * elemSize;
        while (toSkip > 0) {
            long skipped = dis.skip(toSkip);
            if (skipped <= 0) throw new IOException("Unexpected end of shape buffer");
            toSkip -= skipped;
        }
    }

    private static float[] readFloatBuffer(DataInputStream dis) throws IOException {
        dis.readUTF();
        long length = dis.readLong();
        String type = dis.readUTF();
        if (!"FLOAT".equals(type)) {
            throw new IOException("Only FLOAT parameters are supported, got " + type);
        }
        float[] data = new float[Math.toIntExact(length)];
        for (int i = 0; i < data.length; i++) {
            data[i] = dis.readFloat();
        }
        return data;
    }

    // f-order [nIn x nOut]: element (i, j) at j * nIn + i  ->  input-major: i * nOut + j
    private static float[] transpose(float[] src, int offset, int nIn, int nOut) {
        float[] dst = new float[nIn * nOut];
        for (int j = 0; j < nOut; j++) {
            int base = offset + j * nIn;
            for (int i = 0; i < nIn; i++) {
                dst[i * nOut + j] = flush(src[base + i]);
            }
        }
        return dst;
    }

    private static float[] copy(float[] src, int offset, int len) {
        float[] dst = new float[len];
        for (int i = 0; i < len; i++) {
            dst[i] = flush(src[offset + i]);
        }
        return dst;
    }

    private static float flush(float v) {
        return Math.abs(v) < WEIGHT_FLUSH_THRESHOLD ? 0f : v;
    }

    public int inputSize() {
        return tableIn + handIn;
    }

    /**
     * @param features concatenated [table | hand] vector, length {@code tableIn + handIn}
     * @return sigmoid output (win probability)
     */
    public float predict(float[] features) {
        float[] merged = new float[h1 + h2];
        denseLeaky(features, 0, tableIn, wTable, bTable, h1, merged, 0);
        denseLeaky(features, tableIn, handIn, wHand, bHand, h2, merged, h1);

        float[] hidden = new float[h3];
        denseLeaky(merged, 0, h1 + h2, wHidden, bHidden, h3, hidden, 0);

        float z = bOut;
        for (int i = 0; i < h3; i++) {
            z += hidden[i] * wOut[i];
        }
        return (float) (1.0 / (1.0 + Math.exp(-z)));
    }

    /**
     * Batch prediction, parallelised across states (each state is independent).
     */
    @Override
    public float[] predictBatch(float[][] features) {
        float[] result = new float[features.length];
        if (features.length < 4) {
            for (int i = 0; i < features.length; i++) {
                result[i] = predict(features[i]);
            }
        } else {
            IntStream.range(0, features.length).parallel().forEach(i -> result[i] = predict(features[i]));
        }
        return result;
    }

    private static void denseLeaky(float[] in, int inOffset, int nIn, float[] w, float[] b, int nOut, float[] out, int outOffset) {
        float[] acc = new float[nOut];
        System.arraycopy(b, 0, acc, 0, nOut);
        for (int i = 0; i < nIn; i++) {
            float x = in[inOffset + i];
            if (x == 0f) continue; // table/hand vectors are mostly sparse
            int row = i * nOut;
            for (int j = 0; j < nOut; j++) {
                acc[j] += x * w[row + j];
            }
        }
        for (int j = 0; j < nOut; j++) {
            float v = acc[j];
            v = v >= 0f ? v : v * LEAKY_ALPHA;
            out[outOffset + j] = Math.abs(v) < Float.MIN_NORMAL ? 0f : v; // keep activations out of subnormal range
        }
    }
}
