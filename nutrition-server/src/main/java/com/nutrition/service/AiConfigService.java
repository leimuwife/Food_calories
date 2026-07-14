package com.nutrition.service;

import com.nutrition.param.AiConfigParam;
import com.nutrition.vo.AiConfigVO;

import java.util.List;

/**
 * AI配置服务接口
 */
public interface AiConfigService {

    /**
     * 获取所有配置列表
     */
    List<AiConfigVO> listAll();

    /**
     * 获取启用的配置
     */
    AiConfigVO getEnabledConfig();

    /**
     * 获取启用配置的原始实体（用于模型调用）
     */
    com.nutrition.entity.AiConfig getEnabledConfigEntity();

    /**
     * 新增配置
     */
    Long add(AiConfigParam param);

    /**
     * 更新配置
     */
    void update(Long id, AiConfigParam param);

    /**
     * 删除配置
     */
    void delete(Long id);

    /**
     * 切换启用配置
     */
    void enable(Long id);

    /**
     * 刷新缓存
     */
    void refreshCache();
}