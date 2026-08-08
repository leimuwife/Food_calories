package com.nutrition.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * RAG知识库-上传文档实体
 * 对应数据库表 rag_knowledge_document
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("rag_knowledge_document")
public class RagKnowledgeDocument extends Common {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 文档名称 */
    @TableField("doc_name")
    private String docName;

    /** 文件整体MD5，用于查重，和Python侧md5校验一致 */
    @TableField("file_md5")
    private String fileMd5;

    /** 上传管理员ID */
    @TableField("upload_user_id")
    private Long uploadUserId;

    /** 状态：1正常 2向量入库中 3入库失败 4已删除 */
    private Integer status;

    /** 备注说明 */
    private String remark;

    /** 阿里云向量库该文档分组ID，删除时用 */
    @TableField("vector_store_id")
    private String vectorStoreId;

    /** 附件表(sys_file)主键ID，多个用逗号分隔，参考食物图片存储方式 */
    @TableField("file_ids")
    private String fileIds;
}
