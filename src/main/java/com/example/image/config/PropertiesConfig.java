package com.example.image.config;

import com.example.image.properties.SftpProperties;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ SftpProperties.class })
public class PropertiesConfig {}
