package com.github.springboot.config;

import javax.validation.constraints.NotNull;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@ConfigurationProperties(prefix = "cert")
@Validated
public class JwtConfigurationProperties {
    @NotNull
    private Integer expireInMinutes = 1440;
}
