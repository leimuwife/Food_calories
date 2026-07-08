package com.nutrition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nutrition.common.BusinessException;
import com.nutrition.entity.Attachment;
import com.nutrition.entity.DietItem;
import com.nutrition.entity.DietRecord;
import com.nutrition.enums.MealType;
import com.nutrition.mapper.AttachmentMapper;
import com.nutrition.mapper.DietItemMapper;
import com.nutrition.mapper.DietRecordMapper;
import com.nutrition.service.DietRecordService;
import com.nutrition.util.IdConvertUtil;
import com.nutrition.util.RedisCache;
import com.nutrition.vo.DailyDietVO;
import com.nutrition.vo.DietItemVO;
import com.nutrition.vo.DietRecordVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 饮食记录服务实现类
 * 实现饮食记录相关的业务逻辑
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DietRecordServiceImpl extends ServiceImpl<DietRecordMapper, DietRecord> implements DietRecordService {

    private final DietItemMapper dietItemMapper;
    private final AttachmentMapper attachmentMapper;
    private final ObjectMapper objectMapper;
    private final RedisCache redisCache;

    private static final int MAX_QUERY_DAYS_OFFSET = 90;

    @Override
    @Transactional(readOnly = true)
    public DailyDietVO getDailyDiet(Long userId, LocalDate date) {
        validateParams(userId, date);

        String cacheKey = RedisCache.getDietKey(userId, date.toString());
        DailyDietVO cachedVO = redisCache.get(cacheKey, DailyDietVO.class);
        if (cachedVO != null) {
            log.debug("用户{}在{}的饮食记录命中缓存", userId, date);
            return cachedVO;
        }

        List<DietRecord> records = baseMapper.selectByUserIdAndDate(userId, date);

        if (CollectionUtils.isEmpty(records)) {
            log.debug("用户{}在{}无饮食记录", userId, date);
            DailyDietVO emptyResult = createEmptyDailyDiet(date);
            cacheDiet(cacheKey, emptyResult);
            return emptyResult;
        }

        List<Long> recordIds = records.stream()
                .map(DietRecord::getId)
                .collect(Collectors.toList());

        List<DietItem> items = dietItemMapper.selectByRecordIds(recordIds);

        ItemsProcessResult processResult = processItems(items);

        DailyDietVO result = buildDailyDietVO(records, processResult);

        cacheDiet(cacheKey, result);

        log.debug("用户{}在{}查询饮食记录完成，共{}条记录，{}个食物", 
                userId, date, result.getRecords().size(), result.getFoodList().size());

        return result;
    }

    /**
     * 缓存每日饮食统计数据
     */
    private void cacheDiet(String cacheKey, DailyDietVO dailyDietVO) {
        long ttlSeconds = redisCache.getDietCacheTtlSeconds();
        redisCache.set(cacheKey, dailyDietVO, ttlSeconds, TimeUnit.SECONDS);
        log.debug("每日饮食记录已缓存: {}, TTL={}秒", cacheKey, ttlSeconds);
    }

    /**
     * 清除指定用户指定日期的饮食记录缓存
     */
    public void clearDietCache(Long userId, String date) {
        String cacheKey = RedisCache.getDietKey(userId, date);
        redisCache.delete(cacheKey);
        log.debug("已清除饮食记录缓存: {}", cacheKey);
    }

    /**
     * 清除指定用户所有饮食记录缓存
     */
    public void clearAllDietCache(Long userId) {
        redisCache.delete(RedisCache.PREFIX_DIET + userId + ":*");
        log.debug("已清除用户{}所有饮食记录缓存", userId);
    }

    private void validateParams(Long userId, LocalDate date) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(400, "用户ID无效");
        }
        if (date == null) {
            throw new BusinessException(400, "查询日期不能为空");
        }
        LocalDate today = LocalDate.now();
        if (date.isAfter(today)) {
            throw new BusinessException(400, "查询日期不能大于今天");
        }
        if (date.isBefore(today.minusDays(MAX_QUERY_DAYS_OFFSET))) {
            throw new BusinessException(400, "查询日期不能超过90天前");
        }
    }

    private ItemsProcessResult processItems(List<DietItem> items) {
        ItemsProcessResult result = new ItemsProcessResult();

        for (DietItem item : items) {
            Long recordId = item.getRecordId();

            result.itemsByRecordId.computeIfAbsent(recordId, k -> new ArrayList<>()).add(item);

            BigDecimal calories = item.getCalories() != null ? item.getCalories() : BigDecimal.ZERO;
            result.caloriesByRecordId.merge(recordId, calories, BigDecimal::add);

            String fileIdsStr = item.getFileIds();
            if (StringUtils.hasText(fileIdsStr)) {
                if (!result.fileIdsCache.containsKey(fileIdsStr)) {
                    List<Long> parsedIds = parseFileIds(fileIdsStr);
                    result.fileIdsCache.put(fileIdsStr, parsedIds);
                }
                result.uniqueFileIds.addAll(result.fileIdsCache.get(fileIdsStr));
            }
        }

        result.uniqueFileIds = result.uniqueFileIds.stream().distinct().collect(Collectors.toList());

        result.fileUrlMap = loadFileUrls(result.uniqueFileIds);

        return result;
    }

    private List<Long> parseFileIds(String fileIdsStr) {
        try {
            return objectMapper.readValue(fileIdsStr, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            log.warn("解析fileIds JSON失败: {}", fileIdsStr, e);
            try {
                return Collections.singletonList(Long.parseLong(fileIdsStr.trim()));
            } catch (NumberFormatException ex) {
                log.warn("fileIds非有效JSON且非有效数字: {}", fileIdsStr);
                return Collections.emptyList();
            }
        }
    }

    private DailyDietVO buildDailyDietVO(List<DietRecord> records, ItemsProcessResult processResult) {
        DailyDietVO result = new DailyDietVO();
        List<DietRecordVO> recordVOs = new ArrayList<>(records.size());
        List<DietItemVO> foodList = new ArrayList<>();

        Map<MealType, BigDecimal> mealCaloriesMap = new EnumMap<>(MealType.class);
        for (MealType mealType : MealType.values()) {
            mealCaloriesMap.put(mealType, BigDecimal.ZERO);
        }

        for (DietRecord record : records) {
            DietRecordVO recordVO = convertToRecordVO(record, processResult);
            recordVOs.add(recordVO);
            foodList.addAll(recordVO.getItems());

            mealCaloriesMap.merge(record.getMealType(), 
                    recordVO.getTotalCalories(), BigDecimal::add);
        }

        result.setTotalCalories(mealCaloriesMap.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        result.setBreakfastCalories(mealCaloriesMap.get(MealType.BREAKFAST));
        result.setLunchCalories(mealCaloriesMap.get(MealType.LUNCH));
        result.setDinnerCalories(mealCaloriesMap.get(MealType.DINNER));
        result.setSnackCalories(mealCaloriesMap.get(MealType.SNACK));
        result.setRecords(recordVOs);
        result.setFoodList(foodList);

        return result;
    }

    private DietRecordVO convertToRecordVO(DietRecord record, ItemsProcessResult processResult) {
        DietRecordVO vo = new DietRecordVO();
        vo.setId(IdConvertUtil.toString(record.getId()));
        vo.setRecordDate(record.getRecordDate().toString());
        vo.setMealType(record.getMealType().getCode());
        vo.setMealTypeName(record.getMealType().getDescription());

        BigDecimal recordCalories = processResult.caloriesByRecordId.getOrDefault(record.getId(), BigDecimal.ZERO);
        vo.setTotalCalories(recordCalories);

        List<DietItem> recordItems = processResult.itemsByRecordId.getOrDefault(record.getId(), Collections.emptyList());
        List<DietItemVO> itemVOs = recordItems.stream()
                .map(item -> convertToItemVO(item, processResult))
                .collect(Collectors.toList());
        vo.setItems(itemVOs);

        return vo;
    }

    private DietItemVO convertToItemVO(DietItem item, ItemsProcessResult processResult) {
        DietItemVO vo = new DietItemVO();
        vo.setId(IdConvertUtil.toString(item.getId()));
        vo.setRecordId(IdConvertUtil.toString(item.getRecordId()));
        vo.setFoodName(item.getFoodName());
        vo.setFoodDesc(item.getFoodDesc());
        vo.setWeight(item.getWeight());
        vo.setCalories(item.getCalories());
        vo.setRemark(item.getRemark());

        String fileIdsStr = item.getFileIds();
        if (StringUtils.hasText(fileIdsStr)) {
            List<Long> fileIdList = processResult.fileIdsCache.getOrDefault(fileIdsStr, Collections.emptyList());
            List<String> urls = fileIdList.stream()
                    .map(processResult.fileUrlMap::get)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toList());
            vo.setImageUrls(urls);
        } else {
            vo.setImageUrls(Collections.emptyList());
        }

        return vo;
    }

    private Map<Long, String> loadFileUrls(List<Long> fileIds) {
        if (CollectionUtils.isEmpty(fileIds)) {
            return Collections.emptyMap();
        }

        LambdaQueryWrapper<Attachment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Attachment::getId, fileIds);
        queryWrapper.eq(Attachment::getDeleteFlag, 0);

        List<Attachment> attachments = attachmentMapper.selectList(queryWrapper);
        return attachments.stream()
                .collect(Collectors.toMap(Attachment::getId, Attachment::getFileUrl, (u1, u2) -> u1));
    }

    private DailyDietVO createEmptyDailyDiet(LocalDate date) {
        DailyDietVO result = new DailyDietVO();
        result.setTotalCalories(BigDecimal.ZERO);
        result.setBreakfastCalories(BigDecimal.ZERO);
        result.setLunchCalories(BigDecimal.ZERO);
        result.setDinnerCalories(BigDecimal.ZERO);
        result.setSnackCalories(BigDecimal.ZERO);
        result.setRecords(Collections.emptyList());
        result.setFoodList(Collections.emptyList());
        return result;
    }

    private static class ItemsProcessResult {
        Map<Long, List<DietItem>> itemsByRecordId = new HashMap<>();
        Map<Long, BigDecimal> caloriesByRecordId = new HashMap<>();
        Map<String, List<Long>> fileIdsCache = new HashMap<>();
        List<Long> uniqueFileIds = new ArrayList<>();
        Map<Long, String> fileUrlMap = Collections.emptyMap();
    }
}