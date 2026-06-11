package com.eureka.flowbuilder.ai.tools;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 工具管理器
 * 统一管理所有工具，提供根据名称获取工具的功能
 */
@Slf4j
@Component
public class ToolManager {

    /**
     * 工具名称到工具实例的映射
     */
    private final Map<String, BaseTool> toolMap = new HashMap<>();

    /**
     * 自动注入所有工具
     */
    @Resource
    private BaseTool[] tools;

    /**
     * 初始化工具映射
     */
    @PostConstruct
    public void initTools() {
        for (BaseTool tool : tools) {
            toolMap.put(tool.getToolName(), tool);
            log.info("注册工具: {} -> {}", tool.getToolName(), tool.getDisplayName());
        }
        log.info("工具管理器初始化完成，共注册 {} 个工具", toolMap.size());
    }

    /**
     * 根据工具名称获取工具实例
     *
     * @param toolName 工具英文名称（LangChain4j 的 {@link dev.langchain4j.agent.tool.Tool} 方法名）
     * @return 工具实例，未找到时返回 null
     */
    public BaseTool getTool(String toolName) {
        if (StrUtil.isBlank(toolName)) {
            return null;
        }
        return toolMap.get(toolName);
    }

    /**
     * 根据工具名称解析工具；toolId 仅为 LangChain4j 生成的调用实例 ID，不能用于查找注册表
     *
     * @param toolName 工具方法名，如 writeFile
     * @param toolId   单次调用的实例 ID（仅用于日志）
     * @return 工具实例，未找到时返回 null
     */
    public BaseTool resolveTool(String toolName, String toolId) {
        BaseTool tool = getTool(toolName);
        if (tool == null && StrUtil.isNotBlank(toolId)) {
            log.warn("未找到工具 name={}, id={}（id 不是注册名，请使用 name 字段）", toolName, toolId);
        }
        return tool;
    }

    /**
     * 获取已注册的工具集合
     *
     * @return 工具实例集合
     */
    public BaseTool[] getAllTools() {
        return tools;
    }
}
