package com.example.image.config;

import java.util.Map;

import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.error.ErrorAttributeOptions.Include;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;

@Component @Order(-2)
public class CustomWebExceptionHandler extends AbstractErrorWebExceptionHandler {

    // Spring boot 2.6.x <- ResourcePropeties was removed in 2.6.x (deprecated since 2.4)
    public CustomWebExceptionHandler(
        ErrorAttributes errorAttributes, WebProperties webProperties,
        ApplicationContext applicationContext, ServerCodecConfigurer configurer) {

        super(errorAttributes, webProperties.getResources(), applicationContext);
        this.setMessageWriters(configurer.getWriters());
    }

    @Override
    protected void logError(ServerRequest request, ServerResponse response, Throwable throwable) {

        if (!(throwable instanceof ResponseStatusException))
        super.logError(request, response, throwable);
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {

        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    private Mono<ServerResponse> renderErrorResponse(final ServerRequest request) {

        final Map<String, Object> errorAttributes =
        getErrorAttributes(request, ErrorAttributeOptions.of(Include.MESSAGE));

        final int status = (Integer) errorAttributes.get("status");

        // do somethig

        errorAttributes.remove("path");
        errorAttributes.remove("timestamp");
        errorAttributes.remove("status");

        return ServerResponse.status(status).bodyValue(errorAttributes);
    }
}