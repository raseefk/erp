package com.supererp.erp.rbac.annotation;

import java.lang.annotation.*;

/**
 * Annotation to enforce that a tenant has a specific feature enabled.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresFeature {
    String value();
}
