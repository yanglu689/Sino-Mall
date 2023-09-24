package com.sino.auth.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "sinomall.server")
public class AuthServerProperties {
    private String authDomain;
    private String indexDomain;

}
