# @CrossOrigin
### An annotation based approach to CORS support for Spring based projects.

## Overview

Browsers have adopted the [Cross-Origin Request Scripting (CORS)](http://www.html5rocks.com/en/tutorials/cors/) standard as a way of protecting the user from malicious sites.  When the browser detects that the current page is making a request to an 'origin' other than where that page was loaded, the browser applies some rules to decide if it will allow the request.  This is particularly important when the page is asking the browser to send any cookies associated to the other site or add custom headers on the request.  Imagine the havoc if any page you viewed could ask the browser to send a money-transfer request to your bank and the browser blindly sent your session cookie along with it.

CrossOrigin is a small library which adds server side support for CORS requests.  By annotating your existing request handler methods with @CrossOrigin, you indicate that requests handled by that method should be accessible from origins other than that of the request handler itself.

```java

@CrossOrigin
@RequestMapping(method = RequestMethod.GET, value = "/movies/{id}")
public Object getMovie(@PathVariable(value = "id") int movieId) {
    ...
}

```

Under the covers, this library adds a request handler mapping for OPTIONS requests to the same path of any annotated 'primary' request handlers.  This CORS request handler provides support for Pre-Flight requests based on the configuration of the request handler.  For example, if the request handler expects a certain header to be present (`@RequestMapping(headers={"AuthToken"})`) then the Pre-Flight request will only be approved if that same header is listed in the Pre-Flight request's `Access-Control-Request-Headers` header.  In other words, the Pre-Flight request is matched as closely as possible to the actual request handler that the client wishes to call.

Also, an interceptor is added to the Spring context which handles adding response headers that the browser expects on both Pre-Flight and primary requests, when those requests are across origins.  The interceptor executes prior to the request handler so that any headers that the primary request handler sets will overwrite any that the interceptor created.

## Usage

@CrossOrigin has 4 attributes that control the behavior of the CORS support:
- origin - A list of origins that are permitted to access the annotated request handler.  Defaults to "*", indicating that any origin may access the annotated request handler.
- header - A list of _response_ headers (name of header entry) that should be exposed to the page that made the request.  Empty by default.  Add the name of any header that the annotated request handler adds to the _response_ that should be exposed to the client, such as pagination links, authorization tokens, etc.  The values in this list are used to populate the CORS response header `Access-Control-Expose-Headers`
- allowCredentials - A boolean flag, defaulted to false, which indicates to the browser whether or not it should send 'credentials' (aka, cookies) with the request.  Setting this to true results in adding the CORS response header `Access-Control-Allow-Credentials` with a value of `true`.
- maxAge - An integer indicating the maximum time, in seconds, that a browser should cache or otherwise retain the CORS settings for this request handler.  Defaulted to 0, no caching.  Setting this higher is encouraged, as it will prevent the browser from having to make two requests (a CORS Pre-Flight request and the primary request) for each application request.

## Configuration

In order to enable @CrossOrigin support, you must enable it by adding @EnableCrossOrigin to any Spring Java Configuration class.

This results in adding two beans to the context:
```java
@Bean
public MappedInterceptor crossOriginInterceptor() {
    return new MappedInterceptor(null, new CrossOriginInterceptor());
}

@Bean
public CrossOriginHandlerMapping crossOriginHandlerMapping() {
    return new CrossOriginHandlerMapping();
}
```

If you are not using Java based Spring configuration, simply add these beans to the appropriate context.

## Known Issues

1. Path Collisions - If two request handler methods have the same path and use headers, content type, or other non-path based factors to distinguish themselves from one another, the CrossOrogin library will attempt to create a mirror Pre-Flight request handler for each.  Unfortunately the paths are identical and this results in a duplicate mapping exception.
1. Expressiveness Mismatch - Annotation based request mappings are very expressive.  They allow mappings to be defined based on header values, and even custom conditions.  Unfortunately, the CORS Pre-Flight request does not provide similar levels of detail about the request that the browser is considering making.  As a result, it is possible for a Pre-Flight response to indicate approval for a cross origin request, only for the cross origin request to then be rejected by Spring as not meeting the request mapping's more granular specification.  This is not a function of this library.  It's inherent to the expressiveness of the CORS requests.

