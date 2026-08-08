package com.nutrition.config;

import com.nutrition.dto.NutritionDTO;
import com.nutrition.entity.FoodNutrition;
import com.nutrition.mapper.FoodNutritionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 食物营养数据Redis缓存预热器
 * 项目启动完成后自动执行，将food_nutrition表全量数据预热到Redis
 * 使用Redis Hash结构存储，Key为food:nutrition，field为食物名称，value为精简营养数据JSON
 * 设置过期时间为7天作为兜底机制，异常时不阻断项目启动
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FoodNutritionCacheRunner implements ApplicationRunner {

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
     * 批量写入Redis的批次大小
     */
    private static final int BATCH_SIZE = 500;

    /**
     * 应用启动完成后执行缓存预热
     *
     * @param args 应用启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        log.info("========== 开始执行食物营养数据Redis缓存预热 ==========");

        try {
            // ========== 第1步：全量查询food_nutrition表有效数据 ==========
            LambdaQueryWrapper<FoodNutrition> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(FoodNutrition::getDeleteFlag, 0);
            List<FoodNutrition> allNutritionData = foodNutritionMapper.selectList(queryWrapper);

            if (allNutritionData == null || allNutritionData.isEmpty()) {
                log.warn("食物营养数据表为空，无需预热缓存");
                log.info("========== 食物营养数据Redis缓存预热完成（无数据） ==========");
                return;
            }

            int totalCount = allNutritionData.size();

            // ========== 第2步：构建精简营养数据Map ==========
            Map<Object, Object> nutritionMap = new HashMap<>(totalCount);
            for (FoodNutrition nutrition : allNutritionData) {
                NutritionDTO dto = convertToDTO(nutrition);
                String jsonValue = serializeNutritionDTO(dto);
                nutritionMap.put(nutrition.getFoodName(), jsonValue);
            }

            // ========== 第3步：清空旧缓存并批量写入新数据 ==========
            try {
                redisTemplate.delete(REDIS_HASH_KEY);

                int batchCount = (int) Math.ceil((double) nutritionMap.size() / BATCH_SIZE);
                int currentIndex = 0;

                for (int i = 0; i < batchCount; i++) {
                    int endIndex = Math.min(currentIndex + BATCH_SIZE, nutritionMap.size());
                    Map<Object, Object> batchMap = new HashMap<>();

                    int count = 0;
                    for (Map.Entry<Object, Object> entry : nutritionMap.entrySet()) {
                        if (count >= currentIndex && count < endIndex) {
                            batchMap.put(entry.getKey(), entry.getValue());
                        }
                        count++;
                    }

                    redisTemplate.opsForHash().putAll(REDIS_HASH_KEY, batchMap);                  
                    currentIndex = endIndex;
                }

                redisTemplate.expire(REDIS_HASH_KEY, REDIS_EXPIRE_DAYS, TimeUnit.DAYS);

                log.info("食物营养数据Redis缓存预热成功！");
                log.info("  - 预热数据条数：{}", totalCount);
                log.info("  - Redis Hash Key：{}", REDIS_HASH_KEY);
                log.info("  - 过期时间：{}天", REDIS_EXPIRE_DAYS);
            } catch (Exception e) {
                log.error("食物营养数据Redis缓存预热失败: error={}", e.getMessage(), e);
            }

            log.info("========== 食物营养数据Redis缓存预热完成 ==========");

        } catch (Exception e) {
            log.error("食物营养数据缓存预热异常: error={}", e.getMessage(), e);
        }
    }

    /**
     * 将FoodNutrition实体转换为精简的NutritionDTO
     * 仅保留calorie、protein、fat、carbohydrate四个核心计算字段
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
}