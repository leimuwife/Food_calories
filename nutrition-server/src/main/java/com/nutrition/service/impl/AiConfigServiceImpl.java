package com.nutrition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nutrition.common.BusinessException;
import com.nutrition.entity.AiConfig;
import com.nutrition.enums.BizMsgEnum;
import com.nutrition.mapper.AiConfigMapper;
import com.nutrition.param.AiConfigParam;
import com.nutrition.service.AiConfigService;
import com.nutrition.util.AesUtil;
import com.nutrition.util.RedisCache;
import com.nutrition.vo.AiConfigVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AI配置服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiConfigServiceImpl extends ServiceImpl<AiConfigMapper, AiConfig> implements AiConfigService {

    private final AesUtil aesUtil;
    private final RedisCache redisCache;

    private static final String CACHE_KEY_ENABLED_CONFIG = "ai:config:enabled";

    @Override
    public List<AiConfigVO> listAll() {
        List<AiConfig> list = this.list(new LambdaQueryWrapper<AiConfig>()
                .eq(AiConfig::getDeleteFlag, 0)
                .orderByDesc(AiConfig::getCreateTime));

        return list.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public AiConfigVO getEnabledConfig() {
        AiConfig config = getEnabledConfigEntity();
        return config != null ? convertToVO(config) : null;
    }

    @Override
    public AiConfig getEnabledConfigEntity() {
        AiConfig config = redisCache.get(CACHE_KEY_ENABLED_CONFIG, AiConfig.class);
        if (config != null) {
            return config;
        }

        config = this.getOne(new LambdaQueryWrapper<AiConfig>()
                .eq(AiConfig::getIsEnabled, 1)
                .eq(AiConfig::getDeleteFlag, 0));

        if (config != null) {
            redisCache.set(CACHE_KEY_ENABLED_CONFIG, config);
        }

        return config;
    }

    @Override
    @Transactional
    public Long add(AiConfigParam param) {
        AiConfig config = new AiConfig();
        config.setModelName(param.getModelName());
        config.setModelType(param.getModelType());
        config.setApiUrl(param.getApiUrl());
        config.setApiKey(aesUtil.encrypt(param.getApiKey()));
        config.setNickname(param.getNickname());
        config.setSystemPrompt(param.getSystemPrompt());
        config.setTemperature(param.getTemperature());
        config.setMaxTokens(param.getMaxTokens());
        config.setIsEnabled(0);
        config.setDeleteFlag(0);

        this.save(config);
        log.info("新增AI配置: id={}, modelName={}", config.getId(), config.getModelName());

        return config.getId();
    }

    @Override
    @Transactional
    public void update(Long id, AiConfigParam param) {
        AiConfig config = this.getById(id);
        if (config == null || config.getDeleteFlag() == 1) {
            throw new BusinessException(BizMsgEnum.AI_CONFIG_NOT_FOUND);
        }

        config.setModelName(param.getModelName());
        config.setModelType(param.getModelType());
        config.setApiUrl(param.getApiUrl());

        if (param.getApiKey() != null && !param.getApiKey().isEmpty()) {
            config.setApiKey(aesUtil.encrypt(param.getApiKey()));
        }

        config.setNickname(param.getNickname());
        config.setSystemPrompt(param.getSystemPrompt());
        config.setTemperature(param.getTemperature());
        config.setMaxTokens(param.getMaxTokens());

        this.updateById(config);
        log.info("更新AI配置: id={}, modelName={}", id, param.getModelName());

        if (config.getIsEnabled() == 1) {
            refreshCache();
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        AiConfig config = this.getById(id);
        if (config == null || config.getDeleteFlag() == 1) {
            throw new BusinessException(BizMsgEnum.AI_CONFIG_NOT_FOUND);
        }

        if (config.getIsEnabled() == 1) {
            throw new BusinessException(BizMsgEnum.AI_CONFIG_ENABLED_CANNOT_DELETE);
        }

        config.setDeleteFlag(1);
        this.updateById(config);
        log.info("删除AI配置: id={}", id);
    }

    @Override
    @Transactional
    public void enable(Long id) {
        AiConfig config = this.getById(id);
        if (config == null || config.getDeleteFlag() == 1) {
            throw new BusinessException(BizMsgEnum.AI_CONFIG_NOT_FOUND);
        }

        if (config.getIsEnabled() == 1) {
            return;
        }

        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<AiConfig> updateWrapper =
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
        updateWrapper.eq(AiConfig::getIsEnabled, 1)
                .set(AiConfig::getIsEnabled, 0);
        this.update(null, updateWrapper);

        config.setIsEnabled(1);
        this.updateById(config);

        log.info("启用AI配置: id={}, modelName={}", id, config.getModelName());

        refreshCache();
    }

    @Override
    public void refreshCache() {
        redisCache.delete(CACHE_KEY_ENABLED_CONFIG);
        getEnabledConfigEntity();
        log.info("AI配置缓存已刷新");
    }

    private AiConfigVO convertToVO(AiConfig config) {
        AiConfigVO vo = new AiConfigVO();
        vo.setId(config.getId());
        vo.setModelName(config.getModelName());
        vo.setModelType(config.getModelType());
        vo.setApiUrl(config.getApiUrl());
        vo.setApiKey(aesUtil.maskApiKey(aesUtil.decrypt(config.getApiKey())));
        vo.setNickname(config.getNickname());
        vo.setSystemPrompt(config.getSystemPrompt());
        vo.setTemperature(config.getTemperature());
        vo.setMaxTokens(config.getMaxTokens());
        vo.setIsEnabled(config.getIsEnabled());
        return vo;
    }
}