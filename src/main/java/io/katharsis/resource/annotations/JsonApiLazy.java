package io.katharsis.resource.annotations;

import java.lang.annotation.*;

/**
 * Indicates a lazy relationship
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
@Inherited
public @interface JsonApiLazy {
}
