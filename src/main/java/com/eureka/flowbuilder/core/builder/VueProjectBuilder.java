package com.eureka.flowbuilder.core.builder;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.eureka.flowbuilder.constant.AppConstant;
import com.eureka.flowbuilder.model.vo.VueBuildStatusVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class VueProjectBuilder {

    public static final String BUILD_STATUS_FILE = ".vue-build-status.json";

    public static final String STATUS_NONE = "none";
    public static final String STATUS_BUILDING = "building";
    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_FAILED = "failed";

    private static final String VITE_ENTRY_SCRIPT = "<script type=\"module\" src=\"/src/main.js\"></script>";

    /**
     * 同步标记构建开始（在 SSE 结束前写入，避免前端轮询误判为上一次构建成功）
     *
     * @param projectPath 项目路径
     */
    public void markBuildStarting(String projectPath) {
        writeBuildStatus(projectPath, STATUS_BUILDING, false, "正在重新构建预览...");
    }

    /**
     * 异步构建项目（不阻塞主流程）
     *
     * @param projectPath 项目路径
     */
    public void buildProjectAsync(String projectPath) {
        Thread.ofVirtual().name("vue-builder-" + System.currentTimeMillis()).start(() -> {
            try {
                buildProject(projectPath);
            } catch (Exception e) {
                log.error("异步构建 Vue 项目时发生异常: {}", e.getMessage(), e);
                writeBuildStatus(projectPath, STATUS_FAILED, false,
                        "构建过程发生异常: " + e.getMessage());
            }
        });
    }

    /**
     * 构建 Vue 项目
     *
     * @param projectPath 项目根目录路径
     * @return 是否构建成功
     */
    public boolean buildProject(String projectPath) {
        File projectDir = new File(projectPath);
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            log.error("项目目录不存在: {}", projectPath);
            writeBuildStatus(projectPath, STATUS_FAILED, false, "项目目录不存在");
            return false;
        }
        File packageJson = new File(projectDir, "package.json");
        if (!packageJson.exists()) {
            log.error("package.json 文件不存在: {}", packageJson.getAbsolutePath());
            writeBuildStatus(projectPath, STATUS_FAILED, false, "package.json 不存在");
            return false;
        }

        writeBuildStatus(projectPath, STATUS_BUILDING, false, "正在安装依赖并构建项目...");

        if (!ensureIndexHtmlEntryScript(projectDir)) {
            log.error("index.html 缺少 Vite 入口脚本且自动修复失败: {}", projectPath);
            writeBuildStatus(projectPath, STATUS_FAILED, false,
                    "index.html 缺少 <script type=\"module\" src=\"/src/main.js\"></script> 入口");
            return false;
        }

        log.info("开始构建 Vue 项目: {}", projectPath);

        CommandResult installResult = executeNpmInstall(projectDir);
        if (!installResult.success()) {
            log.error("npm install 执行失败");
            writeBuildStatus(projectPath, STATUS_FAILED, false,
                    "npm install 失败: " + truncateOutput(installResult.output()));
            return false;
        }

        CommandResult buildResult = executeNpmBuild(projectDir);
        if (!buildResult.success()) {
            log.error("npm run build 执行失败");
            writeBuildStatus(projectPath, STATUS_FAILED, false,
                    "npm run build 失败: " + truncateOutput(buildResult.output()));
            return false;
        }

        File distDir = new File(projectDir, "dist");
        if (!distDir.exists()) {
            log.error("构建完成但 dist 目录未生成: {}", distDir.getAbsolutePath());
            writeBuildStatus(projectPath, STATUS_FAILED, false, "构建完成但未生成 dist 目录");
            return false;
        }

        if (!hasBundledJsAssets(distDir)) {
            log.error("构建完成但 dist 中未包含打包后的 JS 资源: {}", distDir.getAbsolutePath());
            writeBuildStatus(projectPath, STATUS_FAILED, false,
                    "构建产物无效：dist 目录中未找到打包后的 JS 文件，请检查 index.html 入口与源码语法");
            return false;
        }

        log.info("Vue 项目构建成功，dist 目录: {}", distDir.getAbsolutePath());
        writeBuildStatus(projectPath, STATUS_SUCCESS, true, "构建成功，预览已就绪");
        return true;
    }

    /**
     * 读取 Vue 工程预览构建状态
     */
    public VueBuildStatusVO getBuildStatus(Long appId) {
        String projectPath = AppConstant.getVueProjectDirPath(appId);
        File projectDir = new File(projectPath);
        VueBuildStatusVO statusVO = readBuildStatusFile(projectDir);
        if (statusVO == null) {
            statusVO = new VueBuildStatusVO();
            if (!projectDir.exists()) {
                statusVO.setStatus(STATUS_NONE);
                statusVO.setReady(false);
                statusVO.setMessage("项目尚未生成");
                return statusVO;
            }
            File distDir = new File(projectDir, "dist");
            boolean ready = hasBundledJsAssets(distDir);
            statusVO.setStatus(ready ? STATUS_SUCCESS : STATUS_NONE);
            statusVO.setReady(ready);
            statusVO.setMessage(ready ? "预览已就绪" : "尚未完成构建");
        }
        if (Boolean.TRUE.equals(statusVO.getReady())) {
            statusVO.setStatus(STATUS_SUCCESS);
        }
        return statusVO;
    }

    /**
     * 确保 index.html 包含 Vite 入口脚本
     */
    boolean ensureIndexHtmlEntryScript(File projectDir) {
        File indexHtml = new File(projectDir, "index.html");
        if (!indexHtml.exists()) {
            return false;
        }
        String content = FileUtil.readUtf8String(indexHtml);
        if (content.contains("/src/main.js")) {
            return true;
        }
        String fixedContent;
        if (content.contains("</body>")) {
            fixedContent = content.replace("</body>", "  " + VITE_ENTRY_SCRIPT + "\n</body>");
        } else if (content.contains("</html>")) {
            fixedContent = content.replace("</html>", "  " + VITE_ENTRY_SCRIPT + "\n</html>");
        } else {
            fixedContent = content + "\n" + VITE_ENTRY_SCRIPT + "\n";
        }
        FileUtil.writeUtf8String(fixedContent, indexHtml);
        log.info("已自动为 index.html 补充 Vite 入口脚本: {}", indexHtml.getAbsolutePath());
        return true;
    }

    /**
     * 检查 dist 目录是否包含打包后的 JS 资源
     */
    boolean hasBundledJsAssets(File distDir) {
        if (distDir == null || !distDir.exists() || !distDir.isDirectory()) {
            return false;
        }
        List<File> jsFiles = FileUtil.loopFiles(distDir, file ->
                file.isFile() && file.getName().endsWith(".js"));
        return !jsFiles.isEmpty();
    }

    void writeBuildStatus(String projectPath, String status, boolean ready, String message) {
        File statusFile = new File(projectPath, BUILD_STATUS_FILE);
        JSONObject json = new JSONObject();
        json.set("status", status);
        json.set("ready", ready);
        json.set("message", message);
        json.set("timestamp", System.currentTimeMillis());
        FileUtil.writeUtf8String(json.toString(), statusFile);
    }

    private VueBuildStatusVO readBuildStatusFile(File projectDir) {
        File statusFile = new File(projectDir, BUILD_STATUS_FILE);
        if (!statusFile.exists()) {
            return null;
        }
        try {
            String json = FileUtil.readUtf8String(statusFile);
            if (StrUtil.isBlank(json)) {
                return null;
            }
            JSONObject obj = JSONUtil.parseObj(json);
            VueBuildStatusVO statusVO = new VueBuildStatusVO();
            statusVO.setStatus(obj.getStr("status", STATUS_NONE));
            statusVO.setReady(obj.getBool("ready", false));
            statusVO.setMessage(obj.getStr("message"));
            statusVO.setTimestamp(obj.getLong("timestamp"));
            return statusVO;
        } catch (Exception e) {
            log.warn("读取构建状态文件失败: {}", statusFile.getAbsolutePath(), e);
            return null;
        }
    }

    private CommandResult executeNpmInstall(File projectDir) {
        log.info("执行 npm install...");
        String command = String.format("%s install", buildCommand("npm"));
        return executeCommand(projectDir, command, 300);
    }

    private CommandResult executeNpmBuild(File projectDir) {
        log.info("执行 npm run build...");
        String command = String.format("%s run build", buildCommand("npm"));
        return executeCommand(projectDir, command, 180);
    }

    private CommandResult executeCommand(File workingDir, String command, int timeoutSeconds) {
        try {
            log.info("在目录 {} 中执行命令: {}", workingDir.getAbsolutePath(), command);
            ProcessBuilder processBuilder = new ProcessBuilder(command.split("\\s+"));
            processBuilder.directory(workingDir);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new CommandResult(false, output + "\n命令执行超时（" + timeoutSeconds + "秒）");
            }
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                log.info("命令执行成功: {}", command);
                return new CommandResult(true, output);
            }
            log.error("命令执行失败，退出码: {}\n{}", exitCode, output);
            return new CommandResult(false, output);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("执行命令失败: {}, 错误信息: {}", command, e.getMessage());
            return new CommandResult(false, e.getMessage());
        }
    }

    private String truncateOutput(String output) {
        if (StrUtil.isBlank(output)) {
            return "无详细输出";
        }
        String trimmed = output.trim();
        if (trimmed.length() <= 500) {
            return trimmed;
        }
        return trimmed.substring(trimmed.length() - 500);
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    private String buildCommand(String baseCommand) {
        if (isWindows()) {
            return baseCommand + ".cmd";
        }
        return baseCommand;
    }

    private record CommandResult(boolean success, String output) {
    }
}
