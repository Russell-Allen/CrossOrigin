package com.allenru.crossorigin;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;


/**
 * Applies response headers for requests that are handled by handlers annotated with a CrossOrigin
 * annotation.  Response headers are applied BEFORE the handler executes.  Thus, if a handler
 * writes to the CORS response headers, it will overwrite the values set here.
 *
 * As handlers are discovered, their CrossOrigin settings are cached for efficiency.
 */
public class CrossOriginInterceptor extends HandlerInterceptorAdapter {
	
	private static final Logger log = LoggerFactory.getLogger(CrossOriginInterceptor.class);

	private Map<Object, Map<String, String>> responseHeadersByHandler = new IdentityHashMap<>();

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		Map<String, String> responseHeaders = getResponseHeaders(handler);
		if (responseHeaders != null) {
			//While the handler supports CORS, we still need to see if the requesting origin is in the allowed set...
			String allowedOrigins = responseHeaders.get("Access-Control-Allow-Origin");
			String requestedOrigin = request.getHeader("Origin");
			if ("*".equals(allowedOrigins) || allowedOrigins.contains(requestedOrigin)) {
				log.trace("requests origin '{}' is allowed.  Adding CORS headers to response.", requestedOrigin);
				for (Map.Entry<String, String> entry : responseHeaders.entrySet()) {
					response.setHeader(entry.getKey(), entry.getValue());
				}
				//OVERWRITE the Access-Control-Allow-Origin header with the request specific value!
				response.setHeader("Access-Control-Allow-Origin", requestedOrigin);
			}
			else {
				log.trace("requests origin '{}' is not in configured allowed origins: {}", requestedOrigin, allowedOrigins);
			}
		}
		else {
			log.trace("No CORS response headers found for request handler: {}", handler);
		}
		return true;
	}

	private Map<String, String> getResponseHeaders(Object handler) {
		/*
		 * The handler object is a new instance with each call.  Need to resolve it into
		 * a long lived object so that we may use it as a cache key. 
		 */
		if (!(handler instanceof HandlerMethod)) return null;
		HandlerMethod handlerMethod = (HandlerMethod) handler;
		Method javaMethod = handlerMethod.getMethod();
		if (!responseHeadersByHandler.containsKey(javaMethod)) {
			Map<String, String> headers = null;
			CrossOrigin crossOrigin = AnnotationUtils.findAnnotation(javaMethod, CrossOrigin.class);
			if (crossOrigin != null) {
				headers = new HashMap<>();
				headers.put("Access-Control-Allow-Origin", StringUtils.arrayToDelimitedString(crossOrigin.origin(), ", "));
				if (crossOrigin.header().length > 0) headers.put("Access-Control-Expose-Headers", StringUtils.arrayToDelimitedString(crossOrigin.header(), ", "));
				if (crossOrigin.allowCredentials()) headers.put("Access-Control-Allow-Credentials", "true");
			}
			responseHeadersByHandler.put(javaMethod, headers);
		}
		return responseHeadersByHandler.get(javaMethod);
	}
}
