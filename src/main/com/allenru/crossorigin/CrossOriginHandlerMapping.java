package com.allenru.crossorigin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.HeadersRequestCondition;
import org.springframework.web.servlet.mvc.condition.NameValueExpression;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * {@link CrossOriginHandlerMapping} extends Spring's {@link RequestMappingHandlerMapping} class and 
 * enables support for CORS requests.  Most importantly, it watches for request methods that are 
 * annotated with {@link CrossOrigin}, and then registers a 'mirror' request handler method (created
 * here) to handle the OPTIONS request used by browsers for pre-flighting CORS requests.
 *
 */
class CrossOriginHandlerMapping extends RequestMappingHandlerMapping {

	private final Method corsMethod;
	private final RequestMappingInfo baseCorsRequestMappingInfo;
	
	public CrossOriginHandlerMapping() {
		/*
		 * This mapping handler MUST precede the traditional RequestMappingHandlerMapping, 
		 * else RequestMappingHandlerMapping will exception out the OPTIONS request 
		 * (as OPTIONS method not supported) instead of allowing the dispatcher to try 
		 * other handler mappings.
		 */
		super.setOrder(-1);  
		//These are created once here instead of every pass through the registerHandlerMethod() method.
		this.corsMethod = CorsPreflightController.class.getDeclaredMethods()[0];
		this.baseCorsRequestMappingInfo = getMappingForMethod(corsMethod, CorsPreflightController.class);
	}
	
	@Override
	protected HandlerMethod getHandlerInternal(HttpServletRequest request) throws Exception {
		/*
		 * The parent RequestMappingHandlerMapping will ultimately throw an exception if it 
		 * finds a path based match but the method does not match.  To prevent this, we intercept
		 * any non-OPTIONS calls here and simply return null, which triggers DispatcherServlet to
		 * try the next Handler Mapping, which in all likelihood has the true target handler.
		 */
		if (!HttpMethod.OPTIONS.name().equals(request.getMethod())) return null;
		//otherwise try, but don't exception out as this may not be the only handler mapping with OPTIONS handlers.
		try {
			return super.getHandlerInternal(request);
		}
		catch (Exception ignored) {
			return null;
		}
	}
	
	@Override
	protected void registerHandlerMethod(Object handler, Method method, RequestMappingInfo primaryMapping) {
		/*
		 * The traditional RequestMappingHandlerMapping (another instance) will handle the handlers passed in.
		 * All we need to do here is register an OPTIONS variant so that we can support Pre-Flight requests to
		 * those other handlers.
		 */
		CrossOrigin crossOrigin = AnnotationUtils.findAnnotation(method, CrossOrigin.class);
		if (crossOrigin != null) {
			//create the CORS handler's RequestMappingInfo using the path pattern from the primary handler.
			ArrayList<String> requestMethodConditions = new ArrayList<String>();
			if (primaryMapping.getMethodsCondition() != null) {
				Set<RequestMethod> allowedMethods = primaryMapping.getMethodsCondition().getMethods();
				for (RequestMethod requestMethod : allowedMethods) {
					requestMethodConditions.add("Access-Control-Request-Method="+requestMethod.name());
				}
			}
			if (requestMethodConditions.isEmpty()) requestMethodConditions.add("Access-Control-Request-Method");
			HeadersRequestCondition requestMethodHeaderCondition = new HeadersRequestCondition(requestMethodConditions.toArray(new String[requestMethodConditions.size()]));
			RequestMappingInfo corsMapping = baseCorsRequestMappingInfo.combine(new RequestMappingInfo(primaryMapping.getPatternsCondition(), null, null, requestMethodHeaderCondition, null, null, null));
			//create the handler (curry some information from primary handler)...
			RequestMapping requestMapping = AnnotationUtils.findAnnotation(method, RequestMapping.class);
			CorsPreflightController corsHandler = new CorsPreflightController(
					crossOrigin.origin(), 
					requestMapping.method(), 
					primaryMapping.getHeadersCondition() == null ? 
							Collections.<NameValueExpression<String>>emptySet() 
							: primaryMapping.getHeadersCondition().getExpressions(), 
					crossOrigin.allowCredentials(), 
					crossOrigin.maxAge());
			//finally, register the CORS mirror request handler...
			super.registerHandlerMethod(corsHandler, corsMethod, corsMapping);
		}
	}
	
	
	private static class CorsPreflightController {

		private String[] allowedOrigins;
		private RequestMethod[] allowedMethods;
		private Set<NameValueExpression<String>> headerExpressions;
		private boolean allowCredentials;
		private int maxAge;
		
		private CorsPreflightController(
				String[] allowedOrigins, 
				RequestMethod[] allowedMethods, 
				Set<NameValueExpression<String>> headerExpressions, 
				boolean allowCredentials, 
				int maxAge) {
			Arrays.sort(this.allowedOrigins = allowedOrigins);
			Arrays.sort(this.allowedMethods = allowedMethods);
			this.headerExpressions = headerExpressions;
			this.allowCredentials = allowCredentials;
			this.maxAge = maxAge;
		}
		
		@RequestMapping(method=RequestMethod.OPTIONS, headers={"Origin" /*Access-Control-Request-Method is dynamically added based on primary handlers accepted methods*/})
		private @ResponseBody ResponseEntity<Void> universalCorsPreflightHandler(
				@RequestHeader("Origin") String requestedOrigin,
				@RequestHeader("Access-Control-Request-Method") RequestMethod requestedMethod,
				@RequestHeader(value="Access-Control-Request-Headers", required=false) String[] requestedHeaders) {
			
			if (Arrays.binarySearch(allowedOrigins, requestedOrigin) < 0
				&& Arrays.binarySearch(allowedOrigins, "*") < 0) {
				return new ResponseEntity<Void>(HttpStatus.FORBIDDEN);
			}
			
			if (Arrays.binarySearch(allowedMethods, requestedMethod) < 0) {
				return new ResponseEntity<Void>(HttpStatus.FORBIDDEN);
			}

			//check the header conditions.  Note, this assumes extraneous headers from the user-agent are valid.
			if (requestedHeaders != null) Arrays.sort(requestedHeaders);
			for (NameValueExpression<String> headerExpression : headerExpressions) {
				if (headerExpression.isNegated()) {
					if (requestedHeaders != null && Arrays.binarySearch(requestedHeaders, headerExpression.getName()) >= 0) {
						//user-agent is requesting to send a header that is negated by the primary handler
						return new ResponseEntity<Void>(HttpStatus.FORBIDDEN);
					}   //else user-agent is not sending the negated header, thus no issues.
				}
				else {
					if (requestedHeaders == null || Arrays.binarySearch(requestedHeaders, headerExpression.getName()) < 0) {
						//user-agent is missing (not requested) a header that is required by the primary handler
						return new ResponseEntity<Void>(HttpStatus.FORBIDDEN);
					}   //else user-agent is sending the required header, thus no issues.
				}
			}
			
			HttpHeaders responseHeaders = new HttpHeaders();
			//CORS Required PREFLIGHT Only - Tells browser which header values may be transmitted.
		    if (requestedHeaders != null) responseHeaders.set("Access-Control-Allow-Headers", StringUtils.arrayToDelimitedString(requestedHeaders, ", "));
		    //CORS Required PREFLIGHT Only - Tells browser which http methods are allowed.
		    responseHeaders.set("Access-Control-Allow-Methods", StringUtils.arrayToDelimitedString(allowedMethods, ", "));
		    //CORS Optional PREFLIGHT Only - Tells browser it can cache the OPTIONS response for indicated seconds, thus avoiding double requests to the server for previously pre-flight'd URIs. 
		    if (maxAge > 0) responseHeaders.set("Access-Control-Max-Age", Integer.toString(maxAge));
		    //CORS Optional - (in PREFLIGHT response) tells Browser it should send cookies.  
		    if (allowCredentials) responseHeaders.set("Access-Control-Allow-Credentials", "true");
		    return new ResponseEntity<Void>(responseHeaders, HttpStatus.NO_CONTENT);
		}
	}
}