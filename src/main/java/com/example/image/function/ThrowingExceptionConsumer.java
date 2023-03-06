package com.example.image.function;

import java.util.function.Consumer;

@FunctionalInterface
public interface ThrowingExceptionConsumer<T, E extends Exception> {

    void accept(T t) throws E;

    static <T> Consumer<T> wrap(ThrowingExceptionConsumer<T, Exception> consumer) {

        return t -> {

            try { consumer.accept(t); }
            catch (Exception e) { throw new RuntimeException(e); }
        };
    }
}
