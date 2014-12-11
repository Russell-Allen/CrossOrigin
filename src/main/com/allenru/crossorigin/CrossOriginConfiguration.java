package com.allenru.crossorigin;

import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.handler.MappedInterceptor;

/**
 * A Spring java configuration that must be loaded into the context in order to
 * enable CORS support via this library.
 * 
 * For XML configurations, simply declare the two beans below.
 * 
 * For Java configuration, instead of including this class by name, simply annotate
 * an existing configuration class with {@link EnableCrossOrigin} which will then 
 * load this class.
 */
class CrossOriginConfiguration {

	@Bean
	public MappedInterceptor crossOriginInterceptor() {
		return new MappedInterceptor(null, new CrossOriginInterceptor());
	}

	@Bean
	public CrossOriginHandlerMapping crossOriginHandlerMapping() {
		return new CrossOriginHandlerMapping();
	}
	
}
