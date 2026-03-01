package com.terraforming.ares.services.policyai.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * <p> Example:
 * <pre>
 *   {@code @Feature(scope = FeatureScope.TABLE)}
 *   {@code @DataField}
 *   public byte oxygenLeft;
 *
 *   {@code @Feature(scope = FeatureScope.TABLE_AND_HAND)}
 *   {@code @BitMask(validBits = 24)}
 *   public int[] corporationMask;
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Feature {

    FeatureScope scope() default FeatureScope.TABLE;

    enum FeatureScope {
        TABLE,
        TABLE_AND_HAND
    }
}