package com.nutrition.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nutrition.dto.FeedCountUpdateDTO;
import com.nutrition.entity.Feed;
import com.nutrition.param.FeedCommentParam;
import com.nutrition.param.FeedListParam;
import com.nutrition.param.FeedPublishParam;
import com.nutrition.vo.FeedListResultVO;

import java.util.List;
import java.util.Map;

/**
 * 轻友圈动态服务接口
 * 提供动态发布、查询、点赞、评论等业务操作
 */
public interface FeedService extends IService<Feed> {

    /**
     * 发布动态
     *
     * @param userId 用户ID
     * @param param  发布动态请求参数
     * @return 动态ID
     */
    Long publishFeed(Long userId, FeedPublishParam param);

    /**
     * 点赞/取消点赞动态
     * 逻辑：SETNX防重 → INCR/DECR计数 → 插入/删除明细记录
     * 不实时更新MySQL动态表，仅操作Redis和明细表
     *
     * @param userId 用户ID
     * @param feedId 动态ID
     * @return 操作结果：isLiked（是否已点赞）、likeCount（当前点赞数）
     */
    Map<String, Object> toggleLike(Long userId, Long feedId);

    /**
     * 添加评论
     * 逻辑：文本审核 → 插入评论明细 → INCR计数
     * 不实时更新MySQL动态表，仅操作Redis和明细表
     *
     * @param userId 用户ID
     * @param feedId 动态ID
     * @param param  评论请求参数
     * @return 操作结果：commentCount（当前评论数）
     */
    Map<String, Object> addComment(Long userId, Long feedId, FeedCommentParam param);

    /**
     * 批量更新动态计数（点赞数、评论数）
     * 使用 CASE WHEN 单 SQL 批量更新，用于定时任务同步 Redis 计数到 MySQL
     *
     * @param updateList 更新数据列表
     * @return 更新成功的记录数
     */
    int batchUpdateFeedCount(List<FeedCountUpdateDTO> updateList);

    /**
     * 分页查询动态列表
     * 核心逻辑：
     * 1. 从MySQL分页查询动态主表记录
     * 2. 从Redis获取实时点赞数和评论数（优先使用Redis，Redis无数据时回退到MySQL）
     * 3. 判断当前用户对每条动态的点赞状态
     * 4. 查询每条动态的前3条评论
     * 5. 组装返回视图对象
     *
     * @param userId 用户ID（用于判断点赞状态）
     * @param param  分页请求参数（已通过 @Valid 校验）
     * @return 动态列表查询结果
     */
    FeedListResultVO queryFeedList(Long userId, FeedListParam param);
}
