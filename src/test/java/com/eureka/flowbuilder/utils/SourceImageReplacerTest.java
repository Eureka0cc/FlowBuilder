package com.eureka.flowbuilder.utils;

import com.eureka.flowbuilder.model.vo.ReplaceImageVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SourceImageReplacerTest {
    @TempDir
    Path tempDir;

    @Test
    void replacesExactUrlsAndSkipsGeneratedDirectories() throws Exception {
        Path source = Files.createDirectories(tempDir.resolve("src"));
        Path vue = source.resolve("App.vue");
        Files.writeString(vue, "<img src=\"old-url\"><img src=\"old-url-extra\">old-url");
        Path dist = Files.createDirectories(source.resolve("dist"));
        Path generated = dist.resolve("bundle.js");
        Files.writeString(generated, "old-url");
        Path binary = source.resolve("image.png");
        Files.write(binary, new byte[]{0, 1, 2});

        ReplaceImageVO result = SourceImageReplacer.replace(source, "old-url", "new-url");

        assertEquals(3, result.getReplacementCount());
        assertEquals(java.util.List.of("App.vue"), result.getAffectedPaths());
        assertEquals("<img src=\"new-url\"><img src=\"new-url-extra\">new-url", Files.readString(vue));
        assertEquals("old-url", Files.readString(generated));
    }

    @Test
    void ignoresUnsupportedTextFiles() throws Exception {
        Path markdown = tempDir.resolve("README.md");
        Files.writeString(markdown, "old-url");

        ReplaceImageVO result = SourceImageReplacer.replace(tempDir, "old-url", "new-url");

        assertEquals(0, result.getReplacementCount());
        assertEquals("old-url", Files.readString(markdown));
    }
}
