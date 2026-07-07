package com.nutrition.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 全局统一附件文件实体类
 * 对应数据库表 sys_file
 */
@Data
@TableName("sys_file")
public class Attachment extends Common {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "雪花算法文件主键ID")
    private Long id;

    @TableField("file_name")
    @Schema(description = "原始文件名")
    private String fileName;

    @TableField("file_suffix")
    @Schema(description = "文件后缀：jpg/png/pdf")
    private String fileSuffix;

    @TableField("file_size")
    @Schema(description = "文件大小(字节)")
    private Long fileSize;

    @TableField("file_url")
    @Schema(description = "文件线上访问地址")
    private String fileUrl;

    @TableField("storage_type")
    @Schema(description = "存储类型 1本地 2OSS对象存储")
    private Integer storageType;

    @TableField("upload_user_id")
    @Schema(description = "上传人用户ID")
    private Long uploadUserId;

    @TableField("delete_flag")
    @Schema(description = "逻辑删除 0正常 1已删除")
    private Integer deleteFlag;
}