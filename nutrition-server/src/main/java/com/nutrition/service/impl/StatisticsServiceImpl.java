package com.nutrition.service.impl;

import com.nutrition.entity.*;
import com.nutrition.mapper.*;
import com.nutrition.service.StatisticsService;
import com.nutrition.util.NutritionCalculator;
import com.nutrition.vo.DailySummaryVO;
import com.nutrition.vo.DietItemVO;
import com.nutrition.vo.DietRecordVO;
import com.nutrition.vo.MonthlySummaryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计分析业务层实现类
 * 负责饮食数据的统计分析、趋势计算和数据导出
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final DietRecordMapper dietRecordMapper;
    private final DietRecordItemMapper itemMapper;
    private final SysUserMapper userMapper;

    @Override
    public DailySummaryVO getDailySummary(Long userId, String dateStr) {
        LocalDate date = LocalDate.parse(dateStr);
        List<DietRecordVO> recordVOs = dietRecordMapper.findByUserAndDate(userId, date);
        for (DietRecordVO vo : recordVOs) {
            vo.setItems(itemMapper.findByRecordId(vo.getId()));
        }

        SysUser user = userMapper.selectById(userId);

        double totalProtein = 0, totalFat = 0, totalCarbs = 0;
        int totalCalories = 0;
        Map<String, DailySummaryVO.MealSummaryVO> meals = new LinkedHashMap<>();

        for (DietRecordVO vo : recordVOs) {
            String mt = vo.getMealType();
            meals.putIfAbsent(mt, new DailySummaryVO.MealSummaryVO());
            DailySummaryVO.MealSummaryVO meal = meals.get(mt);

            int mc = 0;
            double mp = 0, mf = 0, mcarb = 0;
            List<DietItemVO> mealItems = new ArrayList<>();

            if (vo.getItems() != null) {
                for (DietItemVO item : vo.getItems()) {
                    mc += item.getCalories() != null ? item.getCalories() : 0;
                    mp += item.getProtein() != null ? item.getProtein() : 0;
                    mf += item.getFat() != null ? item.getFat() : 0;
                    mcarb += item.getCarbs() != null ? item.getCarbs() : 0;
                    mealItems.add(item);
                }
            }
            meal.setCalories(mc);
            meal.setProtein(NutritionCalculator.round1(mp));
            meal.setFat(NutritionCalculator.round1(mf));
            meal.setCarbs(NutritionCalculator.round1(mcarb));
            meal.setItems(mealItems);

            totalCalories += mc;
            totalProtein += mp;
            totalFat += mf;
            totalCarbs += mcarb;
        }

        double[] ratios = NutritionCalculator.calcEnergyRatio(totalProtein, totalFat, totalCarbs);

        DailySummaryVO result = new DailySummaryVO();
        result.setTotalCalories(totalCalories);
        result.setTotalProtein(NutritionCalculator.round1(totalProtein));
        result.setTotalFat(NutritionCalculator.round1(totalFat));
        result.setTotalCarbs(NutritionCalculator.round1(totalCarbs));
        result.setProteinRatio(ratios[0]);
        result.setFatRatio(ratios[1]);
        result.setCarbsRatio(ratios[2]);
        result.setMeals(meals);

        if (user != null) {
            result.setCalorieGoal(user.getDailyCalorieGoal());
            result.setProteinGoal(user.getDailyProteinGoal());
            result.setFatGoal(user.getDailyFatGoal());
            result.setCarbsGoal(user.getDailyCarbsGoal());
        }

        return result;
    }

    @Override
    public MonthlySummaryVO getMonthlySummary(Long userId, int year, int month) {
        List<DietRecordVO> recordVOs = dietRecordMapper.findByUserAndMonth(userId, year, month);
        for (DietRecordVO vo : recordVOs) {
            vo.setItems(itemMapper.findByRecordId(vo.getId()));
        }

        Map<LocalDate, List<DietRecordVO>> byDate = recordVOs.stream()
                .collect(Collectors.groupingBy(v -> LocalDate.parse(v.getRecordDate())));

        int daysWithData = byDate.size();
        double totalCalAll = 0, totalPAll = 0, totalFAll = 0, totalCAll = 0;

        List<MonthlySummaryVO.DailyTrendVO> dailyTrend = new ArrayList<>();

        Map<String, Integer> foodCount = new HashMap<>();

        for (Map.Entry<LocalDate, List<DietRecordVO>> entry : byDate.entrySet()) {
            LocalDate d = entry.getKey();
            List<DietRecordVO> dayRecords = entry.getValue();

            double dp = 0, df = 0, dc = 0;
            int dCal = 0;

            for (DietRecordVO vo : dayRecords) {
                if (vo.getItems() != null) {
                    for (DietItemVO item : vo.getItems()) {
                        dCal += item.getCalories() != null ? item.getCalories() : 0;
                        dp += item.getProtein() != null ? item.getProtein() : 0;
                        df += item.getFat() != null ? item.getFat() : 0;
                        dc += item.getCarbs() != null ? item.getCarbs() : 0;

                        String name = item.getFoodName();
                        foodCount.merge(name, 1, Integer::sum);
                    }
                }
            }

            totalCalAll += dCal;
            totalPAll += dp;
            totalFAll += df;
            totalCAll += dc;

            MonthlySummaryVO.DailyTrendVO dayData = new MonthlySummaryVO.DailyTrendVO();
            dayData.setDay(d.getDayOfMonth());
            dayData.setCalories(dCal);
            dailyTrend.add(dayData);
        }

        dailyTrend.sort(Comparator.comparingInt(MonthlySummaryVO.DailyTrendVO::getDay));

        double[] avgRatios = NutritionCalculator.calcEnergyRatio(
                daysWithData > 0 ? totalPAll / daysWithData : 0,
                daysWithData > 0 ? totalFAll / daysWithData : 0,
                daysWithData > 0 ? totalCAll / daysWithData : 0);

        List<MonthlySummaryVO.TopFoodVO> topFoods = foodCount.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(10)
                .map(e -> {
                    MonthlySummaryVO.TopFoodVO m = new MonthlySummaryVO.TopFoodVO();
                    m.setFoodName(e.getKey());
                    m.setCount(e.getValue());
                    return m;
                })
                .collect(Collectors.toList());

        MonthlySummaryVO result = new MonthlySummaryVO();
        result.setAvgDailyCalories(daysWithData > 0 ? (int)(totalCalAll / daysWithData) : 0);
        result.setAvgProteinRatio(avgRatios[0]);
        result.setAvgFatRatio(avgRatios[1]);
        result.setAvgCarbsRatio(avgRatios[2]);
        result.setDailyTrend(dailyTrend);
        result.setTopFoods(topFoods);
        result.setTotalDays(daysWithData);
        return result;
    }

    @Override
    public String exportCSV(Long userId, String startDate, String endDate) {
        List<DietRecordVO> recordVOs = dietRecordMapper.findByUserAndDateRange(
                userId, LocalDate.parse(startDate), LocalDate.parse(endDate));
        for (DietRecordVO vo : recordVOs) {
            vo.setItems(itemMapper.findByRecordId(vo.getId()));
        }

        StringBuilder csv = new StringBuilder();
        csv.append("日期,餐次,食物名称,重量(g),热量(kcal),蛋白质(g),脂肪(g),碳水(g)\n");

        for (DietRecordVO vo : recordVOs) {
            if (vo.getItems() != null) {
                for (DietItemVO item : vo.getItems()) {
                    csv.append(vo.getRecordDate()).append(",");
                    csv.append(vo.getMealType()).append(",");
                    csv.append(escapeCSV(item.getFoodName())).append(",");
                    csv.append(item.getWeight()).append(",");
                    csv.append(item.getCalories()).append(",");
                    csv.append(item.getProtein()).append(",");
                    csv.append(item.getFat()).append(",");
                    csv.append(item.getCarbs()).append("\n");
                }
            }
        }
        return csv.toString();
    }

    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
