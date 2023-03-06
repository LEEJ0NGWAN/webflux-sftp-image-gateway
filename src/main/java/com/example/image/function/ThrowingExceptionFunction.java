package com.example.image.function;

import java.util.function.Function;

public interface ThrowingExceptionFunction<T, R, E extends Exception> {

    R apply(T t) throws E;

    static <T, R> Function<T, R> wrap(ThrowingExceptionFunction<T, R, Exception> function) {

        return t -> {

            R r = null;

            try { r = function.apply(t); }
            catch (Exception e) { throw new RuntimeException(e); }

            return r;
        };
    }
}
