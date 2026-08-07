package com.nutrition.service;

import com.nutrition.entity.AiConfig;

import java.util.List;

/**
 * AI配置管理服务接口
 */
public interface AiConfigService {

    /**
     * 获取所有AI配置列表
     */
    List<AiConfig> listAll();

    /**
     * 根据ID获取AI配置
     */
    AiConfig getById(Long id);

    /**
     * 新增AI配置
     */
    Long add(AiConfig config);

    /**
     * 修改AI配置
     */
    void update(Long id, AiConfig config);

    /**
     * 删除AI配置
     */
    void delete(Long id);

    /**
     * 启用AI配置
     */
    void enable(Long id);

    /**
     * 禁用AI配置
     */
    void disable(Long id);

    /**
     * 测试AI配置连通性
     */
    String testConnection(Long id, String message);

    /**
     * 获取当前启用的AI配置
     */
    AiConfig getEnabledConfig();
}
