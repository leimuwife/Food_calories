package com.nutrition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nutrition.common.BusinessException;
import com.nutrition.entity.Attachment;
import com.nutrition.entity.DietItem;
import com.nutrition.entity.DietRecord;
import com.nutrition.entity.SysUser;
import com.nutrition.enums.AuditSceneEnum;
import com.nutrition.enums.AuditSuggestEnum;
import com.nutrition.enums.MealType;
import com.nutrition.mapper.AttachmentMapper;
import com.nutrition.mapper.DietItemMapper;
import com.nutrition.mapper.DietRecordMapper;
import com.nutrition.service.AttachmentService;
import com.nutrition.service.ContentAuditService;
import com.nutrition.service.DietRecordService;
import com.nutrition.service.UserService;
import com.nutrition.util.IdConvertUtil;
import com.nutrition.util.RedisCache;
import com.nutrition.vo.DailyDietVO;
import com.nutrition.vo.DietItemVO;
import com.nutrition.vo.DietRecordVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private final ContentAuditService contentAuditService;
    private final UserService userService;

    @Value("${nutrition.default-image.food}")
    private String defaultFoodImageUrl;

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
            log.error("添加饮食记录失败: 用户ID无效, userId={}", userId);
            throw new BusinessException(400, "用户ID无效");
        }

        LocalDate recordDate = parseDate(param.getRecordDate(), userId);
        LocalDate today = LocalDate.now();
        if (recordDate.isAfter(today)) {
            log.error("添加饮食记录失败: 记录日期不能大于今天, userId={}, recordDate={}, today={}", userId, recordDate, today);
            throw new BusinessException(400, "记录日期不能大于今天");
        }

        MealType mealType = parseMealType(param.getMealType(), userId);

        SysUser user = userService.getCurrentUser(userId);
        String openid = user != null ? user.getOpenid() : null;

        StringBuilder allTextContent = new StringBuilder();
        for (com.nutrition.param.DietRecordParam.DietItemParam itemParam : param.getItems()) {
            if (itemParam.getFoodName() != null) {
                allTextContent.append(itemParam.getFoodName()).append(" ");
            }
            if (itemParam.getFoodDesc() != null) {
                allTextContent.append(itemParam.getFoodDesc()).append(" ");
            }
            if (itemParam.getRemark() != null) {
                allTextContent.append(itemParam.getRemark()).append(" ");
            }
        }

        if (allTextContent.length() > 0) {
            try {
                AuditSuggestEnum textAuditResult = contentAuditService.auditText(userId, openid, allTextContent.toString(), AuditSceneEnum.DIET_REMARK);
                if (textAuditResult == AuditSuggestEnum.RISKY) {
                    log.warn("饮食记录文本审核需要人工审核: userId={}, contentLength={}", userId, allTextContent.length());
                    throw new BusinessException(400, "内容需要人工审核，请稍后重试");
                } else if (textAuditResult == AuditSuggestEnum.BLOCK) {
                    log.warn("饮食记录文本审核未通过: userId={}, contentLength={}", userId, allTextContent.length());
                    throw new BusinessException(400, "内容包含违规信息，请修改后重新提交");
                }
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("饮食记录文本审核异常: userId={}, error={}", userId, e.getMessage(), e);
                throw new BusinessException(500, "内容审核服务暂时不可用，请稍后重试");
            }
        }

        DietRecord dietRecord = new DietRecord();
        dietRecord.setUserId(userId);
        dietRecord.setRecordDate(recordDate);
        dietRecord.setMealType(mealType);
        dietRecord.setDeleteFlag(0);

        baseMapper.insert(dietRecord);
        Long recordId = dietRecord.getId();

        String fileIdsStr = null;
        Long uploadedAttachmentId = null;

        if (file != null && !file.isEmpty()) {
            Attachment attachment = attachmentService.upload(file, userId, "diet/");
            uploadedAttachmentId = attachment.getId();
            fileIdsStr = String.valueOf(attachment.getId());
            log.info("饮食记录附件上传成功: recordId={}, attachmentId={}", recordId, attachment.getId());

            try {
                AuditSuggestEnum imageAuditResult = contentAuditService.auditImages(userId, openid, java.util.Collections.singletonList(fileIdsStr), AuditSceneEnum.DIET_REMARK);
                if (imageAuditResult == AuditSuggestEnum.RISKY) {
                    log.warn("饮食记录图片审核需要人工审核: userId={}, attachmentId={}", userId, attachment.getId());
                    throw new BusinessException(400, "图片需要人工审核，请稍后重试");
                } else if (imageAuditResult == AuditSuggestEnum.BLOCK) {
                    log.warn("饮食记录图片审核未通过: userId={}, attachmentId={}", userId, attachment.getId());
                    throw new BusinessException(400, "图片包含违规内容，请更换图片后重新提交");
                }
            } catch (BusinessException e) {
                if (uploadedAttachmentId != null) {
                    attachmentService.delete(uploadedAttachmentId);
                    log.info("审核失败，已清理上传的附件: attachmentId={}", uploadedAttachmentId);
                }
                throw e;
            } catch (Exception e) {
                if (uploadedAttachmentId != null) {
                    attachmentService.delete(uploadedAttachmentId);
                    log.info("审核异常，已清理上传的附件: attachmentId={}", uploadedAttachmentId);
                }
                log.error("饮食记录图片审核异常: userId={}, error={}", userId, e.getMessage(), e);
                throw new BusinessException(500, "图片审核服务暂时不可用，请稍后重试");
            }
        } else {
            Attachment defaultAttachment = new Attachment();
            defaultAttachment.setFileName("defult_food.png");
            defaultAttachment.setFileSuffix(".png");
            defaultAttachment.setFileSize(0L);
            defaultAttachment.setFileUrl(defaultFoodImageUrl);
            defaultAttachment.setStorageType(2);
            defaultAttachment.setUploadUserId(userId);
            defaultAttachment.setDeleteFlag(0);
            attachmentMapper.insert(defaultAttachment);
            fileIdsStr = String.valueOf(defaultAttachment.getId());
            log.info("使用默认食物图片: recordId={}, attachmentId={}, url={}", recordId, defaultAttachment.getId(), defaultFoodImageUrl);
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
            List<Long> fileIdList;
            Map<Long, String> fileUrlMap;
            
            if (processResult != null) {
                fileIdList = processResult.fileIdsCache.getOrDefault(fileIdsStr, Collections.emptyList());
                fileUrlMap = processResult.fileUrlMap;
            } else {
                fileIdList = parseFileIds(fileIdsStr);
                fileUrlMap = loadFileUrls(fileIdList);
            }
            
            List<String> urls = fileIdList.stream()
                    .map(fileUrlMap::get)
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
            log.error("删除饮食记录失败: 饮食记录不存在, userId={}, recordId={}", userId, recordId);
            throw new BusinessException(404, "饮食记录不存在");
        }
        if (!record.getUserId().equals(userId)) {
            log.error("删除饮食记录失败: 无权删除该记录, userId={}, recordId={}, recordUserId={}", userId, recordId, record.getUserId());
            throw new BusinessException(403, "无权删除该记录");
        }

        LambdaQueryWrapper<DietItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(DietItem::getRecordId, recordId);
        dietItemMapper.delete(itemWrapper);

        baseMapper.deleteById(recordId);

        clearDietCache(userId, record.getRecordDate().toString());
        log.debug("用户{}删除饮食记录{}，已清除缓存", userId, recordId);
    }

    @Override
    public DietItemVO getDietItemDetail(Long userId, Long itemId) {
        DietItem item = dietItemMapper.selectById(itemId);
        if (item == null) {
            log.error("查询饮食项详情失败: 饮食项不存在, userId={}, itemId={}", userId, itemId);
            throw new BusinessException(404, "饮食项不存在");
        }

        DietRecord record = baseMapper.selectById(item.getRecordId());
        if (record == null || !record.getUserId().equals(userId)) {
            log.error("查询饮食项详情失败: 无权访问该饮食项, userId={}, itemId={}, recordId={}", userId, itemId, item.getRecordId());
            throw new BusinessException(403, "无权访问该饮食项");
        }

        return convertToItemVO(item, null);
    }

    /**
     * 更新饮食项
     * 更新食物项信息，支持文件上传和默认图片兜底
     *
     * @param userId 用户ID
     * @param param  更新饮食项请求参数
     * @param file   食物图片文件（可选）
     * @return 饮食项ID
     * @throws IOException 文件上传异常
     */
    @Override
    @Transactional
    public Long updateDietItem(Long userId, com.nutrition.param.DietRecordParam param, MultipartFile file) throws IOException {
        if (userId == null || userId <= 0) {
            log.error("更新饮食项失败: 用户ID无效, userId={}", userId);
            throw new BusinessException(400, "用户ID无效");
        }

        com.nutrition.param.DietRecordParam.DietItemParam itemParam = param.getItems().get(0);
        if (itemParam.getId() == null) {
            log.error("更新饮食项失败: 饮食项ID不能为空, userId={}", userId);
            throw new BusinessException(400, "饮食项ID不能为空");
        }

        DietItem existingItem = dietItemMapper.selectById(itemParam.getId());
        if (existingItem == null) {
            log.error("更新饮食项失败: 饮食项不存在, userId={}, itemId={}", userId, itemParam.getId());
            throw new BusinessException(404, "饮食项不存在");
        }

        DietRecord record = baseMapper.selectById(existingItem.getRecordId());
        if (record == null || !record.getUserId().equals(userId)) {
            log.error("更新饮食项失败: 无权修改该饮食项, userId={}, itemId={}, recordUserId={}", userId, itemParam.getId(), record != null ? record.getUserId() : null);
            throw new BusinessException(403, "无权修改该饮食项");
        }

        SysUser user = userService.getCurrentUser(userId);
        String openid = user != null ? user.getOpenid() : null;

        StringBuilder textContent = new StringBuilder();
        if (itemParam.getFoodName() != null) {
            textContent.append(itemParam.getFoodName()).append(" ");
        }
        if (itemParam.getFoodDesc() != null) {
            textContent.append(itemParam.getFoodDesc()).append(" ");
        }
        if (itemParam.getRemark() != null) {
            textContent.append(itemParam.getRemark()).append(" ");
        }

        if (textContent.length() > 0) {
            try {
                AuditSuggestEnum textAuditResult = contentAuditService.auditText(userId, openid, textContent.toString(), AuditSceneEnum.DIET_REMARK);
                if (textAuditResult == AuditSuggestEnum.RISKY) {
                    log.warn("饮食项文本审核需要人工审核: userId={}, itemId={}, contentLength={}", userId, itemParam.getId(), textContent.length());
                    throw new BusinessException(400, "内容需要人工审核，请稍后重试");
                } else if (textAuditResult == AuditSuggestEnum.BLOCK) {
                    log.warn("饮食项文本审核未通过: userId={}, itemId={}, contentLength={}", userId, itemParam.getId(), textContent.length());
                    throw new BusinessException(400, "内容包含违规信息，请修改后重新提交");
                }
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("饮食项文本审核异常: userId={}, itemId={}, error={}", userId, itemParam.getId(), e.getMessage(), e);
                throw new BusinessException(500, "内容审核服务暂时不可用，请稍后重试");
            }
        }

        String fileIdsStr = existingItem.getFileIds();
        Long uploadedAttachmentId = null;

        if (file != null && !file.isEmpty()) {
            Attachment attachment = attachmentService.upload(file, userId, "diet/");
            uploadedAttachmentId = attachment.getId();
            fileIdsStr = String.valueOf(attachment.getId());
            log.info("饮食项图片更新成功: itemId={}, attachmentId={}", itemParam.getId(), attachment.getId());

            try {
                AuditSuggestEnum imageAuditResult = contentAuditService.auditImages(userId, openid, java.util.Collections.singletonList(fileIdsStr), AuditSceneEnum.DIET_REMARK);
                if (imageAuditResult == AuditSuggestEnum.RISKY) {
                    log.warn("饮食项图片审核需要人工审核: userId={}, itemId={}, attachmentId={}", userId, itemParam.getId(), attachment.getId());
                    throw new BusinessException(400, "图片需要人工审核，请稍后重试");
                } else if (imageAuditResult == AuditSuggestEnum.BLOCK) {
                    log.warn("饮食项图片审核未通过: userId={}, itemId={}, attachmentId={}", userId, itemParam.getId(), attachment.getId());
                    throw new BusinessException(400, "图片包含违规内容，请更换图片后重新提交");
                }
            } catch (BusinessException e) {
                if (uploadedAttachmentId != null) {
                    attachmentService.delete(uploadedAttachmentId);
                    log.info("审核失败，已清理上传的附件: attachmentId={}", uploadedAttachmentId);
                }
                throw e;
            } catch (Exception e) {
                if (uploadedAttachmentId != null) {
                    attachmentService.delete(uploadedAttachmentId);
                    log.info("审核异常，已清理上传的附件: attachmentId={}", uploadedAttachmentId);
                }
                log.error("饮食项图片审核异常: userId={}, itemId={}, error={}", userId, itemParam.getId(), e.getMessage(), e);
                throw new BusinessException(500, "图片审核服务暂时不可用，请稍后重试");
            }
        }

        existingItem.setFoodName(itemParam.getFoodName());
        existingItem.setFoodDesc(itemParam.getFoodDesc());
        existingItem.setWeight(itemParam.getWeight());
        existingItem.setCalories(itemParam.getCalories());
        existingItem.setRemark(itemParam.getRemark());
        existingItem.setFileIds(fileIdsStr);

        dietItemMapper.updateById(existingItem);

        clearDietCache(userId, record.getRecordDate().toString());

        log.info("用户{}更新饮食项成功: itemId={}", userId, itemParam.getId());

        return itemParam.getId();
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

    /**
     * 解析日期字符串
     *
     * @param dateStr 日期字符串（YYYY-MM-DD）
     * @param userId  用户ID
     * @return LocalDate
     */
    private LocalDate parseDate(String dateStr, Long userId) {
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            log.error("日期格式解析失败: userId={}, dateStr={}", userId, dateStr, e);
            throw new BusinessException(400, "日期格式错误，应为YYYY-MM-DD");
        }
    }

    /**
     * 解析餐次类型
     *
     * @param mealTypeCode 餐次类型代码
     * @param userId       用户ID
     * @return MealType
     */
    private MealType parseMealType(String mealTypeCode, Long userId) {
        try {
            return MealType.fromCode(mealTypeCode);
        } catch (Exception e) {
            log.error("餐次类型解析失败: userId={}, mealTypeCode={}", userId, mealTypeCode, e);
            throw new BusinessException(400, "无效的餐次类型");
        }
    }
}
