package com.supererp.erp.rbac.annotation;

import java.lang.annotation.*;

/**
 * Annotation to mark service methods for automatic auditing.
 * The aspect will capture the entity state change.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditAction {
    /**
     * The action name (e.g., "CREATE_INVOICE", "UPDATE_ROLE")
     */
    String value();

    /**
     * The type of entity being modified
     */
    String entityType() default "";
}
