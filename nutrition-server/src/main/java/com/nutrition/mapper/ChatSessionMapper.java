package com.nutrition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nutrition.entity.NutritionistChat;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI聊天会话Mapper
 * 对应数据库表 chat_session
 */
@Mapper
public interface ChatSessionMapper extends BaseMapper<NutritionistChat> {
}
