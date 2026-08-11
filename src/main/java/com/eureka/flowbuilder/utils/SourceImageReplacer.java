package com.eureka.flowbuilder.utils;

import com.eureka.flowbuilder.model.vo.ReplaceImageVO;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SourceImageReplacer {
    private static final Set<String> EXTENSIONS = Set.of(
            "html", "htm", "css", "scss", "less", "js", "jsx", "ts", "tsx", "vue", "json");
    private static final Set<String> SKIPPED_DIRECTORIES = Set.of("node_modules", "dist", ".git");
    private static final long MAX_TEXT_FILE_SIZE = 5L * 1024 * 1024;

    private SourceImageReplacer() {
    }

    public static ReplaceImageVO replace(Path sourceRoot, String oldUrl, String newUrl) throws IOException {
        if (oldUrl == null || oldUrl.isEmpty() || newUrl == null) {
            throw new IllegalArgumentException("新旧 URL 不能为空");
        }
        Path normalizedRoot = sourceRoot.toAbsolutePath().normalize();
        if (!Files.isDirectory(normalizedRoot)) {
            throw new IOException("源码目录不存在");
        }
        Path root = normalizedRoot.toRealPath();
        ReplaceImageVO result = new ReplaceImageVO();
        Pattern exactUrlPattern = Pattern.compile(
                "(?<![A-Za-z0-9._~:/?#\\[\\]@!$&'()*+,;=%-])"
                        + Pattern.quote(oldUrl)
                        + "(?![A-Za-z0-9._~:/?#\\[\\]@!$&'()*+,;=%-])");
        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                if (Files.isSymbolicLink(path)) {
                    continue;
                }
                Path normalized = path.toRealPath();
                if (!normalized.startsWith(root)) {
                    throw new IOException("检测到越界源码路径");
                }
                Path relative = root.relativize(normalized);
                if (isSkipped(relative) || !isAllowedExtension(normalized)
                        || Files.size(normalized) > MAX_TEXT_FILE_SIZE) {
                    continue;
                }
                byte[] bytes = Files.readAllBytes(normalized);
                if (containsNullByte(bytes)) {
                    continue;
                }
                String content = new String(bytes, StandardCharsets.UTF_8);
                Matcher matcher = exactUrlPattern.matcher(content);
                int count = countMatches(matcher);
                if (count == 0) {
                    continue;
                }
                String replaced = exactUrlPattern.matcher(content)
                        .replaceAll(Matcher.quoteReplacement(newUrl));
                Files.writeString(normalized, replaced, StandardCharsets.UTF_8,
                        StandardOpenOption.TRUNCATE_EXISTING);
                result.getAffectedPaths().add(relative.toString().replace('\\', '/'));
                result.setReplacementCount(result.getReplacementCount() + count);
            }
        }
        return result;
    }

    private static boolean isSkipped(Path relative) {
        for (Path part : relative) {
            if (SKIPPED_DIRECTORIES.contains(part.toString())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAllowedExtension(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot >= 0 && EXTENSIONS.contains(name.substring(dot + 1).toLowerCase());
    }

    private static boolean containsNullByte(byte[] bytes) {
        for (byte value : bytes) {
            if (value == 0) {
                return true;
            }
        }
        return false;
    }

    private static int countMatches(Matcher matcher) {
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }
}
