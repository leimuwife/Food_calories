package com.nutrition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nutrition.dto.NutritionDTO;
import com.nutrition.entity.FoodNutrition;
import com.nutrition.mapper.FoodNutritionMapper;
import com.nutrition.service.FoodNutritionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 食物营养数据服务实现类
 * 实现食物营养数据的查询和缓存管理逻辑
 * 遵循Redis缓存优先、MySQL兜底的策略，Redis宕机时自动降级为直连MySQL
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FoodNutritionServiceImpl implements FoodNutritionService {

    private final FoodNutritionMapper foodNutritionMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Redis Hash结构的统一Key
     */
    private static final String REDIS_HASH_KEY = "food:nutrition";

    /**
     * Redis Hash过期时间（7天）
     */
    private static final long REDIS_EXPIRE_DAYS = 7;

    /**
     * 批量查询食材营养数据（供AI热量估算调用）
     * 执行逻辑：
     * 1. 优先从Redis Hash中批量查询（opsForHash().multiGet），命中直接返回
     * 2. Redis未命中的食材，去MySQL执行批量精确查询（WHERE food_name IN (...)）
     * 3. MySQL查询到的结果，同步回写入Redis Hash，保证下次查询命中缓存
     * 4. MySQL也未命中的食材，返回空标识，供上层业务做降级处理
     *
     * @param foodNameList 食物名称列表
     * @return Map<String, NutritionDTO> key为食物名称，value为精简营养数据DTO
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, NutritionDTO> batchGetNutrition(List<String> foodNameList) {
        if (foodNameList == null || foodNameList.isEmpty()) {
            log.debug("批量查询食物营养数据：输入列表为空");
            return Collections.emptyMap();
        }

        log.debug("批量查询食物营养数据：共{}种食物", foodNameList.size());

        Map<String, NutritionDTO> result = new HashMap<>();
        List<String> missedNames = new ArrayList<>();

        // ========== 第1步：从Redis批量查询 ==========
        try {
            List<Object> keyList = foodNameList.stream().map(s -> (Object) s).collect(Collectors.toList());
            List<Object> cachedValues = redisTemplate.opsForHash().multiGet(REDIS_HASH_KEY, keyList);
            for (int i = 0; i < foodNameList.size(); i++) {
                String foodName = foodNameList.get(i);
                Object cachedValue = cachedValues.get(i);
                
                if (cachedValue != null) {
                    NutritionDTO dto = deserializeNutritionDTO(String.valueOf(cachedValue));
                    if (dto != null) {
                        result.put(foodName, dto);
                        log.debug("Redis缓存命中：{}", foodName);
                    } else {
                        missedNames.add(foodName);
                    }
                } else {
                    missedNames.add(foodName);
                    log.debug("Redis缓存未命中：{}", foodName);
                }
            }
        } catch (Exception e) {
            log.error("Redis批量查询失败，自动降级为MySQL查询: error={}", e.getMessage(), e);
            missedNames.addAll(foodNameList);
        }

        // ========== 第2步：MySQL批量查询未命中的食物 ==========
        if (!missedNames.isEmpty()) {
            log.debug("MySQL批量查询：{}种食物", missedNames.size());
            
            LambdaQueryWrapper<FoodNutrition> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(FoodNutrition::getFoodName, missedNames);
            queryWrapper.eq(FoodNutrition::getDeleteFlag, 0);
            
            List<FoodNutrition> dbResults = foodNutritionMapper.selectList(queryWrapper);
            
            Map<String, NutritionDTO> dbResultMap = dbResults.stream()
                    .collect(Collectors.toMap(
                            FoodNutrition::getFoodName,
                            this::convertToDTO,
                            (existing, replacement) -> existing
                    ));

            for (String foodName : missedNames) {
                NutritionDTO dto = dbResultMap.get(foodName);
                if (dto != null) {
                    result.put(foodName, dto);
                    // 同步回写入Redis缓存
                    syncToCache(foodName, dto);
                    log.debug("MySQL查询命中并同步缓存：{}", foodName);
                } else {
                    log.debug("MySQL查询也未命中：{}", foodName);
                }
            }
        }

        log.debug("批量查询完成：输入{}种，命中{}种", foodNameList.size(), result.size());
        return result;
    }

    /**
     * 模糊匹配兜底查询
     * 当精确查询无结果时，按食物名称前缀模糊匹配MySQL，返回最匹配的1条结果
     * 匹配到的结果同步写入Redis缓存
     *
     * @param keyword 食物名称关键词
     * @return NutritionDTO 精简营养数据DTO，未匹配到返回null
     */
    @Override
    @Transactional(readOnly = true)
    public NutritionDTO fuzzyMatchNutrition(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            log.debug("模糊匹配查询：关键词为空");
            return null;
        }

        log.debug("模糊匹配查询：keyword={}", keyword);

        LambdaQueryWrapper<FoodNutrition> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.likeRight(FoodNutrition::getFoodName, keyword.trim());
        queryWrapper.eq(FoodNutrition::getDeleteFlag, 0);
        queryWrapper.orderByDesc(FoodNutrition::getCalorie);
        queryWrapper.last("LIMIT 1");

        FoodNutrition nutrition = foodNutritionMapper.selectOne(queryWrapper);

        if (nutrition != null) {
            NutritionDTO dto = convertToDTO(nutrition);
            syncToCache(nutrition.getFoodName(), dto);
            log.debug("模糊匹配成功：{} → {}", keyword, nutrition.getFoodName());
            return dto;
        }

        log.debug("模糊匹配未找到：{}", keyword);
        return null;
    }

    /**
     * 将食物营养数据同步写入Redis缓存
     *
     * @param foodName 食物名称
     * @param nutritionDTO 营养数据DTO
     */
    @Override
    public void syncToCache(String foodName, NutritionDTO nutritionDTO) {
        if (foodName == null || nutritionDTO == null) {
            return;
        }

        try {
            String jsonValue = serializeNutritionDTO(nutritionDTO);
            redisTemplate.opsForHash().put(REDIS_HASH_KEY, foodName, jsonValue);
            redisTemplate.expire(REDIS_HASH_KEY, REDIS_EXPIRE_DAYS, TimeUnit.DAYS);
            log.debug("食物营养数据已写入Redis缓存：{}", foodName);
        } catch (Exception e) {
            log.error("写入Redis缓存失败：foodName={}, error={}", foodName, e.getMessage(), e);
        }
    }

    /**
     * 将FoodNutrition实体转换为NutritionDTO
     * 仅提取4个核心营养字段：calorie、protein、fat、carbohydrate
     *
     * @param entity 食物营养实体
     * @return NutritionDTO 精简营养数据DTO
     */
    private NutritionDTO convertToDTO(FoodNutrition entity) {
        return NutritionDTO.builder()
                .calorie(entity.getCalorie() != null ? entity.getCalorie() : BigDecimal.ZERO)
                .protein(entity.getProtein() != null ? entity.getProtein() : BigDecimal.ZERO)
                .fat(entity.getFat() != null ? entity.getFat() : BigDecimal.ZERO)
                .carbohydrate(entity.getCarbohydrate() != null ? entity.getCarbohydrate() : BigDecimal.ZERO)
                .build();
    }

    /**
     * 序列化NutritionDTO为JSON字符串
     *
     * @param dto 营养数据DTO
     * @return JSON字符串
     */
    private String serializeNutritionDTO(NutritionDTO dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            log.error("序列化NutritionDTO失败：error={}", e.getMessage(), e);
            return "{}";
        }
    }

    /**
     * 反序列化JSON字符串为NutritionDTO
     *
     * @param json JSON字符串
     * @return NutritionDTO，解析失败返回null
     */
    private NutritionDTO deserializeNutritionDTO(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, NutritionDTO.class);
        } catch (JsonProcessingException e) {
            log.error("反序列化NutritionDTO失败：json={}, error={}", json, e.getMessage(), e);
            return null;
        }
    }
}