package com.example.image.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import com.example.image.handler.HealthChecker;
import com.example.image.handler.ImageUploader;
import com.example.image.handler.PayloadDecoder;
import com.example.image.dto.ImageUploadResponse;
import com.example.image.handler.ConnectionManager;
import com.example.image.handler.DestinationParser;
import com.example.image.properties.SftpProperties;
import com.example.image.vo.ImageUploadRequest;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;

@EnableWebFlux
@Configuration(proxyBeanMethods = false)
public class RouterConfig {

    private static final String HEALTH_URI = "/health";
    private static final String UPLOAD_URI = "/upload";

    private static final ResponseStatusException NO_REQUSET_BODY_EXCEPTION =
    new ResponseStatusException(HttpStatus.BAD_REQUEST, "No request body is available");

    @Bean
    public RouterFunction<ServerResponse> health() {

        return route(GET(HEALTH_URI), HealthChecker::handle);
    }

    @Bean
    public RouterFunction<ServerResponse> router(SftpProperties sftpProperties)
    throws Exception {

        ConnectionManager
        .init(
            sftpProperties.getPrivateKeyBytes(),
            sftpProperties.getRemoteUsername(),
            sftpProperties.getRemoteAddress(),
            sftpProperties.getMinSessionCount(),
            sftpProperties.getMaxChannelCount());

        final String remoteDirectory = sftpProperties.getRemoteDirectory();

        return route(
            POST(UPLOAD_URI), request -> request
            .bodyToMono(ImageUploadRequest.class)
            .switchIfEmpty(Mono.error(NO_REQUSET_BODY_EXCEPTION))
            .flatMap(
                vo -> Mono.zip(
                    PayloadDecoder.handle(vo.getPayload()),
                    DestinationParser.handle(remoteDirectory, vo.getName())))
            .zipWhen($ -> ConnectionManager.connect())
            .flatMap(t -> Mono.zip(
                Mono.just(t.getT2()),
                Mono.just(t.getT1().getT2()),
                ImageUploader.handle(t.getT2(), t.getT1().getT1(), t.getT1().getT2())))
            .flatMap(t -> {

                ConnectionManager.disconnect(t.getT1());

                final ImageUploadResponse body = ImageUploadResponse
                .builder()
                .location(t.getT2())
                .result(t.getT3())
                .build();

                return ServerResponse.ok().bodyValue(body);
            })
        );
    }
}
