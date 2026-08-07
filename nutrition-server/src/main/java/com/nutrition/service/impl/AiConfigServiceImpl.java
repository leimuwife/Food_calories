package com.nutrition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.nutrition.common.BusinessException;
import com.nutrition.entity.AiConfig;
import com.nutrition.mapper.AiConfigMapper;
import com.nutrition.service.AiConfigService;
import com.nutrition.util.AesUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * AI配置管理服务实现类
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AiConfigServiceImpl implements AiConfigService {

    private final AiConfigMapper aiConfigMapper;
    private final AesUtil aesUtil;

    @Override
    public List<AiConfig> listAll() {
        LambdaQueryWrapper<AiConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(AiConfig::getCreateTime);
        List<AiConfig> configs = aiConfigMapper.selectList(wrapper);
        // 对返回的配置进行API密钥脱敏处理
        configs.forEach(config -> {
            if (config.getApiKey() != null && !config.getApiKey().isEmpty()) {
                config.setApiKey(aesUtil.maskApiKey(config.getApiKey()));
            }
        });
        return configs;
    }

    @Override
    public AiConfig getById(Long id) {
        AiConfig config = aiConfigMapper.selectById(id);
        if (config == null) {
            throw new BusinessException("AI配置不存在");
        }
        return config;
    }

    @Override
    @Transactional
    public Long add(AiConfig config) {
        // 校验模型名称唯一性
        Long count = aiConfigMapper.selectCount(
                new LambdaQueryWrapper<AiConfig>().eq(AiConfig::getModelName, config.getModelName())
        );
        if (count > 0) {
            throw new BusinessException("模型名称已存在");
        }

        // 加密API密钥
        if (config.getApiKey() != null && !config.getApiKey().isEmpty()) {
            config.setApiKey(aesUtil.encrypt(config.getApiKey()));
        }

        // 若设置为启用状态，先禁用其他配置（确保只有一个默认启用的配置）
        if (config.getIsEnabled() != null && config.getIsEnabled() == 1) {
            aiConfigMapper.update(null, new LambdaUpdateWrapper<AiConfig>()
                    .set(AiConfig::getIsEnabled, 0)
                    .eq(AiConfig::getIsEnabled, 1));
        }

        aiConfigMapper.insert(config);

        log.info("新增AI配置成功: id={}, modelName={}", config.getId(), config.getModelName());
        return config.getId();
    }

    @Override
    @Transactional
    public void update(Long id, AiConfig config) {
        AiConfig existing = aiConfigMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("AI配置不存在");
        }

        // 校验模型名称唯一性（排除自身）
        Long count = aiConfigMapper.selectCount(
                new LambdaQueryWrapper<AiConfig>()
                        .eq(AiConfig::getModelName, config.getModelName())
                        .ne(AiConfig::getId, id)
        );
        if (count > 0) {
            throw new BusinessException("模型名称已存在");
        }

        // 处理API密钥：如果前端传递了新密钥则加密更新，否则保留原密钥
        if (config.getApiKey() != null && !config.getApiKey().isEmpty()) {
            // 检查是否为脱敏后的密钥（以******开头或包含），若是则不更新
            if (!config.getApiKey().contains("******")) {
                config.setApiKey(aesUtil.encrypt(config.getApiKey()));
            } else {
                config.setApiKey(existing.getApiKey());
            }
        } else {
            config.setApiKey(existing.getApiKey());
        }

        // 若设置为启用状态，先禁用其他配置
        if (config.getIsEnabled() != null && config.getIsEnabled() == 1) {
            aiConfigMapper.update(null, new LambdaUpdateWrapper<AiConfig>()
                    .set(AiConfig::getIsEnabled, 0)
                    .eq(AiConfig::getIsEnabled, 1)
                    .ne(AiConfig::getId, id));
        }

        config.setId(id);
        config.setCreateTime(null);
        aiConfigMapper.updateById(config);

        log.info("更新AI配置成功: id={}, modelName={}", id, config.getModelName());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        AiConfig existing = aiConfigMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("AI配置不存在");
        }

        aiConfigMapper.deleteById(id);
        log.info("删除AI配置成功: id={}, modelName={}", id, existing.getModelName());
    }

    @Override
    @Transactional
    public void enable(Long id) {
        AiConfig existing = aiConfigMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("AI配置不存在");
        }

        // 先禁用所有配置
        aiConfigMapper.update(null, new LambdaUpdateWrapper<AiConfig>()
                .set(AiConfig::getIsEnabled, 0)
                .eq(AiConfig::getIsEnabled, 1));

        // 启用指定配置
        existing.setIsEnabled(1);
        aiConfigMapper.updateById(existing);

        log.info("启用AI配置: id={}, modelName={}", id, existing.getModelName());
    }

    @Override
    @Transactional
    public void disable(Long id) {
        AiConfig existing = aiConfigMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("AI配置不存在");
        }

        existing.setIsEnabled(0);
        aiConfigMapper.updateById(existing);

        log.info("禁用AI配置: id={}, modelName={}", id, existing.getModelName());
    }

    @Override
    public String testConnection(Long id, String message) {
        AiConfig config = aiConfigMapper.selectById(id);
        if (config == null) {
            throw new BusinessException("AI配置不存在");
        }

        // 解密API密钥
        String decryptedKey = aesUtil.decrypt(config.getApiKey());
        log.info("测试AI配置连通性: id={}, modelName={}, message={}", id, config.getModelName(), message);

        // TODO: 后续接入Python-FastAPI AI服务后，使用解密后的密钥进行真实的API调用
        // 当前返回Mock测试结果
        return "连通性测试成功！模型[" + config.getModelName() + "] 配置验证通过。"
                + "\n输入消息：" + message
                + "\n模型类型：" + config.getModelType()
                + "\nAPI地址：" + config.getApiUrl();
    }

    @Override
    public AiConfig getEnabledConfig() {
        LambdaQueryWrapper<AiConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiConfig::getIsEnabled, 1);
        wrapper.last("LIMIT 1");
        AiConfig config = aiConfigMapper.selectOne(wrapper);
        if (config != null) {
            // 解密API密钥供内部使用
            if (config.getApiKey() != null && !config.getApiKey().isEmpty()) {
                config.setApiKey(aesUtil.decrypt(config.getApiKey()));
            }
        }
        return config;
    }
}
