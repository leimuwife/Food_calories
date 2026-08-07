package com.nutrition.service.impl;

import com.nutrition.client.FastApiClient;
import com.nutrition.dto.CalorieEstimateResultDTO;
import com.nutrition.service.AiFoodEstimateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * AI食物热量估算服务实现类（Mock版本）
 * 原真实LLM调用+向量检索逻辑已迁移至独立Python-FastAPI项目
 * 当前返回固定模拟数据，后续接入FastAPI后替换为真实调用
 *
 * @see FastApiClient 后续通过此客户端调用Python-FastAPI AI服务
 */
@Service
@Slf4j
public class AiFoodEstimateServiceImpl implements AiFoodEstimateService {

    @Override
    public CalorieEstimateResultDTO estimateCalorie(String foodDesc, Integer weight) {
        log.info("AI热量估算(Mock): foodDesc={}, weight={}", foodDesc, weight);

        if (foodDesc == null || foodDesc.trim().isEmpty()) {
            log.warn("AI热量估算：食物描述为空");
            return CalorieEstimateResultDTO.builder().totalCalorie(BigDecimal.ZERO).build();
        }

        // TODO: 后续接入Python-FastAPI AI服务后，调用FastApiClient.estimateCalorie()替换Mock实现
        BigDecimal mockCalorie = calculateMockCalorie(foodDesc, weight);
        log.info("AI热量估算完成(Mock): calorie={}", mockCalorie);

        return CalorieEstimateResultDTO.builder().totalCalorie(mockCalorie).build();
    }

    /**
     * 根据食物描述和重量返回模拟热量数值
     * 模拟真实AI估算的效果，不同输入返回不同模拟值
     */
    private BigDecimal calculateMockCalorie(String foodDesc, Integer weight) {
        int effectiveWeight = (weight != null && weight > 0) ? weight : 200;
        double baseCalorie;

        String desc = foodDesc.toLowerCase();
        if (desc.contains("蛋糕") || desc.contains("奶油") || desc.contains("巧克力")) {
            baseCalorie = 350.0;
        } else if (desc.contains("肉") || desc.contains("鸡") || desc.contains("鱼") || desc.contains("虾")) {
            baseCalorie = 200.0;
        } else if (desc.contains("米") || desc.contains("面") || desc.contains("饭") || desc.contains("包")) {
            baseCalorie = 150.0;
        } else if (desc.contains("果") || desc.contains("蔬") || desc.contains("菜")) {
            baseCalorie = 40.0;
        } else if (desc.contains("奶") || desc.contains("酸奶")) {
            baseCalorie = 65.0;
        } else {
            baseCalorie = 100.0;
        }

        BigDecimal result = new BigDecimal(baseCalorie * effectiveWeight / 100.0)
                .setScale(1, java.math.RoundingMode.HALF_UP);
        return result;
    }
}
