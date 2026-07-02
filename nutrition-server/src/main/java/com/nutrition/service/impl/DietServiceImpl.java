package com.nutrition.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nutrition.common.BusinessException;
import com.nutrition.entity.*;
import com.nutrition.param.DietRecordParam;
import com.nutrition.mapper.*;
import com.nutrition.service.DietService;
import com.nutrition.util.NutritionCalculator;
import com.nutrition.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * 饮食记录业务层实现类
 * 负责饮食记录的增删改查及统计计算
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DietServiceImpl extends ServiceImpl<DietRecordMapper, DietRecord> implements DietService {

    private final DietRecordItemMapper itemMapper;
    private final FoodDictMapper foodDictMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    @Transactional
    public DietRecord addRecord(Long userId, DietRecordParam param) {
        DietRecord record = new DietRecord();
        record.setUserId(userId);
        record.setRecordDate(LocalDate.parse(param.getRecordDate()));
        record.setMealType(param.getMealType());
        record.setRemark(param.getRemark());
        this.save(record);

        List<DietRecordItem> items = new ArrayList<>();
        for (DietRecordParam.DietItemParam itemParam : param.getItems()) {
            DietRecordItem item = buildItem(record.getId(), itemParam);
            items.add(item);
        }
        for (DietRecordItem item : items) {
            itemMapper.insert(item);
        }

        record.setItems(items);
        return record;
    }

    @Override
    public DailyDietVO getRecordsByDate(Long userId, String dateStr) {
        LocalDate date = LocalDate.parse(dateStr);
        List<DietRecordVO> recordVOs = this.baseMapper.findByUserAndDate(userId, date);

        for (DietRecordVO vo : recordVOs) {
            List<DietItemVO> items = itemMapper.findByRecordId(vo.getId());
            vo.setItems(items);
        }

        DailySummaryVO summary = buildDailySummary(userId, recordVOs);

        DailyDietVO result = new DailyDietVO();
        result.setRecords(recordVOs);
        result.setSummary(summary);
        return result;
    }

    @Override
    public List<DietRecordVO> getRecordsByRange(Long userId, String startDate, String endDate) {
        List<DietRecordVO> recordVOs = this.baseMapper.findByUserAndDateRange(
                userId, LocalDate.parse(startDate), LocalDate.parse(endDate));
        for (DietRecordVO vo : recordVOs) {
            vo.setItems(itemMapper.findByRecordId(vo.getId()));
        }
        return recordVOs;
    }

    @Override
    @Transactional
    public void deleteRecord(Long userId, Long recordId) {
        DietRecord record = this.getById(recordId);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权删除此记录");
        }
        itemMapper.deleteByRecordId(recordId);
        this.removeById(recordId);
    }

    @Override
    @Transactional
    public void updateItemWeight(Long userId, Long itemId, int weight) {
        DietRecordItem item = itemMapper.selectById(itemId);
        if (item == null) throw new BusinessException(404, "明细不存在");

        DietRecord record = this.getById(item.getRecordId());
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权修改");
        }

        FoodDict food = foodDictMapper.selectById(item.getFoodId());
        if (food != null) {
            double[] intake = NutritionCalculator.calcIntake(
                    food.getCaloriesPer100g(), food.getProteinPer100g(),
                    food.getFatPer100g(), food.getCarbsPer100g(), weight);
            item.setWeight(weight);
            item.setCalories((int) intake[0]);
            item.setProtein(intake[1]);
            item.setFat(intake[2]);
            item.setCarbs(intake[3]);
        }
        itemMapper.updateById(item);
    }

    @Override
    @Transactional
    public void deleteItem(Long userId, Long itemId) {
        DietRecordItem item = itemMapper.selectById(itemId);
        if (item == null) throw new BusinessException(404, "明细不存在");

        DietRecord record = this.getById(item.getRecordId());
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权删除");
        }
        itemMapper.deleteById(itemId);

        List<DietItemVO> remaining = itemMapper.findByRecordId(record.getId());
        if (remaining.isEmpty()) {
            this.removeById(record.getId());
        }
    }

    @Override
    @Transactional
    public void copyRecordToDate(Long userId, Long recordId, String targetDate) {
        DietRecord source = this.getById(recordId);
        if (source == null || !source.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权复制此记录");
        }

        DietRecord copy = new DietRecord();
        copy.setUserId(userId);
        copy.setRecordDate(LocalDate.parse(targetDate));
        copy.setMealType(source.getMealType());
        copy.setRemark(source.getRemark());
        this.save(copy);

        List<DietItemVO> sourceItems = itemMapper.findByRecordId(recordId);
        for (DietItemVO si : sourceItems) {
            DietRecordItem ci = new DietRecordItem();
            ci.setRecordId(copy.getId());
            ci.setFoodId(si.getFoodId());
            ci.setFoodName(si.getFoodName());
            ci.setWeight(si.getWeight());
            ci.setCalories(si.getCalories());
            ci.setProtein(si.getProtein());
            ci.setFat(si.getFat());
            ci.setCarbs(si.getCarbs());
            itemMapper.insert(ci);
        }
    }

    @Override
    public DietRecordVO convertToVO(DietRecord record) {
        DietRecordVO vo = new DietRecordVO();
        vo.setId(record.getId());
        vo.setRecordDate(record.getRecordDate().toString());
        vo.setMealType(record.getMealType());
        vo.setRemark(record.getRemark());

        List<DietItemVO> itemVOs = new ArrayList<>();
        if (record.getItems() != null) {
            for (DietRecordItem item : record.getItems()) {
                DietItemVO itemVO = new DietItemVO();
                itemVO.setId(item.getId());
                itemVO.setFoodId(item.getFoodId());
                itemVO.setFoodName(item.getFoodName());
                itemVO.setWeight(item.getWeight());
                itemVO.setCalories(item.getCalories());
                itemVO.setProtein(item.getProtein());
                itemVO.setFat(item.getFat());
                itemVO.setCarbs(item.getCarbs());
                itemVOs.add(itemVO);
            }
        }
        vo.setItems(itemVOs);
        return vo;
    }

    private DietRecordItem buildItem(Long recordId, DietRecordParam.DietItemParam param) {
        DietRecordItem item = new DietRecordItem();
        item.setRecordId(recordId);
        item.setFoodId(param.getFoodId());
        item.setFoodName(param.getFoodName());
        item.setWeight(param.getWeight());

        FoodDict food = foodDictMapper.selectById(param.getFoodId());
        if (food != null) {
            double[] intake = NutritionCalculator.calcIntake(
                    food.getCaloriesPer100g(), food.getProteinPer100g(),
                    food.getFatPer100g(), food.getCarbsPer100g(), param.getWeight());
            item.setCalories((int) intake[0]);
            item.setProtein(intake[1]);
            item.setFat(intake[2]);
            item.setCarbs(intake[3]);
        } else {
            item.setCalories(0);
            item.setProtein(0.0);
            item.setFat(0.0);
            item.setCarbs(0.0);
        }
        return item;
    }

    private DailySummaryVO buildDailySummary(Long userId, List<DietRecordVO> recordVOs) {
        SysUser user = sysUserMapper.selectById(userId);

        int calorieGoal = user != null && user.getDailyCalorieGoal() != null ? user.getDailyCalorieGoal() : 2000;
        int proteinGoal = user != null && user.getDailyProteinGoal() != null ? user.getDailyProteinGoal() : 60;
        int fatGoal = user != null && user.getDailyFatGoal() != null ? user.getDailyFatGoal() : 55;
        int carbsGoal = user != null && user.getDailyCarbsGoal() != null ? user.getDailyCarbsGoal() : 250;

        double totalProtein = 0, totalFat = 0, totalCarbs = 0;
        int totalCalories = 0;

        Map<String, DailySummaryVO.MealSummaryVO> meals = new LinkedHashMap<>();
        meals.put("breakfast", buildMealSummaryVO());
        meals.put("lunch", buildMealSummaryVO());
        meals.put("dinner", buildMealSummaryVO());
        meals.put("snack", buildMealSummaryVO());

        for (DietRecordVO vo : recordVOs) {
            DailySummaryVO.MealSummaryVO meal = meals.get(vo.getMealType());
            if (meal == null) continue;

            List<DietItemVO> items = vo.getItems();
            if (items == null) continue;

            int mealCal = 0;
            double mealProtein = 0, mealFat = 0, mealCarbs = 0;
            List<DietItemVO> mealItems = new ArrayList<>();

            for (DietItemVO item : items) {
                mealCal += item.getCalories() != null ? item.getCalories() : 0;
                mealProtein += item.getProtein() != null ? item.getProtein() : 0;
                mealFat += item.getFat() != null ? item.getFat() : 0;
                mealCarbs += item.getCarbs() != null ? item.getCarbs() : 0;

                mealItems.add(item);
            }

            meal.setCalories(mealCal);
            meal.setProtein(NutritionCalculator.round1(mealProtein));
            meal.setFat(NutritionCalculator.round1(mealFat));
            meal.setCarbs(NutritionCalculator.round1(mealCarbs));
            meal.setItems(mealItems);

            totalCalories += mealCal;
            totalProtein += mealProtein;
            totalFat += mealFat;
            totalCarbs += mealCarbs;
        }

        double[] ratios = NutritionCalculator.calcEnergyRatio(totalProtein, totalFat, totalCarbs);

        DailySummaryVO summary = new DailySummaryVO();
        summary.setTotalCalories(totalCalories);
        summary.setTotalProtein(NutritionCalculator.round1(totalProtein));
        summary.setTotalFat(NutritionCalculator.round1(totalFat));
        summary.setTotalCarbs(NutritionCalculator.round1(totalCarbs));
        summary.setCalorieGoal(calorieGoal);
        summary.setProteinGoal(proteinGoal);
        summary.setFatGoal(fatGoal);
        summary.setCarbsGoal(carbsGoal);
        summary.setCaloriePercent(calorieGoal > 0 ? Math.min(100, totalCalories * 100 / calorieGoal) : 0);
        summary.setProteinPercent(proteinGoal > 0 ? Math.min(100, (int)(totalProtein * 100 / proteinGoal)) : 0);
        summary.setFatPercent(fatGoal > 0 ? Math.min(100, (int)(totalFat * 100 / fatGoal)) : 0);
        summary.setCarbsPercent(carbsGoal > 0 ? Math.min(100, (int)(totalCarbs * 100 / carbsGoal)) : 0);
        summary.setProteinRatio(ratios[0]);
        summary.setFatRatio(ratios[1]);
        summary.setCarbsRatio(ratios[2]);
        summary.setMeals(meals);
        return summary;
    }

    private DailySummaryVO.MealSummaryVO buildMealSummaryVO() {
        DailySummaryVO.MealSummaryVO meal = new DailySummaryVO.MealSummaryVO();
        meal.setCalories(0);
        meal.setProtein(0.0);
        meal.setFat(0.0);
        meal.setCarbs(0.0);
        meal.setItems(new ArrayList<>());
        return meal;
    }
}
