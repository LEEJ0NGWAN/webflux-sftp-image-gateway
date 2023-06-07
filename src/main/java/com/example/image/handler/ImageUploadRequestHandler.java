package com.example.image.handler;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;

import com.example.image.dto.ImageUploadResponse;
import com.example.image.properties.SftpProperties;
import com.example.image.vo.ImageUploadRequest;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Component
@RequiredArgsConstructor
public class ImageUploadRequestHandler {

    private static final ResponseStatusException NO_REQUSET_BODY_EXCEPTION =
    new ResponseStatusException(BAD_REQUEST, "No request body is available");

    private final SftpProperties sftpProperties;
    private final ConnectionManager connectionManager;

    public Mono<ServerResponse> handle(ServerRequest serverRequest) {

        return serverRequest
        .bodyToMono(ImageUploadRequest.class)
        .switchIfEmpty(Mono.defer(()->Mono.error(NO_REQUSET_BODY_EXCEPTION)))
        .flatMap(vo -> Mono.zip(
            PayloadDecoder.handle(vo.getPayload()),
            DestinationParser.handle(sftpProperties.getRemoteDirectory(), vo.getName())
        ))
        .zipWhen($ -> connectionManager.connect())
        .flatMap(tuple -> Mono.zip(
            Mono .just(tuple.getT2()),
            Mono.just(tuple.getT1().getT2()),
            ImageUploader.handle(tuple.getT2(), tuple.getT1().getT1(), tuple.getT1().getT2())
        ))
        .flatMap(tuple -> {

            connectionManager.disconnect(tuple.getT1());

            final ImageUploadResponse body = ImageUploadResponse
            .builder()
            .location(tuple.getT2())
            .result(tuple.getT3())
            .build();

            return ServerResponse.ok().bodyValue(body);
        });
    }
}
