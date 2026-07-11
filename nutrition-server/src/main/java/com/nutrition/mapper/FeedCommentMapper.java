package com.nutrition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nutrition.entity.FeedComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 动态评论记录 Mapper 接口
 */
@Mapper
public interface FeedCommentMapper extends BaseMapper<FeedComment> {

    /**
     * 查询动态的评论列表（按创建时间升序，最多返回前3条）
     *
     * @param feedId 动态ID
     * @return 评论列表（最多3条）
     */
    List<FeedComment> selectCommentsByFeedId(@Param("feedId") Long feedId);
}
