package com.nutrition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nutrition.entity.AiConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI配置数据访问层
 */
@Mapper
public interface AiConfigMapper extends BaseMapper<AiConfig> {
}