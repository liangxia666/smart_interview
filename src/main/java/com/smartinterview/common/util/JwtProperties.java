package com.smartinterview.common.util;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix="smartinterview.jwt")
public class JwtProperties {
    private  String userSecretkey;
    private Long userTtl;
    private  String userTokenName;
}
