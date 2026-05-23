package com.eureka.flowbuilder.config;

import com.volcengine.tos.TOSV2;
import com.volcengine.tos.TOSV2ClientBuilder;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TOS 客户端配置
 */
@Configuration
@ConfigurationProperties(prefix = "volcengine.tos")
@Data
public class TosClientConfig {

    /**
     * TOS 服务地址
     */
    private String endpoint;

    /**
     * TOS 区域
     */
    private String region;

    /**
     * TOS 访问密钥
     */
    private String accessKey;

    /**
     * TOS 秘密密钥
     */
    private String secretKey;

    /**
     * TOS 桶名称
     */
    private String bucketName;

    /**
     * TOS 客户端
     */
    @Bean
    public TOSV2 tosClient() {
        return new TOSV2ClientBuilder()
                .build(region, endpoint, accessKey, secretKey);
    }
}
