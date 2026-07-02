CREATE DATABASE IF NOT EXISTS nutrition_db
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE nutrition_db;

-- ================================================
-- 1. 用户表
-- ================================================
DROP TABLE IF EXISTS `diet_record_item`;
DROP TABLE IF EXISTS `diet_record`;
DROP TABLE IF EXISTS `food_dict`;
DROP TABLE IF EXISTS `sys_user`;

CREATE TABLE `sys_user` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
  `openid` VARCHAR(128) DEFAULT NULL COMMENT '微信openid',
  `nickname` VARCHAR(64) DEFAULT '新用户' COMMENT '昵称',
  `avatar` VARCHAR(512) DEFAULT NULL COMMENT '头像URL',
  `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
  `username` VARCHAR(64) DEFAULT NULL COMMENT '用户名(账号登录)',
  `password_hash` VARCHAR(256) DEFAULT NULL COMMENT 'BCrypt密码哈希',
  `daily_calorie_goal` INT DEFAULT 2000 COMMENT '每日热量目标(kcal)',
  `daily_protein_goal` INT DEFAULT 60 COMMENT '每日蛋白质目标(g)',
  `daily_fat_goal` INT DEFAULT 55 COMMENT '每日脂肪目标(g)',
  `daily_carbs_goal` INT DEFAULT 250 COMMENT '每日碳水目标(g)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY `uk_openid` (`openid`),
  UNIQUE KEY `uk_username` (`username`),
  INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ================================================
-- 2. 食物字典表
-- ================================================
CREATE TABLE `food_dict` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '食物ID',
  `food_name` VARCHAR(128) NOT NULL COMMENT '食物名称',
  `category` VARCHAR(32) NOT NULL DEFAULT '其他' COMMENT '食物分类',
  `calories_per_100g` INT NOT NULL DEFAULT 0 COMMENT '每100g热量(kcal)',
  `protein_per_100g` DECIMAL(5,1) NOT NULL DEFAULT 0.0 COMMENT '每100g蛋白质(g)',
  `fat_per_100g` DECIMAL(5,1) NOT NULL DEFAULT 0.0 COMMENT '每100g脂肪(g)',
  `carbs_per_100g` DECIMAL(5,1) NOT NULL DEFAULT 0.0 COMMENT '每100g碳水化合物(g)',
  `fiber_per_100g` DECIMAL(5,1) DEFAULT 0.0 COMMENT '每100g膳食纤维(g)',
  `edible_portion` DECIMAL(4,1) DEFAULT 100.0 COMMENT '可食部比例(%)',
  `data_source` VARCHAR(64) DEFAULT '中国食物成分表' COMMENT '数据来源',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX `idx_food_name` (`food_name`),
  INDEX `idx_category` (`category`),
  FULLTEXT INDEX `ft_food_name` (`food_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='食物字典表';

-- ================================================
-- 3. 饮食记录表
-- ================================================
CREATE TABLE `diet_record` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID(逻辑关联sys_user.id)',
  `record_date` DATE NOT NULL COMMENT '记录日期',
  `meal_type` VARCHAR(16) NOT NULL COMMENT '餐次类型:breakfast/lunch/dinner/snack',
  `remark` VARCHAR(256) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_user_date` (`user_id`, `record_date`),
  INDEX `idx_user_meal` (`user_id`, `record_date`, `meal_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='饮食记录表';

-- ================================================
-- 4. 饮食记录明细表
-- ================================================
CREATE TABLE `diet_record_item` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '明细ID',
  `record_id` BIGINT NOT NULL COMMENT '记录ID(逻辑关联diet_record.id)',
  `food_id` BIGINT DEFAULT NULL COMMENT '食物ID(逻辑关联food_dict.id)',
  `food_name` VARCHAR(128) NOT NULL COMMENT '食物名称快照',
  `weight` INT NOT NULL DEFAULT 100 COMMENT '食用重量(g)',
  `calories` INT NOT NULL DEFAULT 0 COMMENT '总热量(kcal)',
  `protein` DECIMAL(5,1) NOT NULL DEFAULT 0.0 COMMENT '总蛋白质(g)',
  `fat` DECIMAL(5,1) NOT NULL DEFAULT 0.0 COMMENT '总脂肪(g)',
  `carbs` DECIMAL(5,1) NOT NULL DEFAULT 0.0 COMMENT '总碳水(g)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX `idx_record_id` (`record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='饮食记录明细表';