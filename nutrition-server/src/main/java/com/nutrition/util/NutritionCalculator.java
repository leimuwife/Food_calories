package com.nutrition.util;

/**
 * 营养计算工具类
 * 严格封装热量换算、供能占比计算逻辑，全局复用
 */
public class NutritionCalculator {

    /** 能量换算系数 (kcal/g) */
    public static final double PROTEIN_KCAL_PER_GRAM = 4.0;
    public static final double CARBS_KCAL_PER_GRAM = 4.0;
    public static final double FAT_KCAL_PER_GRAM = 9.0;

    /**
     * 根据营养素克数计算总热量
     * 公式：总热量 = 蛋白质×4 + 碳水×4 + 脂肪×9
     */
    public static int calcCalories(double protein, double fat, double carbs) {
        return (int) Math.round(protein * PROTEIN_KCAL_PER_GRAM
                + fat * FAT_KCAL_PER_GRAM
                + carbs * CARBS_KCAL_PER_GRAM);
    }

    /**
     * 根据每100g营养素和实际食用重量计算摄入量
     * @param caloriesPer100g 每100g热量
     * @param proteinPer100g  每100g蛋白质
     * @param fatPer100g      每100g脂肪
     * @param carbsPer100g    每100g碳水
     * @param weight          实际食用重量(g)
     * @return [热量(kcal), 蛋白质(g), 脂肪(g), 碳水(g)]
     */
    public static double[] calcIntake(int caloriesPer100g, double proteinPer100g,
                                       double fatPer100g, double carbsPer100g, int weight) {
        double factor = weight / 100.0;
        return new double[]{
                Math.round(caloriesPer100g * factor),
                round1(proteinPer100g * factor),
                round1(fatPer100g * factor),
                round1(carbsPer100g * factor),
        };
    }

    /**
     * 计算供能占比
     * @return [蛋白质占比%, 脂肪占比%, 碳水占比%]
     */
    public static double[] calcEnergyRatio(double protein, double fat, double carbs) {
        double totalCal = protein * PROTEIN_KCAL_PER_GRAM
                + fat * FAT_KCAL_PER_GRAM
                + carbs * CARBS_KCAL_PER_GRAM;
        if (totalCal == 0) return new double[]{0, 0, 0};
        return new double[]{
                round1(protein * PROTEIN_KCAL_PER_GRAM / totalCal * 100),
                round1(fat * FAT_KCAL_PER_GRAM / totalCal * 100),
                round1(carbs * CARBS_KCAL_PER_GRAM / totalCal * 100),
        };
    }

    /** 保留1位小数 */
    public static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
