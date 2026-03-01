package com.terraforming.ares.services.policyai.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * <p>Example:
 * <pre>
 *   {@code @DataField}
 *   public byte oxygenLeft;
 *
 *   {@code @DataField}
 *   public short[] mcIncomeTotal; // max по обоим игрокам
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DataField {
}