-- nutrition_db.sys_user 定义
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `openid` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '微信openid',
  `nickname` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '新用户' COMMENT '昵称',
  `email` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱',
  `username` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户名(账号登录)',
  `password_hash` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'BCrypt密码哈希',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `delete_flag` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '逻辑删除是否删除',
  `file_ids` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联附件表中的id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_openid` (`openid`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=2074423796085907458 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';



-- nutrition_db.diet_item 定义

CREATE TABLE `diet_item` (
  `id` bigint NOT NULL COMMENT '雪花主键ID',
  `record_id` bigint NOT NULL COMMENT '关联diet_record主键',
  `food_name` varchar(200) NOT NULL COMMENT '用户输入食物名称',
  `food_desc` text COMMENT '食物描述，AI计算热量依据',
  `weight` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '食用重量(g)',
  `calories` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '总热量kcal（AI估算/手动填写）',
  `remark` varchar(500) DEFAULT NULL COMMENT '单条食物备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `delete_flag` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 0正常 1删除',
  `file_ids` varchar(200) DEFAULT NULL COMMENT '食物图片',
  PRIMARY KEY (`id`),
  KEY `idx_record_id` (`record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='单条食物明细';


-- nutrition_db.diet_record 定义

CREATE TABLE `diet_record` (
  `id` bigint NOT NULL COMMENT '雪花主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `record_date` date NOT NULL COMMENT '饮食记录日期',
  `meal_type` varchar(20) NOT NULL COMMENT '餐次：breakfast/lunch/dinner/snack（代码枚举控制）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `delete_flag` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 0正常 1删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_date_meal` ((if((`delete_flag` = 0),concat(`user_id`,_utf8mb4'_',`record_date`,_utf8mb4'_',`meal_type`),NULL))),
  KEY `idx_user_date` (`user_id`,`record_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='每日餐次总记录';



-- nutrition_db.feed 定义

CREATE TABLE `feed` (
  `id` bigint NOT NULL COMMENT '雪花主键ID',
  `user_id` bigint NOT NULL COMMENT '发布用户ID',
  `content` text COMMENT '动态文字内容',
  `file_ids` varchar(2000) DEFAULT NULL COMMENT '图片ID JSON数组',
  `like_count` int NOT NULL DEFAULT '0' COMMENT '点赞总数',
  `comment_count` int NOT NULL DEFAULT '0' COMMENT '评论总数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `delete_flag` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 0正常 1删除',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='轻友圈动态';



-- nutrition_db.feed_comment 定义

CREATE TABLE `feed_comment` (
  `id` bigint NOT NULL COMMENT '雪花主键ID',
  `feed_id` bigint NOT NULL COMMENT '动态ID',
  `user_id` bigint NOT NULL COMMENT '评论用户ID',
  `content` text NOT NULL COMMENT '评论内容',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `delete_flag` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 0正常 1删除',
  PRIMARY KEY (`id`),
  KEY `idx_feed` (`feed_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='轻友圈一级评论';



-- nutrition_db.feed_like 定义

CREATE TABLE `feed_like` (
  `id` bigint NOT NULL COMMENT '雪花主键ID',
  `feed_id` bigint NOT NULL COMMENT '动态ID',
  `user_id` bigint NOT NULL COMMENT '点赞用户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  `delete_flag` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 0正常 1删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_feed_user` (`feed_id`,`user_id`,`delete_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='动态点赞记录';


-- nutrition_db.checkin_record 定义

CREATE TABLE `checkin_record` (
  `id` bigint NOT NULL COMMENT '雪花主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `checkin_date` date NOT NULL COMMENT '打卡日期',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `delete_flag` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 0正常 1删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_checkin_date` (`user_id`,`checkin_date`,`delete_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户每日打卡';




-- nutrition_db.nutritionist_chat 定义

CREATE TABLE `nutritionist_chat` (
  `id` bigint NOT NULL COMMENT '雪花主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role` varchar(20) NOT NULL COMMENT '角色 user用户 / assistantAI',
  `content` text NOT NULL COMMENT '对话文本',
  `file_ids` varchar(2000) DEFAULT NULL COMMENT '图片附件ID JSON数组',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '消息时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `delete_flag` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 0正常 1删除',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='营养师AI对话记录';



-- nutrition_db.sys_file 定义

CREATE TABLE `sys_file` (
  `id` bigint NOT NULL COMMENT '雪花算法文件主键ID',
  `file_name` varchar(255) NOT NULL COMMENT '原始文件名',
  `file_suffix` varchar(50) DEFAULT '' COMMENT '文件后缀：jpg/png/pdf',
  `file_size` bigint DEFAULT '0' COMMENT '文件大小(字节)',
  `file_url` varchar(1000) NOT NULL COMMENT '文件线上访问地址',
  `storage_type` tinyint DEFAULT '1' COMMENT '存储类型 1本地 2OSS对象存储',
  `upload_user_id` bigint DEFAULT NULL COMMENT '上传人用户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `delete_flag` varchar(20) DEFAULT '0' COMMENT '逻辑删除 0正常 1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_upload_user` (`upload_user_id`),
  KEY `idx_delete_flag` (`delete_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='全局统一附件文件表';