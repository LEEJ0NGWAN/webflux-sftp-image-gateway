package com.example.image.handler;

import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

public class DestinationParser {

    private static final ResponseStatusException FAILED_PARSE_EXCEPTION =
    new ResponseStatusException(BAD_REQUEST, "Invalid destination");

    private static final ResponseStatusException NO_PARAMETER_EXCEPTION =
    new ResponseStatusException(BAD_REQUEST, "File name or directory is not available");

    public static Mono<String> handle(final String directory, final String fileName) {

        return Mono
        .zip(Mono.justOrEmpty(directory), Mono.justOrEmpty(fileName))
        .map(t -> t.getT1().concat(t.getT2()))
        .onErrorMap(e -> FAILED_PARSE_EXCEPTION)
        .switchIfEmpty(Mono.error(NO_PARAMETER_EXCEPTION));
    }
}
