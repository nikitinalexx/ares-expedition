package com.terraforming.ares.services.ai.network.threads;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class CustomNetworkThreadPool {
    private static CustomNetworkThreadPool instance;
    private ExecutorService es;
    private final int threadCount = this.getDefaultThreadsCount();
    private List<Future<?>> results;

    private CustomNetworkThreadPool() {
        this.es = Executors.newFixedThreadPool(this.threadCount);
    }

    public static CustomNetworkThreadPool getInstance() {
        if (instance == null) {
            instance = new CustomNetworkThreadPool();
        }

        if (instance.es.isShutdown() || instance.es.isTerminated()) {
            instance.es = Executors.newFixedThreadPool(instance.threadCount);
        }

        return instance;
    }

    public void run(Collection<Callable<Void>> tasks) throws InterruptedException {
        this.es.invokeAll(tasks);
    }

    public Future<?> submit(Callable<?> task) {
        return this.es.submit(task);
    }

    public void run(Runnable task) {
        this.es.submit(task);
    }

    public void shutdown() {
        this.es.shutdown();
    }

    public void shutdownNow() {
        this.es.shutdownNow();
    }

    public final int getThreadCount() {
        return this.threadCount;
    }

    private int getDefaultThreadsCount() {
        int threads = Runtime.getRuntime().availableProcessors();
        threads /= 2;
        --threads;
        if (threads < 1) {
            threads = 1;
        }

        return threads;
    }

    public static int[] calculateCellsPerThread(int width, int threadCount) {
        int[] threads = new int[threadCount];
        int cpt = width / threadCount;

        int rest;
        for(rest = 0; rest < threadCount; ++rest) {
            threads[rest] = cpt;
        }

        if (width % threadCount != 0) {
            rest = width % threadCount;

            for(int i = 0; i < rest; ++i) {
                int var10002 = threads[i]++;
            }
        }

        return threads;
    }
}
