package com.example.image.handler;

import com.jcraft.jsch.ChannelSftp;

import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import java.io.ByteArrayInputStream;

public class ImageUploader {

    private static final ResponseStatusException NO_PARAMETER_EXCEPTION =
    new ResponseStatusException(BAD_REQUEST, "Channel or bytes or destination is not available");

    public static Mono<Boolean> handle(final ChannelSftp channel, final byte[] bytes, final String destination) {

        return Mono
        .zip(
            Mono.justOrEmpty(channel),
            Mono.justOrEmpty(bytes),
            Mono.justOrEmpty(destination))
        // .map(ImageUploader::upload)  <-- map is synchronous action; we should avoid map
        .flatMap(t -> Mono.just((ImageUploader.upload(t)))) // async emit event
        .switchIfEmpty(Mono.error(NO_PARAMETER_EXCEPTION));
    }

    private static boolean upload(final Tuple3<ChannelSftp, byte[], String> tuple3) {

        boolean result = false;
        try (final ByteArrayInputStream bas = new ByteArrayInputStream(tuple3.getT2())) {

            final ChannelSftp channel = tuple3.getT1();
            final String destination = tuple3.getT3();

            channel.put(bas, destination);
            channel.chmod(Integer.parseInt("666", 8), destination);

            result = true;

        } catch (Exception e) { e.printStackTrace(); }

        return result;
    }
}
