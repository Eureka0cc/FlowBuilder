package com.eureka.flowbuilder.config;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于 Spring Redis 的 ChatMemoryStore 实现。
 * <p>
 * langchain4j 的 RedisChatMemoryStore 在未设置 user 时会忽略 password，
 * 导致生产环境 Redis 开启密码认证后出现 NOAUTH 错误。
 * 这里复用 Spring Data Redis 的连接配置，避免重复维护 Jedis 认证逻辑。
 */
@Component
public class SpringRedisChatMemoryStore implements ChatMemoryStore {

    private static final String KEY_PREFIX = "chat:memory:";

    private final StringRedisTemplate redisTemplate;
    private final long ttlSeconds;

    public SpringRedisChatMemoryStore(StringRedisTemplate redisTemplate,
                                      @Value("${spring.data.redis.ttl:3600}") long ttlSeconds) {
        this.redisTemplate = redisTemplate;
        this.ttlSeconds = ttlSeconds;
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String json = redisTemplate.opsForValue().get(toRedisKey(memoryId));
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        return ChatMessageDeserializer.messagesFromJson(json);
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String json = ChatMessageSerializer.messagesToJson(messages);
        String key = toRedisKey(memoryId);
        if (ttlSeconds > 0) {
            redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(ttlSeconds));
        } else {
            redisTemplate.opsForValue().set(key, json);
        }
    }

    @Override
    public void deleteMessages(Object memoryId) {
        redisTemplate.delete(toRedisKey(memoryId));
    }

    private String toRedisKey(Object memoryId) {
        if (memoryId == null || memoryId.toString().isBlank()) {
            throw new IllegalArgumentException("memoryId cannot be null or empty");
        }
        return KEY_PREFIX + memoryId;
    }
}
