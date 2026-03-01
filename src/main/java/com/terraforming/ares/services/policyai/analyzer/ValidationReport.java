package com.terraforming.ares.services.policyai.analyzer;

import java.util.*;

/**
 * Результат анализа набора {@link com.terraforming.ares.services.policyai.dto.PolicyRecord}.
 *
 * <p>Содержит:
 * <ul>
 *   <li>Максимумы по {@code @DataField} полям — для нормализации</li>
 *   <li>Статус покрытия по {@code @BitMask} полям — для валидации</li>
 * </ul>
 */
public class ValidationReport {

    private final Map<String, Double> dataFieldMaxValues;
    private final Map<String, BitMaskStatus> bitMaskStatuses;
    private final long recordsAnalyzed;

    public ValidationReport(
            Map<String, Double> dataFieldMaxValues,
            Map<String, BitMaskStatus> bitMaskStatuses,
            long recordsAnalyzed
    ) {
        this.dataFieldMaxValues = Collections.unmodifiableMap(dataFieldMaxValues);
        this.bitMaskStatuses = Collections.unmodifiableMap(bitMaskStatuses);
        this.recordsAnalyzed = recordsAnalyzed;
    }

    // -------------------------------------------------------------------------
    // Печать отчёта
    // -------------------------------------------------------------------------

    public void print() {
        System.out.println("========================================");
        System.out.println("  PolicyRecord Validation Report");
        System.out.printf ("  Records analyzed: %,d%n", recordsAnalyzed);
        System.out.println("========================================");

        printDataFields();
        printBitMasks();

        System.out.println("========================================");
    }

    private void printDataFields() {
        System.out.println("\n--- @DataField maximums (use for normalization) ---");

        List<String> keys = new ArrayList<>(dataFieldMaxValues.keySet());
        Collections.sort(keys);

        boolean allOk = true;
        for (String key : keys) {
            double max = dataFieldMaxValues.get(key);
            if (max == 0.0) {
                System.out.printf("  [WARN] %-52s max = 0.0  <-- suspicious!%n", key);
                allOk = false;
            } else {
                System.out.printf("  [OK]   %-52s max = %s%n", key, formatMax(max));
            }
        }
        if (allOk) {
            System.out.println("  All data fields have non-zero maximums.");
        }
    }

    private void printBitMasks() {
        System.out.println("\n--- @BitMask coverage ---");

        List<String> keys = new ArrayList<>(bitMaskStatuses.keySet());
        Collections.sort(keys);

        int okCount = 0;
        for (String key : keys) {
            BitMaskStatus s = bitMaskStatuses.get(key);
            if (s.isOk()) {
                okCount++;
                System.out.printf("  [OK]   %-52s %d element(s), all bits covered%n",
                        key, s.elementCount());
            } else {
                System.out.printf("  [WARN] %-52s%n", key);
                // Выводим детали по каждому элементу, у которого есть проблемы
                for (int i = 0; i < s.elementCount(); i++) {
                    long uncovered = s.uncoveredBitsAt(i);
                    long extra     = s.extraBitsAt(i);
                    if (uncovered != 0)
                        System.out.printf(
                                "           uncovered bits (0x%X) -> [%s]%n",
                                uncovered,
                                bitsToString(uncovered)
                        );

                    if (extra != 0)
                        System.out.printf(
                                "           extra bits     (0x%X) -> [%s]%n",
                                extra,
                                bitsToString(extra)
                        );
                }
            }
        }
        if (okCount == bitMaskStatuses.size()) {
            System.out.println("  All bit masks fully covered, no extra bits.");
        }
    }

    private static String bitsToString(long mask) {
        StringBuilder sb = new StringBuilder();

        for (int bit = 0; bit < Long.SIZE; bit++) {
            if ((mask & (1L << bit)) != 0) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(bit);
            }
        }

        return sb.length() == 0 ? "-" : sb.toString();
    }

    private String formatMax(double max) {
        if (max == Math.floor(max) && max < 1e9) return String.valueOf((long) max);
        return String.format("%.4f", max);
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public Map<String, Double> getDataFieldMaxValues() { return dataFieldMaxValues; }
    public Map<String, BitMaskStatus> getBitMaskStatuses() { return bitMaskStatuses; }
    public long getRecordsAnalyzed() { return recordsAnalyzed; }

    public boolean hasWarnings() {
        boolean dataWarning = dataFieldMaxValues.values().stream().anyMatch(v -> v == 0.0);
        boolean maskWarning = bitMaskStatuses.values().stream().anyMatch(s -> !s.isOk());
        return dataWarning || maskWarning;
    }

    // -------------------------------------------------------------------------
    // Inner: BitMaskStatus
    // -------------------------------------------------------------------------

    /**
     * Результат проверки одного @BitMask поля.
     *
     * <p>Хранит состояние по каждому элементу отдельно:
     * для примитива — один элемент, для массива — по одному на каждый индекс.
     * Это позволяет корректно проверить поля вроде {@code long[] handCardMask = new long[5]}
     * с {@code @BitMask(validBits=64, lastElementBits=12)}, где последний long частичный.
     */
    public static class BitMaskStatus {

        /** Ожидаемые маски для каждого элемента, вычисленные из bitsPerElement. */
        private final long[] expectedMasks;

        /** OR всех встреченных значений по каждому элементу. */
        private final long[] observedBits;

        public BitMaskStatus(int[] bitsPerElement, long[] observedBits) {
            this.expectedMasks = new long[bitsPerElement.length];
            for (int i = 0; i < bitsPerElement.length; i++) {
                int bits = bitsPerElement[i];
                this.expectedMasks[i] = bits >= 64 ? -1L : (1L << bits) - 1L;
            }
            this.observedBits = observedBits.clone();
        }

        public int elementCount() {
            return expectedMasks.length;
        }

        /** Биты, которые ожидались в элементе i, но ни разу не встретились. */
        public long uncoveredBitsAt(int i) {
            return expectedMasks[i] & ~observedBits[i];
        }

        /** Биты, которые встретились в элементе i, но выходят за пределы validBits. */
        public long extraBitsAt(int i) {
            return observedBits[i] & ~expectedMasks[i];
        }

        public boolean isOk() {
            for (int i = 0; i < expectedMasks.length; i++) {
                if (uncoveredBitsAt(i) != 0 || extraBitsAt(i) != 0) return false;
            }
            return true;
        }
    }
}