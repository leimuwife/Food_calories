package com.nutrition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nutrition.entity.FeedLike;
import org.apache.ibatis.annotations.Mapper;

/**
 * 动态点赞记录 Mapper 接口
 */
@Mapper
public interface FeedLikeMapper extends BaseMapper<FeedLike> {
}
