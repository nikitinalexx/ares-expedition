package com.terraforming.ares.services.policyai.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Помечает поле как битовую маску.
 *
 * <p>Для примитивных полей (byte, short, int, long) — маска напрямую.
 * Для массивов (byte[], int[], long[], long[][]) — каждый элемент является маской.
 *
 * <p>Сервис анализа будет аккумулировать OR встреченных значений отдельно для каждого
 * элемента массива и сравнивать с соответствующей маской валидных бит, чтобы выявить:
 * <ul>
 *   <li>непокрытые биты — симуляции не покрыли все состояния</li>
 *   <li>лишние биты — в коде есть биты, которых не должно быть</li>
 * </ul>
 *
 * <h3>Три способа задать валидные биты (приоритет сверху вниз):</h3>
 *
 * <p><b>1. {@code bitsPerElement} — явный массив, полный контроль:</b>
 * <pre>
 *   {@code @BitMask(bitsPerElement = {64, 64, 64, 64, 12})}
 *   public long[] handCardMask = new long[5];
 * </pre>
 *
 * <p><b>2. {@code validBits} + {@code lastElementBits} — компактная запись для неоднородных массивов:</b>
 * <pre>
 *   {@code @BitMask(validBits = 64, lastElementBits = 12)}
 *   public long[] handCardMask = new long[5]; // эквивалентно {64, 64, 64, 64, 12}
 * </pre>
 *
 * <p><b>3. {@code validBits} — однородный массив, все элементы одинаковые:</b>
 * <pre>
 *   {@code @BitMask(validBits = 24)}
 *   public int[] corporationMask = new int[2]; // оба элемента — 24 бита
 *
 *   {@code @BitMask(validBits = 9)}
 *   public short openOceans; // примитив — 9 бит
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface BitMask {

    /**
     * Количество валидных бит для всех элементов (начиная с LSB).
     * Для примитивов — единственный параметр.
     * Для массивов — применяется ко всем элементам, кроме последнего если задан {@link #lastElementBits()}.
     * Игнорируется если задан {@link #bitsPerElement()}.
     */
    int validBits() default 0;

    /**
     * Количество валидных бит для последнего элемента массива.
     * Используется совместно с {@link #validBits()} для компактного описания неоднородных массивов.
     * Игнорируется если задан {@link #bitsPerElement()}.
     * Значение 0 означает "не задано" — последний элемент использует {@link #validBits()}.
     */
    int lastElementBits() default 0;

    /**
     * Явное количество валидных бит для каждого элемента массива.
     * Имеет наивысший приоритет. Длина массива должна совпадать с длиной поля.
     * Для примитивных полей не используется.
     */
    int[] bitsPerElement() default {};
}