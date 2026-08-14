数据库表设计

创建数据库
create database nutrition_db;

-- nutrition_db.ai_config 定义

CREATE TABLE `ai_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `model_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模型标识名称，如 qwen-max、deepseek-chat',
  `nickname` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '模型昵称',
  `model_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模型厂商类型，用于适配不同调用实现，如 openai、dashscope',
  `api_url` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '大模型接口请求地址',
  `api_key` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'API 密钥，AES 加密后存储',
  `system_prompt` text COLLATE utf8mb4_unicode_ci COMMENT '系统提示词',
  `temperature` decimal(3,2) DEFAULT '0.70' COMMENT '模型温度参数，范围 0~1',
  `max_tokens` int DEFAULT '800' COMMENT '单次回答最大 token 数',
  `is_enabled` tinyint DEFAULT '0' COMMENT '是否启用：0-禁用，1-启用；全局仅允许1条启用记录',
  `delete_flag` tinyint DEFAULT '0' COMMENT '逻辑删除标记：0-未删除，1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI大模型配置表';


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



-- nutrition_db.content_audit_record 定义

CREATE TABLE `content_audit_record` (
  `id` bigint NOT NULL COMMENT '主键雪花ID',
  `user_id` bigint NOT NULL COMMENT '操作用户ID，关联sys_user',
  `openid` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户微信openid',
  `audit_type` tinyint NOT NULL COMMENT '审核类型：1文本 2图片',
  `content_text` text COLLATE utf8mb4_unicode_ci COMMENT '待审核文本内容，文本审核时存储',
  `file_ids` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '附件表主键ID数组JSON字符串，存储本次上传所有图片附件id',
  `scene` int NOT NULL COMMENT '业务场景值：1朋友圈动态 2评论 3饮食备注 4个人资料',
  `suggest` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '微信审核结果：pass放行/risky待复审/block违规拦截',
  `label` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '违规分类标签，如色情/广告/涉政',
  `audit_time` datetime NOT NULL COMMENT '审核调用时间',
  `review_status` tinyint NOT NULL DEFAULT '0' COMMENT '人工复审状态：0无需复审 1待复审 2已处理',
  `review_operator` bigint DEFAULT NULL COMMENT '复审管理员ID',
  `review_result` tinyint DEFAULT NULL COMMENT '人工复核结论：0合规 1确认违规',
  `review_remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '管理员复审备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `delete_flag` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 0正常 1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_review_status` (`review_status`),
  KEY `idx_audit_time` (`audit_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内容安全审核记录表';



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
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_feed_user` (`feed_id`,`user_id`,`delete_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='动态点赞记录';


-- nutrition_db.food_nutrition 定义

CREATE TABLE `food_nutrition` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `food_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '食物名称',
  `food_category` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '食物分类',
  `edible_part` decimal(5,1) DEFAULT '100.0' COMMENT '可食部百分比(%)',
  `calorie` decimal(8,1) DEFAULT '0.0' COMMENT '能量(千卡/100g)',
  `protein` decimal(6,2) DEFAULT '0.00' COMMENT '蛋白质(克/100g)',
  `fat` decimal(6,2) DEFAULT '0.00' COMMENT '脂肪(克/100g)',
  `carbohydrate` decimal(6,2) DEFAULT '0.00' COMMENT '碳水化合物(克/100g)',
  `delete_flag` tinyint(1) DEFAULT '0' COMMENT '是否删除: 0-未删除, 1-已删除',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_food_name` (`food_name`),
  KEY `idx_food_category` (`food_category`),
  KEY `idx_delete_flag` (`delete_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=1865 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='食物营养成分表';



-- nutrition_db.rag_knowledge_document 定义

CREATE TABLE `rag_knowledge_document` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID，业务唯一文档ID',
  `doc_name` varchar(255) NOT NULL COMMENT '上传文档名称',
  `file_md5` varchar(64) NOT NULL COMMENT '文件整体MD5，用于查重，和Python侧md5校验一致',
  `upload_user_id` bigint NOT NULL COMMENT '上传管理员ID，关联系统用户表',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `status` tinyint DEFAULT '1' COMMENT '状态：1正常 2向量入库中 3入库失败 4已删除',
  `remark` varchar(500) DEFAULT '' COMMENT '备注说明',
  `vector_store_id` varchar(128) DEFAULT '' COMMENT '阿里云向量库该文档分组ID，删除时用',
  `delete_flag` tinyint(1) DEFAULT '0' COMMENT '是否删除: 0-未删除, 1-已删除',
  `file_ids` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '附件表主键ID数组JSON字符串，存储本次上传所有图片附件id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_md5_del` (`file_md5`,`delete_flag`),
  KEY `idx_upload_user` (`upload_user_id`),
  KEY `idx_delete_flag` (`delete_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='RAG知识库-上传文档主表';


-- nutrition_db.chat_message 定义

CREATE TABLE `chat_message` (
  `id` bigint NOT NULL COMMENT '雪花主键ID',
  `session_id` varchar(64) NOT NULL COMMENT '关联会话唯一id',
  `role` varchar(32) NOT NULL COMMENT '消息类型：user用户提问、ai_thought AI思考、tool_call工具入参、tool_result工具返回、ai_answer AI最终回答',
  `content` text NOT NULL COMMENT '消息正文',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '消息创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '消息更新时间',
  `delete_flag` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 0正常 1删除',
  KEY `idx_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='聊天会话消息记录表';



-- nutrition_db.chat_session 定义

CREATE TABLE `chat_session` (
  `session_id` bigint NOT NULL COMMENT '雪花算法生成会话主键ID',
  `user_id` bigint NOT NULL COMMENT '登录用户id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '会话创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '会话更新时间',
  `delete_flag` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 0正常 1删除',
  PRIMARY KEY (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI聊天会话主表';



-- nutrition_db.sys_admin 定义

CREATE TABLE `sys_admin` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '管理员主键ID',
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '登录账号，唯一',
  `password` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '加密后的登录密码',
  `nickname` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '管理员昵称',
  `file_ids` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '头像地址',
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '联系手机号',
  `delete_flag` tinyint DEFAULT '0' COMMENT '逻辑删除：0未删除 1已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统管理员表';




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




-- nutrition_db.sys_user 定义

CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `openid` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '微信openid',
  `nickname` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '新用户' COMMENT '昵称',
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




-- nutrition_db.user_feedback 定义

CREATE TABLE `user_feedback` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键自增ID',
  `user_id` bigint NOT NULL COMMENT '关联用户表主键user_id',
  `feedback_content` varchar(2000) NOT NULL COMMENT '用户反馈内容',
  `feedback_status` tinyint DEFAULT '0' COMMENT '处理状态 0待处理 1处理中 2已完结',
  `admin_reply` varchar(1000) DEFAULT NULL COMMENT '后台管理员回复内容',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `delete_flag` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 0未删除 1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_user_delete` (`user_id`,`delete_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户问题反馈表';


-- nutrition_db.rag_knowledge_document 定义

CREATE TABLE `rag_knowledge_document` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文档ID',
  `doc_name` varchar(255) NOT NULL COMMENT '文档名称',
  `file_md5` varchar(64) NOT NULL COMMENT '文件MD5，用于查重',
  `upload_user_id` bigint DEFAULT NULL COMMENT '上传管理员ID',
  `status` tinyint DEFAULT '2' COMMENT '状态：1正常 2向量入库中 3入库失败 4已删除',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注说明',
  `vector_store_id` varchar(255) DEFAULT '' COMMENT '阿里云向量库文档分组ID',
  `file_ids` varchar(2000) DEFAULT NULL COMMENT '关联附件表(sys_file)主键ID，多个用逗号分隔',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `delete_flag` tinyint DEFAULT '0' COMMENT '逻辑删除 0未删除 1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_md5` (`file_md5`),
  KEY `idx_upload_user` (`upload_user_id`),
  KEY `idx_delete_flag` (`delete_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='RAG知识库文档表';

-- 如果表已存在，添加file_ids字段
-- ALTER TABLE `rag_knowledge_document` ADD COLUMN `file_ids` varchar(2000) DEFAULT NULL COMMENT '关联附件表(sys_file)主键ID，多个用逗号分隔' AFTER `vector_store_id`;