package com.example.image.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
public class WebfluxConfig implements WebFluxConfigurer {

    private static final int MEMORY_BYTES = 5 * 1024 * 1024; // 5MB memory for request body

    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {

        configurer.defaultCodecs().maxInMemorySize(MEMORY_BYTES);
    }
}
