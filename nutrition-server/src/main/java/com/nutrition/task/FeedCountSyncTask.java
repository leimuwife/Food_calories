package com.nutrition.task;

import com.nutrition.dto.FeedCountUpdateDTO;
import com.nutrition.service.FeedService;
import com.nutrition.util.RedisCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 动态计数同步定时任务
 * 每小时将 Redis 中的点赞/评论计数批量同步到 MySQL feed 表
 * 实现高性能 + 最终数据一致性
 *
 * 职责边界：
 * - 只负责组装 DTO、判空、调用 Service 批量方法
 * - 不处理分片与事务（由 Service 层统一封装）
 *
 * 性能优化要点：
 * 1. 使用 SCAN 游标分批非阻塞遍历 Redis 键，替代 KEYS 全量匹配
 * 2. Service 层使用 CASE WHEN 单 SQL 批量更新，替代循环单条 UPDATE
 * 3. Service 层自动按 500 条分片，每批独立事务
 * 4. 异常隔离：单批失败不中断整体任务
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FeedCountSyncTask {

    private final FeedService feedService;
    private final RedisCache redisCache;

    /**
     * 每小时同步一次点赞数和评论数到数据库
     * Cron 表达式：0 0 * * * ?
     * 执行逻辑：
     * 1. SCAN 扫描所有 moment:like:count:* 和 moment:comment:count:* 键
     * 2. 批量读取 Redis 计数
     * 3. 组装 FeedCountUpdateDTO 列表
     * 4. 判空后调用 Service 批量更新（Service 内部自动分片 + 独立事务）
     * 5. 同步失败打印日志，不中断任务，下次周期重试
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void syncFeedCounts() {

        try {
            // 1. SCAN 扫描点赞计数键
            String likeCountPattern = RedisCache.PREFIX_MOMENT_LIKE_COUNT + "*";
            Set<String> likeCountKeys = redisCache.scanKeys(likeCountPattern);
            log.info("SCAN扫描到点赞计数键: {} 个", likeCountKeys.size());

            // 2. SCAN 扫描评论计数键
            String commentCountPattern = RedisCache.PREFIX_MOMENT_COMMENT_COUNT + "*";
            Set<String> commentCountKeys = redisCache.scanKeys(commentCountPattern);
            log.info("SCAN扫描到评论计数键: {} 个", commentCountKeys.size());

            // 3. 解析计数键，构建映射表
            Map<Long, Integer> likeCountMap = parseCountKeys(likeCountKeys, RedisCache.PREFIX_MOMENT_LIKE_COUNT);
            Map<Long, Integer> commentCountMap = parseCountKeys(commentCountKeys, RedisCache.PREFIX_MOMENT_COMMENT_COUNT);

            // 4. 合并需要更新的动态ID
            Set<Long> allFeedIds = Stream.concat(likeCountMap.keySet().stream(), commentCountMap.keySet().stream())
                    .collect(Collectors.toSet());

            log.info("需要更新的动态数: {} 个", allFeedIds.size());

            //5. 组装批量更新 DTO 列表
            List<FeedCountUpdateDTO> updateList = new ArrayList<>();
            for (Long feedId : allFeedIds) {
                FeedCountUpdateDTO dto = new FeedCountUpdateDTO();
                dto.setFeedId(feedId);
                dto.setLikeCount(likeCountMap.get(feedId));
                dto.setCommentCount(commentCountMap.get(feedId));
                updateList.add(dto);
            }

            // 6. 空列表前置判断
            if (updateList == null || updateList.isEmpty()) {
                log.info("动态计数同步任务：无数据需要更新，跳过数据库更新");
                return;
            }

            // 7. 调用 Service 批量更新（Service 内部处理分片 + 独立事务）==========
            int totalUpdated = feedService.batchUpdateFeedCount(updateList);

            log.info("动态计数同步任务完成，成功更新 {} 条记录", totalUpdated);

        } catch (Exception e) {
            log.error("动态计数同步任务执行失败: {}", e.getMessage(), e);
            // 任务失败不中断，下次周期重试
        }
    }

    /**
     * 解析 Redis 计数键，提取 feedId 和计数值
     *
     * @param keys    Redis 键集合
     * @param prefix  键前缀
     * @return feedId -> count 的映射
     */
    private Map<Long, Integer> parseCountKeys(Set<String> keys, String prefix) {
        Map<Long, Integer> countMap = new HashMap<>();

        for (String key : keys) {
            try {
                // 提取 feedId
                String feedIdStr = key.substring(prefix.length());
                Long feedId = Long.parseLong(feedIdStr);

                // 读取计数值
                String countStr = redisCache.getString(key);
                if (countStr != null && !countStr.isEmpty()) {
                    int count = Integer.parseInt(countStr);
                    countMap.put(feedId, count);
                }
            } catch (NumberFormatException e) {
                log.warn("解析计数键失败: key={}, error={}", key, e.getMessage());
            }
        }

        return countMap;
    }
}
