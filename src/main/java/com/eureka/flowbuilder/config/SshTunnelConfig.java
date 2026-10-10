package com.eureka.flowbuilder.config;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.ArrayList;
import java.util.List;

/**
 * 生产环境 SSH 隧道配置。
 * 在应用启动时自动建立 SSH 端口转发隧道，
 * 将远程数据库/Redis 端口映射到本地，避免数据库直接暴露在公网。
 *
 * 实现 SmartLifecycle 并设置最低相位（Integer.MIN_VALUE），
 * 确保隧道在 DataSource / Redis 连接池初始化之前就绪。
 */
@Slf4j
@Configuration
@Profile("prod")
public class SshTunnelConfig implements SmartLifecycle {

    @Value("${ssh.tunnel.host}")
    private String sshHost;

    @Value("${ssh.tunnel.port:22}")
    private int sshPort;

    @Value("${ssh.tunnel.user}")
    private String sshUser;

    @Value("${ssh.tunnel.password:}")
    private String sshPassword;

    @Value("${ssh.tunnel.private-key-path:}")
    private String privateKeyPath;

    /**
     * 隧道配置，格式：localPort:remoteHost:remotePort
     * 多个用英文逗号分隔。
     * 示例：3307:127.0.0.1:3306,6380:127.0.0.1:6379
     */
    @Value("${ssh.tunnel.tunnels}")
    private String tunnelsConfig;

    @Value("${ssh.tunnel.connect-timeout:10000}")
    private int connectTimeout;

    @Value("${ssh.tunnel.max-retries:3}")
    private int maxRetries;

    @Value("${ssh.tunnel.retry-delay-ms:3000}")
    private long retryDelayMs;

    private final List<Session> sessions = new ArrayList<>();
    private volatile boolean running = false;

    @Override
    public int getPhase() {
        // 最低相位，确保在所有其他 bean 之前启动
        return Integer.MIN_VALUE;
    }

    @Override
    public void start() {
        log.info("==================== SSH 隧道启动 ====================");
        log.info("SSH 主机: {}:{}", sshHost, sshPort);
        log.info("SSH 用户: {}", sshUser);
        log.info("隧道数量: {} 条", tunnelsConfig.split(",").length);

        JSch jSch = new JSch();

        // 如果配置了私钥路径，则使用私钥认证
        if (privateKeyPath != null && !privateKeyPath.isBlank()) {
            try {
                jSch.addIdentity(privateKeyPath);
                log.info("已加载 SSH 私钥: {}", privateKeyPath);
            } catch (Exception e) {
                log.error("加载 SSH 私钥失败: {} - {}", privateKeyPath, e.getMessage());
                throw new RuntimeException("SSH 私钥加载失败", e);
            }
        }

        String[] tunnelEntries = tunnelsConfig.split(",");
        for (String entry : tunnelEntries) {
            entry = entry.trim();
            if (entry.isEmpty()) continue;

            String[] parts = entry.split(":");
            if (parts.length != 3) {
                log.error("隧道配置格式错误，应为 localPort:remoteHost:remotePort，实际: {}", entry);
                throw new IllegalArgumentException("隧道配置格式错误: " + entry);
            }

            int localPort = Integer.parseInt(parts[0].trim());
            String remoteHost = parts[1].trim();
            int remotePort = Integer.parseInt(parts[2].trim());

            Session session = connectWithRetry(jSch, localPort, remoteHost, remotePort);
            sessions.add(session);
        }

        running = true;
        log.info("==================== SSH 隧道全部就绪 ({} 条) ====================", sessions.size());
    }

    /**
     * 带重试的 SSH 连接 + 端口转发。
     */
    private Session connectWithRetry(JSch jSch, int localPort, String remoteHost, int remotePort) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("建立 SSH 隧道 [{}/{}]: localhost:{} -> {}:{}",
                        attempt, maxRetries, localPort, remoteHost, remotePort);

                Session session = jSch.getSession(sshUser, sshHost, sshPort);

                // 密码认证（仅在未配置私钥时使用）
                if (privateKeyPath == null || privateKeyPath.isBlank()) {
                    session.setPassword(sshPassword);
                }

                session.setConfig("StrictHostKeyChecking", "no");
                session.setConfig("TCPKeepAlive", "yes");
                session.setConfig("ServerAliveInterval", "30000");
                session.setTimeout(connectTimeout);
                session.connect();

                // 建立本地端口转发
                int assignedPort = session.setPortForwardingL(localPort, remoteHost, remotePort);

                log.info("SSH 隧道已建立: localhost:{} -> {}:{} (会话: {})",
                        assignedPort, remoteHost, remotePort, session.getHost());
                return session;

            } catch (Exception e) {
                log.error("SSH 隧道建立失败 (第 {} 次尝试): {}:{} -> {}:{} - {}",
                        attempt, localPort, remoteHost, remotePort, e.getMessage());
                if (attempt >= maxRetries) {
                    throw new RuntimeException(
                            String.format("SSH 隧道建立失败（已重试 %d 次）: localhost:%d -> %s:%d",
                                    maxRetries, localPort, remoteHost, remotePort), e);
                }
                try {
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("SSH 隧道建立被中断", ie);
                }
            }
        }
        throw new RuntimeException("无法建立 SSH 隧道");
    }

    @Override
    public void stop() {
        log.info("正在关闭 SSH 隧道（共 {} 条）...", sessions.size());
        for (Session session : sessions) {
            try {
                if (session.isConnected()) {
                    session.disconnect();
                    log.info("SSH 隧道已关闭: {}", session.getHost());
                }
            } catch (Exception e) {
                log.warn("关闭 SSH 隧道时出错: {}", e.getMessage());
            }
        }
        sessions.clear();
        running = false;
        log.info("SSH 隧道已全部关闭");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    /**
     * SmartLifecycle 默认不自动启动，需要显式返回 true。
     */
    @Override
    public boolean isAutoStartup() {
        return true;
    }

    /**
     * 关闭时也按相位顺序：最后关闭（最高相位）。
     */
    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }
}
