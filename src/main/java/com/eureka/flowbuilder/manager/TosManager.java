package com.eureka.flowbuilder.manager;

import com.eureka.flowbuilder.config.TosClientConfig;
import com.volcengine.tos.TOSV2;
import com.volcengine.tos.TosClientException;
import com.volcengine.tos.TosServerException;
import com.volcengine.tos.model.object.PutObjectFromFileInput;
import com.volcengine.tos.model.object.PutObjectFromFileOutput;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * TOS对象存储管理器
 */
@Component
@Slf4j
public class TosManager {

    @Resource
    private TosClientConfig tosClientConfig;

    @Resource
    private TOSV2 tosClient;

    /**
     * 上传文件到 TOS
     *
     * @param key  对象键
     * @param file 文件
     * @return 上传结果
     */
    public PutObjectFromFileOutput putObject(String key, File file) {
        PutObjectFromFileInput input = new PutObjectFromFileInput()
                .setBucket(tosClientConfig.getBucketName())
                .setKey(key)
                .setFilePath(file.getAbsolutePath());
        return tosClient.putObjectFromFile(input);
    }

    /**
     * 上传文件到 TOS 并返回访问 URL
     *
     * @param key  对象键
     * @param file 文件
     * @return 访问URL，失败返回null
     */
    public String uploadFile(String key, File file) {
        try {
            putObject(key, file);
            String url = String.format("https://%s.%s/%s",
                    tosClientConfig.getBucketName(),
                    tosClientConfig.getEndpoint(),
                    key);
            log.info("文件上传TOS成功: {} -> {}", file.getName(), url);
            return url;
        } catch (TosClientException e) {
            log.error("TOS客户端异常: {}", e.getMessage());
            return null;
        } catch (TosServerException e) {
            log.error("TOS服务端异常: statusCode={}, code={}, requestId={}",
                    e.getStatusCode(), e.getCode(), e.getRequestID());
            return null;
        }
    }
}
