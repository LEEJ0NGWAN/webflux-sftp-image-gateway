package com.example.image.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@ConfigurationProperties("api.path")
public class ApiPathProperties {

    private String healthCheck;
    private String imageUpload;
}
