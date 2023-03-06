package com.example.image.handler;

import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Mono;

public class HealthChecker {

    public static Mono<ServerResponse> handle(final ServerRequest request) {

        return ServerResponse.ok().bodyValue("Health Check OK");
    }
}
