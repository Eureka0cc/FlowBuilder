package com.eureka.flowbuilder.config;

import com.eureka.flowbuilder.constant.AppConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Slf4j
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 覆盖所有请求
        registry.addMapping("/**")
                // 允许发送 Cookie
                .allowCredentials(true)
                // 放行哪些域名（必须用 patterns，否则 * 会和 allowCredentials 冲突）
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("*");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 获取绝对路径并转换为 URI 格式
        File outputDir = new File(AppConstant.CODE_OUTPUT_ROOT_DIR).getAbsoluteFile();
        File deployDir = new File(AppConstant.CODE_DEPLOY_ROOT_DIR).getAbsoluteFile();

        // 使用 toURI() 方法确保路径格式正确
        String outputUri = outputDir.toURI().toString();
        String deployUri = deployDir.toURI().toString();

        log.info("配置静态资源映射 - 输出目录: {}", outputUri);
        log.info("配置静态资源映射 - 部署目录: {}", deployUri);

        // 配置静态资源映射（目录访问自动解析 index.html）
        registry.addResourceHandler("/static/**")
                .addResourceLocations(outputUri)
                .resourceChain(true)
                .addResolver(new WelcomeFileResourceResolver(AppConstant.DEPLOY_INDEX_FILE));

        // 配置部署目录的静态资源映射
        registry.addResourceHandler("/deploy/**")
                .addResourceLocations(deployUri)
                .resourceChain(true)
                .addResolver(new WelcomeFileResourceResolver(AppConstant.DEPLOY_INDEX_FILE));
    }
}
