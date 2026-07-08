package com.nutrition.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus自动填充处理器
 * 自动填充创建时间、更新时间和删除标记字段
 */
@Component
@Slf4j
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        
        if (metaObject.hasSetter("createTime")) {
            this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        }
        
        if (metaObject.hasSetter("updateTime")) {
            this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        }
        
        if (metaObject.hasSetter("deleteFlag")) {
            Object deleteFlag = metaObject.getValue("deleteFlag");
            if (deleteFlag == null) {
                this.strictInsertFill(metaObject, "deleteFlag", Integer.class, 0);
            }
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        if (metaObject.hasSetter("updateTime")) {
            this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        }
    }
}
