package com.nutrition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nutrition.dto.FeedCountUpdateDTO;
import com.nutrition.entity.Feed;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 轻友圈动态 Mapper 接口
 */
@Mapper
public interface FeedMapper extends BaseMapper<Feed> {

    /**
     * 批量更新动态计数（点赞数、评论数）
     * 使用 CASE WHEN 单 SQL 批量更新，大幅降低 DB 交互次数
     *
     * @param updateList 更新数据列表
     * @return 更新成功的记录数
     */
    int batchUpdateFeedCount(@Param("updateList") List<FeedCountUpdateDTO> updateList);

    /**
     * 分页查询动态列表（按创建时间降序）
     * 查询条件：delete_flag = 0（仅查询未删除的动态）
     *
     * @param offset 偏移量
     * @param limit  每页条数
     * @return 动态列表
     */
    List<Feed> selectFeedList(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 查询动态总数（仅统计未删除的动态）
     *
     * @return 动态总数
     */
    Long selectFeedCount();
}
