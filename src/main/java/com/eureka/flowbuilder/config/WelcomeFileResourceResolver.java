package com.eureka.flowbuilder.config;

import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * 访问目录或未带文件名的路径时，自动解析为默认首页（如 index.html）。
 * <p>
 * 支持以下场景：
 * <ul>
 *   <li>{@code /deploy/P83jC4/} → {@code /deploy/P83jC4/index.html}</li>
 *   <li>{@code /deploy/P83jC4}  → {@code /deploy/P83jC4/index.html}（无尾斜杠）</li>
 *   <li>{@code /deploy/}        → {@code /deploy/index.html}（根目录）</li>
 * </ul>
 */
public class WelcomeFileResourceResolver extends PathResourceResolver {

    private final String welcomeFile;

    public WelcomeFileResourceResolver(String welcomeFile) {
        this.welcomeFile = welcomeFile;
    }

    @Override
    protected Resource getResource(String resourcePath, Resource location) throws IOException {
        // 先尝试精确匹配
        Resource resource = super.getResource(resourcePath, location);
        if (resource != null && resource.exists() && resource.isReadable() && !isDirectorySafely(resource)) {
            return resource;
        }
        // 非可读文件（目录/不存在/空路径）→ 尝试 welcome 文件
        return resolveWelcomeFile(resourcePath, location);
    }

    private Resource resolveWelcomeFile(String resourcePath, Resource location) throws IOException {
        // 归一化：确保以 "/" 结尾
        String normalized = resourcePath.endsWith("/") || resourcePath.isEmpty()
                ? resourcePath + "/"
                : resourcePath + "/";
        // 拼接 welcome 文件名，剔除前导 "/" 避免破坏相对路径解析
        String filePath = normalized + welcomeFile;
        if (filePath.startsWith("/")) {
            filePath = filePath.substring(1);
        }
        Resource welcomeResource = super.getResource(filePath, location);
        if (welcomeResource != null && welcomeResource.exists() && welcomeResource.isReadable()) {
            return welcomeResource;
        }
        return null;
    }

    /**
     * 安全判断是否为目录，避免 {@code getFile()} 抛异常导致误判。
     */
    private boolean isDirectorySafely(Resource resource) {
        try {
            return resource.getFile() != null && resource.getFile().isDirectory();
        } catch (Exception e) {
            // 无法判断 → 当作目录处理，交给 welcome 文件逻辑
            return true;
        }
    }
}
