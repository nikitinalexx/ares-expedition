package com.terraforming.ares.services.ai.dl4j.reference;

import com.terraforming.ares.services.ai.dl4j.ValueNetwork;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Reference implementation on the original DL4J ComputationGraph (CUDA or CPU backend, whichever is on the classpath).
 * Compiled only with the {@code dl4j-reference} Maven profile; used to verify / benchmark {@code PureJavaValueNetwork}.
 * Instantiated by reflection from NNService.
 * <p>
 * Same throughput trick as the original GPU service: requests of many concurrently running games are coalesced by one
 * batcher thread into big batches (up to {@link #MAX_STATES_PER_BATCH} states) - one GPU call instead of hundreds.
 */
public class Dl4jValueNetwork implements ValueNetwork {
    private static final int MAX_STATES_PER_BATCH = 16384;
    private static final int CHUNK = 8192;

    private record Request(float[][] features, CompletableFuture<float[]> future) {
    }

    private final ComputationGraph net;
    private final int tableSize;
    private final int handSize;
    private final LinkedBlockingQueue<Request> queue = new LinkedBlockingQueue<>();

    public Dl4jValueNetwork(File zip, int tableSize, int handSize) throws IOException {
        this.net = ComputationGraph.load(zip, false);
        this.tableSize = tableSize;
        this.handSize = handSize;
        System.out.println("DL4J reference network loaded, backend=" + Nd4j.getBackend().getClass().getSimpleName());

        Thread batcher = new Thread(this::batchLoop, "dl4j-reference-batcher");
        batcher.setDaemon(true);
        batcher.start();
    }

    @Override
    public float[] predictBatch(float[][] features) {
        if (features.length <= CHUNK) {
            CompletableFuture<float[]> future = new CompletableFuture<>();
            queue.add(new Request(features, future));
            return future.join();
        }
        List<CompletableFuture<float[]>> futures = new ArrayList<>();
        for (int from = 0; from < features.length; from += CHUNK) {
            float[][] chunk = java.util.Arrays.copyOfRange(features, from, Math.min(from + CHUNK, features.length));
            CompletableFuture<float[]> future = new CompletableFuture<>();
            queue.add(new Request(chunk, future));
            futures.add(future);
        }
        float[] result = new float[features.length];
        int pos = 0;
        for (CompletableFuture<float[]> f : futures) {
            float[] part = f.join();
            System.arraycopy(part, 0, result, pos, part.length);
            pos += part.length;
        }
        return result;
    }

    private void batchLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            List<Request> batch = new ArrayList<>();
            try {
                batch.add(queue.take());
                int states = batch.get(0).features.length;
                Request next;
                while (states < MAX_STATES_PER_BATCH && (next = queue.poll()) != null) {
                    batch.add(next);
                    states += next.features.length;
                }
                process(batch, states);
            } catch (InterruptedException e) {
                return;
            } catch (Throwable t) {
                t.printStackTrace();
                batch.forEach(r -> r.future.completeExceptionally(t));
            }
        }
    }

    private void process(List<Request> batch, int totalStates) {
        float[] table = new float[totalStates * tableSize];
        float[] hand = new float[totalStates * handSize];
        int row = 0;
        for (Request r : batch) {
            for (float[] f : r.features) {
                System.arraycopy(f, 0, table, row * tableSize, tableSize);
                System.arraycopy(f, tableSize, hand, row * handSize, handSize);
                row++;
            }
        }
        try (INDArray t = Nd4j.create(table, new int[]{totalStates, tableSize}, 'c');
             INDArray h = Nd4j.create(hand, new int[]{totalStates, handSize}, 'c')) {
            INDArray[] out = net.output(false, t, h);
            try {
                float[] data = out[0].data().asFloat();
                int offset = 0;
                for (Request r : batch) {
                    int n = r.features.length;
                    float[] part = new float[n];
                    System.arraycopy(data, offset, part, 0, n);
                    r.future.complete(part);
                    offset += n;
                }
            } finally {
                for (INDArray o : out) {
                    if (o != null) o.close();
                }
            }
        } catch (Throwable t) {
            Nd4j.getWorkspaceManager().destroyAllWorkspacesForCurrentThread();
            batch.forEach(r -> r.future.completeExceptionally(t));
            t.printStackTrace();
        }
    }
}
