package com.allenru.crossorigin;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Marks the annotated method as permitting cross origin requests.
 * 
 * By default, all origins are permitted.  This may be narrowed by setting
 * the origin property of the annotation.
 * 
 * If any non-standard headers are sent in the response, then the header 
 * name should be included in the header property of this annotation so 
 * that the browser knows to allow the page to access those headers.  See
 * CORS spec's Access-Control-Expose-Headers for additional details.
 *
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CrossOrigin {
	
	/**
	 * List of allowed origins.  By default, all (*) origins are allowed.
	 * 
	 * These values are placed in the Access-Control-Allow-Origin header of
	 * both the pre-flight and primary responses by the {@link CrossOriginInterceptor}.
	 */
	String[] origin() default {"*"};
	
	/**
	 * List of response headers that the user-agent will allow the client to access,
	 * assuming the request is from a CORS aware/compliant source and is making the
	 * request on behalf of a third party script.
	 * 
	 * This property controls the value of both the pre-flight and primary response's 
	 * Access-Control-Expose-Headers header, which is set by the {@link CrossOriginInterceptor}.
	 */
	String[] header() default {};
	
	/**
	 * Set to true if the the browser should include any cookies associated to the domain
	 * of the request being annotated.  False by default.
	 * 
	 * If an only if true, the pre-flight response will include the header
	 * "Access-Control-Allow-Credentials=true".  This is set by {@link CrossOriginHandlerMapping}.
	 */
	boolean allowCredentials() default false;
	
	/**
	 * Controls the cache duration for pre-flight responses.  Setting this to a reasonable 
	 * value can reduce the number of pre-flight request/response interaction required by
	 * the browser.
	 * 
	 * This property controls the value of the Access-Control-Max-Age header in the pre-flight
	 * response, as set by {@link CrossOriginHandlerMapping}.
	 */
	int maxAge() default 0;
	
}