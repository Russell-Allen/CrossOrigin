package com.allenru.crossorigin;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CrossOrigin {
	
	String[] origin() default {"*"};
	
	/**
	 * List of response headers that the user-agent will allow the client to access,
	 * assuming the request is from a CORS aware/compliant source and is making the
	 * request on behalf of a third party script.
	 */
	String[] header() default {};
	
	boolean allowCredentials() default false;
	
	int maxAge() default 0;
	
}