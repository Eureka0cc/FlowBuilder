package com.eureka.flowbuilder.core;

import com.eureka.flowbuilder.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AiCodeGeneratorFacadeTest {

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Test
    void generateAndSaveCode() {
        File file = aiCodeGeneratorFacade.generateAndSaveCode("我要一个“我永远喜欢爱莉希雅的”日历页面，要求要带有爱莉希雅的图片", CodeGenTypeEnum.MULTI_FILE, 1L);
        assertNotNull(file);
    }
}