package com.allenru.crossorigin;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

/**
 * Apply this annotation to a Spring Java Configuration class so that this library may
 * properly configure itself.  See {@link CrossOriginConfiguration} for exactly what
 * this involves.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(CrossOriginConfiguration.class)
public @interface EnableCrossOrigin {

}
