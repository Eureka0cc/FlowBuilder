package com.eureka.flowbuilder;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.eureka.flowbuilder.mapper")
public class EurekaFlowBuilderApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaFlowBuilderApplication.class, args);
    }

}
