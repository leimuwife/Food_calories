package com.nutrition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 知识库文档上传结果VO
 */
@Data
@Schema(description = "知识库文档上传结果")
public class KnowledgeUploadVO {

    /** 是否上传成功 */
    @Schema(description = "是否上传成功")
    private Boolean success;

    /** 是否为重复文件 */
    @Schema(description = "是否为重复文件")
    private Boolean duplicate;

    /** 上传后的文档信息 */
    @Schema(description = "上传后的文档信息")
    private KnowledgeDocumentVO document;

    public KnowledgeUploadVO(Boolean success, Boolean duplicate, KnowledgeDocumentVO document) {
        this.success = success;
        this.duplicate = duplicate;
        this.document = document;
    }

    public KnowledgeUploadVO() {
    }
}
