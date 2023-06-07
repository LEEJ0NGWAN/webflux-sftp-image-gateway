package com.example.image.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import com.example.image.handler.HealthChecker;
import com.example.image.handler.ImageUploadRequestHandler;
import com.example.image.properties.ApiPathProperties;

import lombok.RequiredArgsConstructor;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;

@EnableWebFlux
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class RouterConfig {

    private final ApiPathProperties apiPathProperties;

    @Bean
    public RouterFunction<ServerResponse> health() {

        return route(GET(apiPathProperties.getHealthCheck()), HealthChecker::handle);
    }

    @Bean
    public RouterFunction<ServerResponse> router(ImageUploadRequestHandler handler)
    throws Exception {

        return route(POST(apiPathProperties.getImageUpload()), handler::handle);
    }
}
