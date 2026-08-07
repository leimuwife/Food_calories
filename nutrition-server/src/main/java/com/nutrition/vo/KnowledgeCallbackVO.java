package com.nutrition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Python回调处理结果VO
 */
@Data
@Schema(description = "Python回调处理结果")
public class KnowledgeCallbackVO {

    /** 是否已接收处理 */
    @Schema(description = "是否已接收处理")
    private Boolean received;

    /** 文档ID */
    @Schema(description = "文档ID")
    private String docId;

    public KnowledgeCallbackVO(Boolean received, String docId) {
        this.received = received;
        this.docId = docId;
    }

    public KnowledgeCallbackVO() {
    }
}
