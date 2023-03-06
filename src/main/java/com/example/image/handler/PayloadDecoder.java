package com.example.image.handler;

import java.util.Base64;
import java.util.Base64.Decoder;

import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

public class PayloadDecoder {

    private static final Decoder decoder = Base64.getDecoder();

    private static final ResponseStatusException FAILED_DECODE_EXCEPTION =
    new ResponseStatusException(BAD_REQUEST, "Failed to decode payload");

    private static final ResponseStatusException NO_PAYLOAD_EXCEPTION =
    new ResponseStatusException(BAD_REQUEST, "No payload is available");

    public static Mono<byte[]> handle(final String payload) {

        return Mono
        .justOrEmpty(payload)
        .map(decoder::decode)
        .onErrorMap(e -> FAILED_DECODE_EXCEPTION)
        .switchIfEmpty(Mono.error(NO_PAYLOAD_EXCEPTION));
    }
}
