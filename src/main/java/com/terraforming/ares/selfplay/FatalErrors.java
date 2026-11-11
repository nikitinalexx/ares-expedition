package com.terraforming.ares.selfplay;

/**
 * Out of memory in the headless self-play process: the JVM state is unreliable afterwards (threads die half way, queues
 * stop draining), so the process ends at once with {@link #EXIT_OUT_OF_MEMORY} and the night pipeline starts it again
 * after the finished chunks. Only active when {@code ares.haltOnOutOfMemory=true} (set by {@link SelfPlayMain}); the web
 * application is never halted.
 */
public final class FatalErrors {
    public static final int EXIT_OUT_OF_MEMORY = 6;
    public static final String PROPERTY = "ares.haltOnOutOfMemory";

    private FatalErrors() {
    }

    public static void haltIfOutOfMemory(Throwable t) {
        if (!Boolean.getBoolean(PROPERTY)) {
            return;
        }
        for (Throwable c = t; c != null; c = c.getCause() == c ? null : c.getCause()) {
            if (c instanceof OutOfMemoryError) {
                try {
                    System.err.println("OUT OF MEMORY: the self-play JVM exits with code " + EXIT_OUT_OF_MEMORY
                            + " so the pipeline can restart it after the finished chunks");
                    System.err.flush();
                    System.out.flush();
                } finally {
                    Runtime.getRuntime().halt(EXIT_OUT_OF_MEMORY);
                }
            }
        }
    }
}
