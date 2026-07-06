package com.nutrition.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 附件表实体
 */
@Data
@TableName("attachment")
public class Attachment {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 原始文件名 */
    private String fileName;

    /** 文件后缀：jpg/png/pdf */
    private String fileSuffix;

    /** 文件大小(字节) */
    private Long fileSize;

    /** 文件线上访问地址 */
    private String fileUrl;

    /** 存储类型 1本地 2OSS对象存储 */
    private Integer storageType;

    /** 上传人用户ID */
    private Long uploadUserId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除 0正常 1已删除 */
    @TableLogic
    private String deleteFlag;
}