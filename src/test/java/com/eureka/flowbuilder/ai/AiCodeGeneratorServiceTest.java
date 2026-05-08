package com.eureka.flowbuilder.ai;

import com.eureka.flowbuilder.ai.model.HtmlCodeResult;
import com.eureka.flowbuilder.ai.model.MultiFileCodeResult;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AiCodeGeneratorServiceTest {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    @Test
    void generateHtmlCode() {
        HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode("做一个“我永远喜欢爱莉希雅”的博客，不超过30行");
        assertNotNull(result);
    }

    @Test
    void generateMultiFileCode() {
        MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode("生成“永不放弃就是我的魔法”的页面，不要超过60行");
        assertNotNull(result);
    }
}