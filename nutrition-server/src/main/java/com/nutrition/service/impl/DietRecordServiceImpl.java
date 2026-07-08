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
import com.nutrition.service.AttachmentService;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    private final AttachmentService attachmentService;

    private static final int MAX_QUERY_DAYS_OFFSET = 90;

    /**
     * 添加饮食记录（不带文件）
     * 创建饮食记录主表和食物项明细，并清除相关缓存
     *
     * @param userId 用户ID
     * @param param  饮食记录参数
     * @return 记录ID
     * @throws IOException 文件上传异常
     */
    @Override
    @Transactional
    public Long addDietRecord(Long userId, com.nutrition.param.DietRecordParam param) throws IOException {
        return addDietRecord(userId, param, null);
    }

    /**
     * 添加饮食记录（带文件上传）
     * 创建饮食记录主表和食物项明细，同时上传文件到OSS并保存附件记录
     *
     * @param userId 用户ID
     * @param param  饮食记录参数
     * @param file   食物图片文件（可选）
     * @return 记录ID
     * @throws IOException 文件上传异常
     */
    @Override
    @Transactional
    public Long addDietRecord(Long userId, com.nutrition.param.DietRecordParam param, MultipartFile file) throws IOException {
        if (userId == null || userId <= 0) {
            throw new BusinessException(400, "用户ID无效");
        }
        if (param == null) {
            throw new BusinessException(400, "请求参数不能为空");
        }

        LocalDate recordDate;
        try {
            recordDate = LocalDate.parse(param.getRecordDate());
        } catch (Exception e) {
            throw new BusinessException(400, "日期格式错误，应为YYYY-MM-DD");
        }

        LocalDate today = LocalDate.now();
        if (recordDate.isAfter(today)) {
            throw new BusinessException(400, "记录日期不能大于今天");
        }

        MealType mealType;
        try {
            mealType = MealType.fromCode(param.getMealType());
        } catch (Exception e) {
            throw new BusinessException(400, "无效的餐次类型");
        }

        DietRecord dietRecord = new DietRecord();
        dietRecord.setUserId(userId);
        dietRecord.setRecordDate(recordDate);
        dietRecord.setMealType(mealType);
        dietRecord.setDeleteFlag(0);

        baseMapper.insert(dietRecord);
        Long recordId = dietRecord.getId();

        String fileIdsStr = null;
        if (file != null && !file.isEmpty()) {
            Attachment attachment = attachmentService.upload(file, userId, "diet/");
            fileIdsStr = String.valueOf(attachment.getId());
            log.info("饮食记录附件上传成功: recordId={}, attachmentId={}", recordId, attachment.getId());
        }

        for (com.nutrition.param.DietRecordParam.DietItemParam itemParam : param.getItems()) {
            DietItem dietItem = new DietItem();
            dietItem.setRecordId(recordId);
            dietItem.setFoodName(itemParam.getFoodName());
            dietItem.setFoodDesc(itemParam.getFoodDesc());
            dietItem.setWeight(itemParam.getWeight());
            dietItem.setCalories(itemParam.getCalories());
            dietItem.setRemark(itemParam.getRemark());
            dietItem.setFileIds(fileIdsStr);
            dietItem.setDeleteFlag(0);

            dietItemMapper.insert(dietItem);
        }

        clearDietCache(userId, param.getRecordDate());

        log.info("用户{}添加饮食记录成功: recordId={}, date={}, mealType={}",
                userId, recordId, recordDate, mealType);

        return recordId;
    }

    /**
     * 获取每日饮食记录
     * 优先从缓存获取，缓存未命中时查询数据库并更新缓存
     *
     * @param userId 用户ID
     * @param date   查询日期
     * @return DailyDietVO 每日饮食视图对象
     */
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
            return createEmptyDailyDiet(date);
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
     *
     * @param cacheKey      缓存键
     * @param dailyDietVO   每日饮食视图对象
     */
    private void cacheDiet(String cacheKey, DailyDietVO dailyDietVO) {
        long ttlSeconds = redisCache.getDietCacheTtlSeconds();
        redisCache.set(cacheKey, dailyDietVO, ttlSeconds, TimeUnit.SECONDS);
        log.debug("每日饮食记录已缓存: {}, TTL={}秒", cacheKey, ttlSeconds);
    }

    /**
     * 清除指定用户指定日期的饮食记录缓存
     *
     * @param userId 用户ID
     * @param date   日期
     */
    public void clearDietCache(Long userId, String date) {
        String cacheKey = RedisCache.getDietKey(userId, date);
        redisCache.delete(cacheKey);
        log.debug("已清除饮食记录缓存: {}", cacheKey);
    }

    /**
     * 清除指定用户所有饮食记录缓存
     *
     * @param userId 用户ID
     */
    public void clearAllDietCache(Long userId) {
        redisCache.delete(RedisCache.PREFIX_DIET + userId + ":*");
        log.debug("已清除用户{}所有饮食记录缓存", userId);
    }

    /**
     * 验证用户ID和日期参数
     *
     * @param userId 用户ID
     * @param date   日期
     */
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

    /**
     * 处理食物项列表
     * 解析fileIds JSON，收集所有文件ID，加载文件URL映射
     *
     * @param items 食物项列表
     * @return ItemsProcessResult 处理结果
     */
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

    /**
     * 解析fileIds字符串
     * 支持JSON数组格式和单个数字格式
     *
     * @param fileIdsStr fileIds字符串
     * @return 文件ID列表
     */
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

    /**
     * 构建每日饮食视图对象
     * 汇总各餐次热量，转换记录和食物项为VO
     *
     * @param records        饮食记录列表
     * @param processResult  处理结果
     * @return DailyDietVO 每日饮食视图对象
     */
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

    /**
     * 将饮食记录实体转换为视图对象
     *
     * @param record         饮食记录实体
     * @param processResult  处理结果
     * @return DietRecordVO 饮食记录视图对象
     */
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

    /**
     * 将食物项实体转换为视图对象
     * 解析fileIds并填充图片URL列表
     *
     * @param item           食物项实体
     * @param processResult  处理结果
     * @return DietItemVO 食物项视图对象
     */
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

    /**
     * 根据文件ID列表加载文件URL映射
     *
     * @param fileIds 文件ID列表
     * @return 文件ID到URL的映射
     */
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

    /**
     * 创建空的每日饮食视图对象
     *
     * @param date 日期
     * @return DailyDietVO 空的每日饮食视图对象
     */
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

    /**
     * 删除饮食记录
     * 删除主记录和所有食物项明细，并清除相关缓存
     *
     * @param userId   用户ID
     * @param recordId 记录ID
     */
    @Override
    @Transactional
    public void deleteDietRecord(Long userId, Long recordId) {
        DietRecord record = baseMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(404, "饮食记录不存在");
        }
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权删除该记录");
        }

        LambdaQueryWrapper<DietItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(DietItem::getRecordId, recordId);
        dietItemMapper.delete(itemWrapper);

        baseMapper.deleteById(recordId);

        clearDietCache(userId, record.getRecordDate().toString());
        log.debug("用户{}删除饮食记录{}，已清除缓存", userId, recordId);
    }

    /**
     * 食物项处理结果内部类
     * 用于存储处理过程中的中间数据
     */
    private static class ItemsProcessResult {
        Map<Long, List<DietItem>> itemsByRecordId = new HashMap<>();
        Map<Long, BigDecimal> caloriesByRecordId = new HashMap<>();
        Map<String, List<Long>> fileIdsCache = new HashMap<>();
        List<Long> uniqueFileIds = new ArrayList<>();
        Map<Long, String> fileUrlMap = Collections.emptyMap();
    }
}
