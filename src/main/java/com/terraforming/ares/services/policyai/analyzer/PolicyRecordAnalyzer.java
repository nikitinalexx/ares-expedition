package com.terraforming.ares.services.policyai.analyzer;

import com.terraforming.ares.services.policyai.annotations.BitMask;
import com.terraforming.ares.services.policyai.annotations.DataField;
import com.terraforming.ares.services.policyai.annotations.Feature;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Сервис анализа набора {@link PolicyRecord} после симуляций.
 *
 * <h3>Что делает:</h3>
 * <ul>
 *   <li>По {@code @DataField} полям — отслеживает максимальные значения для нормализации</li>
 *   <li>По {@code @BitMask} полям — аккумулирует OR встреченных бит <b>по каждому элементу отдельно</b>,
 *       что позволяет точно проверить частичные маски (например, last long на 12 бит из 268-битного массива)</li>
 * </ul>
 *
 * <h3>Использование:</h3>
 * <pre>
 *   PolicyRecordAnalyzer analyzer = new PolicyRecordAnalyzer();
 *
 *   // после каждой симуляции:
 *   for (PolicyRecord record : simulationRecords) {
 *       analyzer.analyze(record);
 *   }
 *
 *   // в конце:
 *   analyzer.buildReport().print();
 * </pre>
 *
 * <h3>Thread safety:</h3>
 * Не thread-safe. Если анализ идёт параллельно — создайте отдельный экземпляр на поток,
 * затем объедините через {@link #merge(PolicyRecordAnalyzer)}.
 */
public class PolicyRecordAnalyzer {

    // Метаданные полей — вычисляются один раз при инициализации
    private static final List<FieldMeta> FIELD_METAS;

    static {
        FIELD_METAS = buildFieldMetas();
    }

    // Для @DataField: key -> максимум
    private final Map<String, Double> dataFieldMaxValues = new LinkedHashMap<>();

    // Для @BitMask: key -> long[] где индекс = индекс элемента массива
    // (для примитивов — массив из 1 элемента).
    // Хранит OR всех встреченных значений по каждому элементу отдельно.
    private final Map<String, long[]> bitMaskObserved = new LinkedHashMap<>();

    private final AtomicLong recordsAnalyzed = new AtomicLong(0);

    public PolicyRecordAnalyzer() {
        for (FieldMeta meta : FIELD_METAS) {
            if (meta.isBitMask) {
                bitMaskObserved.put(meta.key, new long[meta.bitsPerElement.length]);
            } else {
                dataFieldMaxValues.put(meta.key, 0.0);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Основной метод
    // -------------------------------------------------------------------------

    /**
     * Анализирует одну запись. Вызывайте после каждой симуляции.
     */
    public void analyze(PolicyRecord record) {
        for (FieldMeta meta : FIELD_METAS) {
            try {
                if (meta.isBitMask) {
                    accumulateBitMask(meta, record);
                } else {
                    accumulateDataMax(meta, record);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Cannot access field: " + meta.key, e);
            }
        }
        recordsAnalyzed.incrementAndGet();
    }

    // -------------------------------------------------------------------------
    // Объединение результатов (для параллельного анализа)
    // -------------------------------------------------------------------------

    /**
     * Объединяет результаты другого анализатора в текущий.
     * Используйте для сборки результатов из параллельных потоков.
     */
    public void merge(PolicyRecordAnalyzer other) {
        for (Map.Entry<String, Double> e : other.dataFieldMaxValues.entrySet()) {
            dataFieldMaxValues.merge(e.getKey(), e.getValue(), Math::max);
        }
        for (Map.Entry<String, long[]> e : other.bitMaskObserved.entrySet()) {
            long[] dst = bitMaskObserved.get(e.getKey());
            long[] src = e.getValue();
            if (dst != null) {
                for (int i = 0; i < dst.length; i++) {
                    dst[i] |= src[i];
                }
            }
        }
        recordsAnalyzed.addAndGet(other.recordsAnalyzed.get());
    }

    // -------------------------------------------------------------------------
    // Построение отчёта
    // -------------------------------------------------------------------------

    public ValidationReport buildReport() {
        Map<String, ValidationReport.BitMaskStatus> statuses = new LinkedHashMap<>();
        for (FieldMeta meta : FIELD_METAS) {
            if (!meta.isBitMask) continue;
            long[] observed = bitMaskObserved.get(meta.key);
            statuses.put(meta.key, new ValidationReport.BitMaskStatus(meta.bitsPerElement, observed));
        }
        return new ValidationReport(
                new LinkedHashMap<>(dataFieldMaxValues),
                statuses,
                recordsAnalyzed.get()
        );
    }

    // -------------------------------------------------------------------------
    // Аккумуляция значений — BitMask
    // -------------------------------------------------------------------------

    private void accumulateBitMask(FieldMeta meta, PolicyRecord record) throws IllegalAccessException {
        Object value = meta.field.get(record);
        if (value == null) return;

        long[] observed = bitMaskObserved.get(meta.key);

        switch (meta.fieldKind) {
            // Примитив — один элемент
            case PRIMITIVE_LONG:
            case PRIMITIVE_INT:
            case PRIMITIVE_SHORT:
            case PRIMITIVE_BYTE:
                observed[0] |= toLong(value);
                break;

            // 1D массивы — каждый элемент -> свой слот observed[i]
            case ARRAY_LONG: {
                long[] arr = (long[]) value;
                for (int i = 0; i < arr.length; i++) observed[i] |= arr[i];
                break;
            }
            case ARRAY_INT: {
                int[] arr = (int[]) value;
                for (int i = 0; i < arr.length; i++) observed[i] |= arr[i] & 0xFFFFFFFFL;
                break;
            }
            case ARRAY_SHORT: {
                short[] arr = (short[]) value;
                for (int i = 0; i < arr.length; i++) observed[i] |= arr[i] & 0xFFFFL;
                break;
            }
            case ARRAY_BYTE: {
                byte[] arr = (byte[]) value;
                for (int i = 0; i < arr.length; i++) observed[i] |= arr[i] & 0xFFL;
                break;
            }

            // 2D массивы — разворачиваем в плоский индекс [row][col] -> row*cols + col
            case ARRAY2D_LONG: {
                long[][] arr = (long[][]) value;
                int idx = 0;
                for (long[] row : arr)
                    for (long v : row) observed[idx++] |= v;
                break;
            }
            case ARRAY2D_INT: {
                int[][] arr = (int[][]) value;
                int idx = 0;
                for (int[] row : arr)
                    for (int v : row) observed[idx++] |= v & 0xFFFFFFFFL;
                break;
            }
            case ARRAY2D_BYTE: {
                byte[][] arr = (byte[][]) value;
                int idx = 0;
                for (byte[] row : arr)
                    for (byte v : row) observed[idx++] |= v & 0xFFL;
                break;
            }

            default:
                break;
        }
    }

    // -------------------------------------------------------------------------
    // Аккумуляция значений — DataField
    // -------------------------------------------------------------------------

    private void accumulateDataMax(FieldMeta meta, PolicyRecord record) throws IllegalAccessException {
        Object value =value = meta.field.get(record);
        if (value == null) return;

        double max = 0.0;

        switch (meta.fieldKind) {
            case PRIMITIVE_BYTE:   max = ((Byte)    value) & 0xFF; break;
            case PRIMITIVE_SHORT:  max = (short)    (Short)   value; break;
            case PRIMITIVE_INT:    max = (int)      (Integer) value; break;
            case PRIMITIVE_FLOAT:  max = (float)    (Float)   value; break;
            case PRIMITIVE_LONG:   max = (long)     (Long)    value; break;
            case PRIMITIVE_BOOL:   max = ((Boolean) value) ? 1.0 : 0.0; break;

            case ARRAY_BYTE:
                for (byte  v : (byte[])  value) max = Math.max(max, v & 0xFF); break;
            case ARRAY_SHORT:
                for (short v : (short[]) value) max = Math.max(max, v); break;
            case ARRAY_INT:
                for (int   v : (int[])   value) max = Math.max(max, v); break;
            case ARRAY_FLOAT:
                for (float v : (float[]) value) max = Math.max(max, v); break;
            case ARRAY_LONG:
                for (long  v : (long[])  value) max = Math.max(max, v); break;

            case ARRAY2D_BYTE:
                for (byte[]  row : (byte[][])  value) for (byte  v : row) max = Math.max(max, v & 0xFF); break;
            case ARRAY2D_FLOAT:
                for (float[] row : (float[][]) value) for (float v : row) max = Math.max(max, v); break;

            default:
                return;
        }

        final double finalMax = max;
        dataFieldMaxValues.merge(meta.key, finalMax, Math::max);
    }

    // -------------------------------------------------------------------------
    // Построение метаданных полей через рефлексию (один раз при старте)
    // -------------------------------------------------------------------------

    private static List<FieldMeta> buildFieldMetas() {
        List<FieldMeta> metas = new ArrayList<>();

        for (Field field : PolicyRecord.class.getDeclaredFields()) {
            BitMask   bitMaskAnnotation   = field.getAnnotation(BitMask.class);
            DataField dataFieldAnnotation = field.getAnnotation(DataField.class);
            Feature   featureAnnotation   = field.getAnnotation(Feature.class);

            if (bitMaskAnnotation == null && dataFieldAnnotation == null) continue;

            field.setAccessible(true);

            FieldKind kind = detectKind(field);
            if (kind == FieldKind.UNKNOWN) {
                System.err.println("[PolicyRecordAnalyzer] Cannot determine kind for field: " + field.getName());
                continue;
            }

            Feature.FeatureScope scope = featureAnnotation != null
                    ? featureAnnotation.scope()
                    : Feature.FeatureScope.TABLE;

            boolean isBitMask = bitMaskAnnotation != null;
            int[] bitsPerElement = isBitMask
                    ? resolveBitsPerElement(bitMaskAnnotation, field, kind)
                    : new int[0];

            metas.add(new FieldMeta(field, kind, isBitMask, bitsPerElement, scope));
        }

        return Collections.unmodifiableList(metas);
    }

    /**
     * Вычисляет массив валидных бит на элемент по правилам приоритета @BitMask:
     * <ol>
     *   <li>{@code bitsPerElement} — явный массив, используется как есть</li>
     *   <li>{@code validBits} + {@code lastElementBits} — заполняем validBits, последний — lastElementBits</li>
     *   <li>{@code validBits} — все элементы одинаковые</li>
     * </ol>
     * Для примитивов всегда возвращает массив из одного элемента [{@code validBits}].
     */
    private static int[] resolveBitsPerElement(BitMask annotation, Field field, FieldKind kind) {
        // Примитив — один элемент, только validBits
        if (!kind.isArray) {
            return new int[]{ annotation.validBits() };
        }

        int length = resolveArrayLength(field, kind);

        // Приоритет 1: явный массив bitsPerElement
        int[] explicit = annotation.bitsPerElement();
        if (explicit.length > 0) {
            if (explicit.length != length) {
                throw new IllegalArgumentException(
                        "Field '" + field.getName() + "': @BitMask bitsPerElement length (" + explicit.length +
                                ") must match array length (" + length + ")"
                );
            }
            return explicit.clone();
        }

        // Приоритет 2 и 3: validBits [+ lastElementBits]
        int[] result = new int[length];
        Arrays.fill(result, annotation.validBits());
        if (annotation.lastElementBits() != 0) {
            result[length - 1] = annotation.lastElementBits();
        }
        return result;
    }

    /**
     * Возвращает число элементов в массиве через прототип PolicyRecord.
     * Для 2D — суммарное число элементов (развёрнутое в плоский observed[]).
     */
    private static int resolveArrayLength(Field field, FieldKind kind) {
        try {
            PolicyRecord proto = new PolicyRecord();
            Object value = field.get(proto);
            if (value == null) return 0;

            if (kind.is2D) {
                int total = 0;
                int rows = Array.getLength(value);
                for (int r = 0; r < rows; r++) {
                    total += Array.getLength(Array.get(value, r));
                }
                return total;
            } else {
                return Array.getLength(value);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot access field to determine length: " + field.getName(), e);
        }
    }

    private static FieldKind detectKind(Field field) {
        Class<?> type = field.getType();

        if (type == byte.class)    return FieldKind.PRIMITIVE_BYTE;
        if (type == short.class)   return FieldKind.PRIMITIVE_SHORT;
        if (type == int.class)     return FieldKind.PRIMITIVE_INT;
        if (type == long.class)    return FieldKind.PRIMITIVE_LONG;
        if (type == float.class)   return FieldKind.PRIMITIVE_FLOAT;
        if (type == boolean.class) return FieldKind.PRIMITIVE_BOOL;

        if (type == byte[].class)    return FieldKind.ARRAY_BYTE;
        if (type == short[].class)   return FieldKind.ARRAY_SHORT;
        if (type == int[].class)     return FieldKind.ARRAY_INT;
        if (type == long[].class)    return FieldKind.ARRAY_LONG;
        if (type == float[].class)   return FieldKind.ARRAY_FLOAT;

        if (type == byte[][].class)  return FieldKind.ARRAY2D_BYTE;
        if (type == int[][].class)   return FieldKind.ARRAY2D_INT;
        if (type == long[][].class)  return FieldKind.ARRAY2D_LONG;
        if (type == float[][].class) return FieldKind.ARRAY2D_FLOAT;

        return FieldKind.UNKNOWN;
    }

    private static long toLong(Object value) {
        if (value instanceof Long)    return (Long) value;
        if (value instanceof Integer) return ((Integer) value) & 0xFFFFFFFFL;
        if (value instanceof Short)   return ((Short)   value) & 0xFFFFL;
        if (value instanceof Byte)    return ((Byte)    value) & 0xFFL;
        return 0L;
    }

    // -------------------------------------------------------------------------
    // Вспомогательные типы
    // -------------------------------------------------------------------------

    private static class FieldMeta {
        final Field field;
        final FieldKind fieldKind;
        final boolean isBitMask;
        /**
         * Для примитивов: [validBits] — один элемент.
         * Для 1D массивов: по одному значению на каждый элемент.
         * Для 2D массивов: развёрнуто в плоский массив [row * cols + col].
         */
        final int[] bitsPerElement;
        final Feature.FeatureScope featureScope;
        final String key;

        FieldMeta(Field field, FieldKind fieldKind, boolean isBitMask,
                  int[] bitsPerElement, Feature.FeatureScope featureScope) {
            this.field = field;
            this.fieldKind = fieldKind;
            this.isBitMask = isBitMask;
            this.bitsPerElement = bitsPerElement;
            this.featureScope = featureScope;
            this.key = field.getName();
        }
    }

    private enum FieldKind {
        PRIMITIVE_BYTE  (false, false),
        PRIMITIVE_SHORT (false, false),
        PRIMITIVE_INT   (false, false),
        PRIMITIVE_LONG  (false, false),
        PRIMITIVE_FLOAT (false, false),
        PRIMITIVE_BOOL  (false, false),
        ARRAY_BYTE      (true,  false),
        ARRAY_SHORT     (true,  false),
        ARRAY_INT       (true,  false),
        ARRAY_LONG      (true,  false),
        ARRAY_FLOAT     (true,  false),
        ARRAY2D_BYTE    (true,  true),
        ARRAY2D_INT     (true,  true),
        ARRAY2D_LONG    (true,  true),
        ARRAY2D_FLOAT   (true,  true),
        UNKNOWN         (false, false);

        final boolean isArray;
        final boolean is2D;

        FieldKind(boolean isArray, boolean is2D) {
            this.isArray = isArray;
            this.is2D = is2D;
        }
    }
}