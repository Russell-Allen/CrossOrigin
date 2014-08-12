package com.allenru.crossorigin;

import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.handler.MappedInterceptor;

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
