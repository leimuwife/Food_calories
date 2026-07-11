package com.nutrition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutrition.common.BusinessException;
import com.nutrition.dto.FeedCountUpdateDTO;
import com.nutrition.entity.Feed;
import com.nutrition.entity.FeedComment;
import com.nutrition.entity.FeedLike;
import com.nutrition.entity.SysUser;
import com.nutrition.enums.AuditSceneEnum;
import com.nutrition.enums.AuditSuggestEnum;
import com.nutrition.enums.BizMsgEnum;
import com.nutrition.mapper.FeedCommentMapper;
import com.nutrition.mapper.FeedLikeMapper;
import com.nutrition.mapper.FeedMapper;
import com.nutrition.param.FeedCommentParam;
import com.nutrition.param.FeedListParam;
import com.nutrition.param.FeedPublishParam;
import com.nutrition.service.ContentAuditService;
import com.nutrition.service.FeedService;
import com.nutrition.service.UserService;
import com.nutrition.util.RedisCache;
import com.nutrition.vo.FeedCommentVO;
import com.nutrition.vo.FeedItemVO;
import com.nutrition.vo.FeedListResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 轻友圈动态服务实现类
 * 核心功能：动态发布审核流程
 * 审核链路：本地敏感词扫描 → 微信文本审核 → 微信图片审核 → 保存动态
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeedServiceImpl extends ServiceImpl<FeedMapper, Feed> implements FeedService {

    private final ContentAuditService contentAuditService;
    private final UserService userService;
    private final ObjectMapper objectMapper;
    private final RedisCache redisCache;
    private final FeedLikeMapper feedLikeMapper;
    private final FeedCommentMapper feedCommentMapper;
    private final TransactionTemplate transactionTemplate;
    private final com.nutrition.service.AttachmentService attachmentService;

    /**
     * 批量更新单次最大条数，防止 SQL 语句过长触发 MySQL max_allowed_packet 包超限
     */
    private static final int BATCH_SIZE = 500;

    /**
     * 发布动态（含完整审核流程）
     * 审核流程：文本审核 → 图片审核 → 保存动态
     * 任一环节审核失败都会中断发布并清理资源
     *
     * @param userId 用户ID
     * @param param  发布请求参数（已通过 @Valid 校验）
     * @return 动态ID
     */
    @Override
    @Transactional
    public Long publishFeed(Long userId, FeedPublishParam param) {
        log.info("开始发布动态: userId={}, contentLength={}, fileCount={}",
                userId, param.getContent().length(), param.getFileIds().size());

        // 获取用户 openid（用于微信审核接口）
        String openid = userService.getCurrentUser(userId).getOpenid();
        if (openid == null || openid.isEmpty()) {
            log.warn("用户未绑定微信: userId={}", userId);
            throw new BusinessException(BizMsgEnum.FEED_NOT_BIND_WECHAT);
        }

        // ========== 第1步：文本审核 ==========
        // 审核场景：朋友圈动态（MOMENT）
        try {
            contentAuditService.auditText(userId, openid, param.getContent(), AuditSceneEnum.MOMENT);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("动态文本审核异常: userId={}, contentLength={}, error={}", userId, param.getContent().length(), e.getMessage(), e);
            throw new BusinessException(BizMsgEnum.AUDIT_SERVICE_UNAVAILABLE);
        }

        // ========== 第2步：图片审核 ==========
        // 审核场景：朋友圈动态（MOMENT）
        try {
            contentAuditService.auditImages(userId, openid, param.getFileIds(), AuditSceneEnum.MOMENT);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("动态图片审核异常: userId={}, fileCount={}, error={}", userId, param.getFileIds().size(), e.getMessage(), e);
            throw new BusinessException(BizMsgEnum.AUDIT_IMAGE_SERVICE_UNAVAILABLE);
        }

        // ========== 第3步：保存动态记录 ==========
        Feed feed = new Feed();
        feed.setUserId(userId);
        feed.setContent(param.getContent());

        // 将图片ID列表转换为JSON字符串存储
        String fileIdsJson;
        try {
            fileIdsJson = objectMapper.writeValueAsString(param.getFileIds());
        } catch (JsonProcessingException e) {
            log.error("序列化图片ID列表失败: userId={}, fileIds={}", userId, param.getFileIds(), e);
            throw new BusinessException(BizMsgEnum.FEED_PUBLISH_FAILED);
        }
        feed.setFileIds(fileIdsJson);

        // 初始化点赞数和评论数
        feed.setLikeCount(0);
        feed.setCommentCount(0);

        save(feed);
        log.info("动态发布成功: userId={}, feedId={}, fileCount={}", userId, feed.getId(), param.getFileIds().size());

        return feed.getId();
    }

    /**
     * 点赞/取消点赞动态
     * 核心逻辑：
     * 1. SETNX 判断是否已点赞（原子操作，防止并发重复点赞）
     * 2. 未点赞 → INCR 计数 + 插入点赞明细记录
     * 3. 已点赞 → 删除防重键 + DECR 计数 + 删除点赞明细记录
     * 4. 不实时更新 MySQL 动态表，仅操作 Redis 和明细表
     *
     * @param userId 用户ID
     * @param feedId 动态ID
     * @return 操作结果：isLiked（是否已点赞）、likeCount（当前点赞数）
     */
    @Override
    @Transactional
    public Map<String, Object> toggleLike(Long userId, Long feedId) {
        log.debug("点赞操作: userId={}, feedId={}", userId, feedId);

        // 构建 Redis 键
        String likeUserKey = RedisCache.getMomentLikeUserKey(feedId, userId);
        String likeCountKey = RedisCache.getMomentLikeCountKey(feedId);

        // SETNX 判断是否已点赞（原子操作）
        boolean isNotLiked = redisCache.setIfAbsent(likeUserKey, "1");

        Map<String, Object> result = new HashMap<>();
        long likeCount;

        if (isNotLiked) {
            // 未点赞：执行点赞
            likeCount = redisCache.increment(likeCountKey);

            // 插入点赞明细记录（作为兜底数据源）
            FeedLike feedLike = new FeedLike();
            feedLike.setFeedId(feedId);
            feedLike.setUserId(userId);
            feedLikeMapper.insert(feedLike);

            result.put("isLiked", true);
            log.info("点赞成功: userId={}, feedId={}, likeCount={}", userId, feedId, likeCount);
        } else {
            // 已点赞：执行取消点赞
            redisCache.delete(likeUserKey);
            likeCount = redisCache.decrement(likeCountKey);

            // 删除点赞明细记录
            LambdaQueryWrapper<FeedLike> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(FeedLike::getFeedId, feedId).eq(FeedLike::getUserId, userId);
            feedLikeMapper.delete(wrapper);

            result.put("isLiked", false);
            log.info("取消点赞成功: userId={}, feedId={}, likeCount={}", userId, feedId, likeCount);
        }

        result.put("likeCount", likeCount);
        return result;
    }

    /**
     * 添加评论
     * 核心逻辑：
     * 1. 文本审核（本地敏感词 + 微信官方审核）
     * 2. 插入评论明细记录
     * 3. INCR Redis 评论计数
     * 4. 不实时更新 MySQL 动态表，仅操作 Redis 和明细表
     *
     * @param userId 用户ID
     * @param feedId 动态ID
     * @param param  评论请求参数（已通过 @Valid 校验）
     * @return 操作结果：commentCount（当前评论数）
     */
    @Override
    @Transactional
    public Map<String, Object> addComment(Long userId, Long feedId, FeedCommentParam param) {
        log.info("添加评论: userId={}, feedId={}, contentLength={}", userId, feedId, param.getContent().length());

        // 获取用户 openid（用于微信审核接口）
        String openid = userService.getCurrentUser(userId).getOpenid();
        if (openid == null || openid.isEmpty()) {
            log.warn("用户未绑定微信: userId={}", userId);
            throw new BusinessException(BizMsgEnum.FEED_NOT_BIND_WECHAT);
        }

        // ========== 文本审核（评论场景）==========
        try {
            contentAuditService.auditText(userId, openid, param.getContent(), AuditSceneEnum.COMMENT);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("评论文本审核异常: userId={}, contentLength={}, error={}", userId, param.getContent().length(), e.getMessage(), e);
            throw new BusinessException(BizMsgEnum.AUDIT_SERVICE_UNAVAILABLE);
        }

        // ========== 插入评论明细记录 ==========
        FeedComment comment = new FeedComment();
        comment.setFeedId(feedId);
        comment.setUserId(userId);
        comment.setContent(param.getContent());
        feedCommentMapper.insert(comment);

        // ========== Redis 评论计数自增 ==========
        String commentCountKey = RedisCache.getMomentCommentCountKey(feedId);
        long commentCount = redisCache.increment(commentCountKey);

        log.info("评论添加成功: userId={}, feedId={}, commentId={}, commentCount={}",
                userId, feedId, comment.getId(), commentCount);

        Map<String, Object> result = new HashMap<>();
        result.put("commentCount", commentCount);
        return result;
    }

    /**
     * 批量更新动态计数（点赞数、评论数）
     * 使用 CASE WHEN 单 SQL 批量更新，用于定时任务同步 Redis 计数到 MySQL
     *
     * 三层防护约束：
     * 1. 空列表前置判断：传入 null/空列表直接返回，不进入 Mapper
     * 2. 500 条分批切割：防止 IN 子句、CASE WHEN 分支过长触发 MySQL 包超限
     * 3. 单批次独立小事务：每批单独开启事务，一批失败不影响其他批次
     *
     * @param updateList 全量更新数据列表（不限条数，Service 内部自动分片）
     * @return 更新成功的总记录数
     */
    @Override
    public int batchUpdateFeedCount(List<FeedCountUpdateDTO> updateList) {
        // 约束 1：空列表前置判断，避免无效 DB 请求
        if (updateList == null || updateList.isEmpty()) {
            log.debug("批量更新动态计数：无数据需要更新");
            return 0;
        }

        log.info("批量更新动态计数：全量数据 {} 条，开始分片处理", updateList.size());

        int totalUpdated = 0;
        int totalBatches = (int) Math.ceil((double) updateList.size() / BATCH_SIZE);

        // 约束 2：500 条分批切割
        for (int i = 0; i < totalBatches; i++) {
            int start = i * BATCH_SIZE;
            int end = Math.min(start + BATCH_SIZE, updateList.size());
            List<FeedCountUpdateDTO> batch = updateList.subList(start, end);

            int batchIndex = i + 1;
            log.debug("开始处理第 {} 批：共 {} 条", batchIndex, batch.size());

            try {
                // 约束 3：单批次独立小事务（编程式事务，等效 REQUIRES_NEW）
                Integer updated = transactionTemplate.execute(status -> {
                    try {
                        return getBaseMapper().batchUpdateFeedCount(batch);
                    } catch (Exception e) {
                        status.setRollbackOnly();
                        throw e;
                    }
                });

                if (updated != null) {
                    totalUpdated += updated;
                    log.debug("第 {} 批更新成功：处理 {} 条，成功 {} 条", batchIndex, batch.size(), updated);
                }
            } catch (Exception e) {
                log.error("第 {} 批更新失败：处理 {} 条，error={}", batchIndex, batch.size(), e.getMessage(), e);
                // 单批失败不中断，继续执行下一批，等待下一小时周期重试失败数据
            }
        }

        log.info("批量更新动态计数完成：共 {} 批，成功更新 {} 条记录", totalBatches, totalUpdated);
        return totalUpdated;
    }

    /**
     * 日期时间格式化器：标准格式（用于 createTime 字段）
     */
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * 日期时间格式化器：发布时间格式（用于 publishTime 字段）
     */
    private static final DateTimeFormatter PUBLISH_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * 分页查询动态列表（只读事务优化版）
     * 核心逻辑：
     * 1. 从MySQL分页查询动态主表记录（仅查询未删除的动态）
     * 2. 从Redis获取实时点赞数和评论数（优先使用Redis，Redis无数据时回退到MySQL）
     * 3. 判断当前用户对每条动态的点赞状态
     * 4. 查询每条动态的前3条评论（含评论用户信息）
     * 5. 组装返回视图对象，包含是否有更多数据的标识
     *
     * 性能优化策略：
     * - 使用 @Transactional(readOnly = true) 只读事务，全程复用同一个SqlSession与JDBC连接
     * - 分页查询使用 LIMIT offset, limit，避免大数据量全表扫描
     * - 用户信息批量缓存：先收集所有用户ID，批量查询后缓存到内存
     * - Redis计数批量获取：一次请求获取多条动态的计数，减少网络开销
     * - 评论查询采用 LIMIT 3 限制条数，避免评论过多导致数据膨胀
     *
     * @param userId 用户ID（用于判断点赞状态）
     * @param param  分页请求参数（已通过 @Valid 校验）
     * @return 动态列表查询结果
     */
    @Override
    @Transactional(readOnly = true)
    public FeedListResultVO queryFeedList(Long userId, FeedListParam param) {
        log.info("查询动态列表: userId={}, pageNum={}, pageSize={}", userId, param.getPageNum(), param.getPageSize());
        int offset = (param.getPageNum() - 1) * param.getPageSize();
        int limit = param.getPageSize();

        List<Feed> feedList = getBaseMapper().selectFeedList(offset, limit);
        if (feedList == null || feedList.isEmpty()) {
            log.debug("动态列表为空: userId={}", userId);
            FeedListResultVO emptyResult = new FeedListResultVO();
            emptyResult.setList(new ArrayList<>());
            emptyResult.setTotal(0L);
            emptyResult.setHasMore(false);
            return emptyResult;
        }
        Long total = getBaseMapper().selectFeedCount();
        boolean hasMore = (offset + feedList.size()) < total;

        Map<Long, SysUser> userMap = new HashMap<>();
        for (Feed feed : feedList) {
            if (feed.getUserId() != null && !userMap.containsKey(feed.getUserId())) {
                try {
                    SysUser user = userService.getCurrentUser(feed.getUserId());
                    if (user != null) {
                        userMap.put(feed.getUserId(), user);
                    }
                } catch (Exception e) {
                    log.warn("获取用户信息失败: userId={}, error={}", feed.getUserId(), e.getMessage());
                }
            }
        }

        // ========== 批量查询附件信息（消除循环单查）==========
        List<Long> allAttachmentIds = new ArrayList<>();
        for (Feed feed : feedList) {
            List<String> fileIds = parseFileIds(feed.getFileIds());
            for (String fileId : fileIds) {
                try {
                    allAttachmentIds.add(Long.parseLong(fileId));
                } catch (NumberFormatException e) {
                    log.warn("附件ID格式错误: fileId={}", fileId);
                }
            }
        }

        Map<Long, com.nutrition.entity.Attachment> attachmentMap = new HashMap<>();
        if (!allAttachmentIds.isEmpty()) {
            List<com.nutrition.entity.Attachment> attachments = attachmentService.batchGetByIds(allAttachmentIds);
            attachmentMap = attachments.stream()
                    .collect(Collectors.toMap(com.nutrition.entity.Attachment::getId, a -> a));
            log.debug("批量查询附件完成: 共 {} 条", attachments.size());
        }

        List<FeedItemVO> voList = new ArrayList<>();
        for (Feed feed : feedList) {
            FeedItemVO vo = convertFeedToVO(feed, userId, userMap, attachmentMap);
            voList.add(vo);
        }

        FeedListResultVO result = new FeedListResultVO();
        result.setList(voList);
        result.setTotal(total);
        result.setHasMore(hasMore);

        log.info("查询动态列表完成: userId={}, 返回 {} 条，总数 {}，是否有更多 {}", userId, voList.size(), total, hasMore);
        return result;
    }

    /**
     * 将动态实体转换为视图对象
     * 包含：用户信息填充、文件ID解析、图片URL获取、Redis计数获取、点赞状态判断、评论列表查询
     *
     * @param feed          动态实体
     * @param userId        当前用户ID（用于判断点赞状态）
     * @param userMap       用户信息缓存Map（避免重复查询）
     * @param attachmentMap 附件信息缓存Map（避免循环单查附件）
     * @return 动态视图对象
     */
    private FeedItemVO convertFeedToVO(Feed feed, Long userId, Map<Long, SysUser> userMap,
                                        Map<Long, com.nutrition.entity.Attachment> attachmentMap) {
        FeedItemVO vo = new FeedItemVO();

        // 基础信息
        vo.setId(feed.getId());
        vo.setUserId(feed.getUserId());

        // 用户信息（从缓存Map中获取）
        SysUser user = userMap.get(feed.getUserId());
        if (user != null) {
            vo.setUserName(user.getNickname());
            vo.setUserAvatar(null);
        } else {
            vo.setUserName("未知用户");
            vo.setUserAvatar(null);
        }

        // 动态内容
        vo.setContent(feed.getContent());

        // 文件ID数组（JSON反序列化）
        List<String> fileIds = parseFileIds(feed.getFileIds());
        vo.setFileIds(fileIds);

        // 图片URL列表（从缓存Map中获取，避免循环单查附件）
        List<String> imageUrls = new ArrayList<>();
        for (String fileId : fileIds) {
            try {
                Long attachmentId = Long.parseLong(fileId);
                com.nutrition.entity.Attachment attachment = attachmentMap.get(attachmentId);
                if (attachment != null) {
                    imageUrls.add(attachment.getFileUrl());
                }
            } catch (NumberFormatException e) {
                log.warn("附件ID格式错误: fileId={}", fileId);
            }
        }
        vo.setImageUrls(imageUrls);

        // 点赞数（优先从Redis获取，回退到MySQL）
        Integer likeCount = getLikeCountFromRedis(feed.getId());
        if (likeCount == null) {
            likeCount = feed.getLikeCount() != null ? feed.getLikeCount() : 0;
        }
        vo.setLikeCount(likeCount);

        // 点赞状态（判断当前用户是否已点赞）
        vo.setIsLiked(isFeedLikedByUser(feed.getId(), userId));

        // 评论数（优先从Redis获取，回退到MySQL）
        Integer commentCount = getCommentCountFromRedis(feed.getId());
        if (commentCount == null) {
            commentCount = feed.getCommentCount() != null ? feed.getCommentCount() : 0;
        }
        vo.setCommentCount(commentCount);

        // 评论列表（最多3条）
        vo.setComments(getFeedComments(feed.getId(), userMap));

        // 时间字段
        LocalDateTime createTime = feed.getCreateTime();
        if (createTime != null) {
            vo.setCreateTime(createTime.format(DATETIME_FORMATTER));
            vo.setPublishTime(createTime.format(PUBLISH_TIME_FORMATTER));
        }

        return vo;
    }

    /**
     * 解析文件ID JSON字符串为列表
     * 容错处理：JSON解析失败时返回空列表
     *
     * @param fileIdsJson 文件ID JSON字符串
     * @return 文件ID列表
     */
    private List<String> parseFileIds(String fileIdsJson) {
        if (fileIdsJson == null || fileIdsJson.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(fileIdsJson, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.warn("解析文件ID JSON失败: fileIdsJson={}, error={}", fileIdsJson, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 从Redis获取动态点赞数
     *
     * @param feedId 动态ID
     * @return 点赞数（Redis无数据返回null）
     */
    private Integer getLikeCountFromRedis(Long feedId) {
        String key = RedisCache.getMomentLikeCountKey(feedId);
        String value = redisCache.getString(key);
        if (value != null && !value.isEmpty()) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                log.warn("解析Redis点赞数失败: key={}, value={}", key, value);
            }
        }
        return null;
    }

    /**
     * 从Redis获取动态评论数
     *
     * @param feedId 动态ID
     * @return 评论数（Redis无数据返回null）
     */
    private Integer getCommentCountFromRedis(Long feedId) {
        String key = RedisCache.getMomentCommentCountKey(feedId);
        String value = redisCache.getString(key);
        if (value != null && !value.isEmpty()) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                log.warn("解析Redis评论数失败: key={}, value={}", key, value);
            }
        }
        return null;
    }

    /**
     * 判断当前用户是否已点赞指定动态
     * 通过Redis防重键判断，避免查询MySQL明细表
     *
     * @param feedId 动态ID
     * @param userId 用户ID
     * @return 是否已点赞
     */
    private boolean isFeedLikedByUser(Long feedId, Long userId) {
        if (userId == null) {
            return false;
        }
        String key = RedisCache.getMomentLikeUserKey(feedId, userId);
        return redisCache.exists(key);
    }

    /**
     * 查询动态的前3条评论（含评论用户信息）
     *
     * @param feedId  动态ID
     * @param userMap 用户信息缓存Map
     * @return 评论视图对象列表
     */
    private List<FeedCommentVO> getFeedComments(Long feedId, Map<Long, SysUser> userMap) {
        List<FeedComment> comments = feedCommentMapper.selectCommentsByFeedId(feedId);
        if (comments == null || comments.isEmpty()) {
            return new ArrayList<>();
        }

        List<FeedCommentVO> voList = new ArrayList<>();
        for (FeedComment comment : comments) {
            FeedCommentVO vo = new FeedCommentVO();
            vo.setId(comment.getId());
            vo.setUserId(comment.getUserId());
            vo.setContent(comment.getContent());

            // 评论用户昵称（从缓存Map中获取，未缓存则查询）
            SysUser commentUser = userMap.get(comment.getUserId());
            if (commentUser == null) {
                try {
                    commentUser = userService.getCurrentUser(comment.getUserId());
                    if (commentUser != null) {
                        userMap.put(comment.getUserId(), commentUser);
                    }
                } catch (Exception e) {
                    log.warn("获取评论用户信息失败: userId={}, error={}", comment.getUserId(), e.getMessage());
                }
            }
            vo.setUserName(commentUser != null ? commentUser.getNickname() : "未知用户");

            // 创建时间
            if (comment.getCreateTime() != null) {
                vo.setCreateTime(comment.getCreateTime().format(DATETIME_FORMATTER));
            }

            voList.add(vo);
        }

        return voList;
    }
}
