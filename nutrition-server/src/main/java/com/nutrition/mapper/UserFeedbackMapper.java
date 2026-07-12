package com.nutrition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nutrition.entity.UserFeedback;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户问题反馈数据访问层
 * 继承 MyBatis-Plus BaseMapper，提供基础 CRUD 操作
 */
@Mapper
public interface UserFeedbackMapper extends BaseMapper<UserFeedback> {
}