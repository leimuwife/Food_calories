package com.nutrition.vo;

import com.nutrition.enums.RagDocumentStatusEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库文档列表VO
 */
@Data
public class KnowledgeDocumentVO {

    /** 文档ID */
    private Long id;

    /** 文档名称 */
    private String fileName;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 文件大小（可读格式） */
    private String fileSizeText;

    /** 上传时间 */
    private LocalDateTime uploadTime;

    /** 文件MD5标识 */
    private String fileMd5;

    /** 状态 */
    private Integer status;

    /** 状态描述 */
    private String statusText;

    /** 附件在线访问地址（OSS） */
    private String fileUrl;

    public String getFileSizeText() {
        if (fileSize == null || fileSize == 0) {
            return "0 B";
        }
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.2f KB", fileSize / 1024.0);
        } else {
            return String.format("%.2f MB", fileSize / (1024.0 * 1024));
        }
    }

    public String getStatusText() {
        return RagDocumentStatusEnum.getDescByCode(status);
    }
}
