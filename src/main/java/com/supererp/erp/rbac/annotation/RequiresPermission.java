package com.supererp.erp.rbac.annotation;

import java.lang.annotation.*;

/**
 * Annotation to enforce a specific permission on a controller method.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPermission {
    String value();
}
