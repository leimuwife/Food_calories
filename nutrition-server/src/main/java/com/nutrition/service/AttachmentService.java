package com.nutrition.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nutrition.entity.Attachment;

import java.util.List;

/**
 * 附件服务接口
 */
public interface AttachmentService extends IService<Attachment> {

    List<Attachment> getAttachmentsByUser(Long userId);

    Attachment saveAttachment(String fileName, String fileSuffix, Long fileSize, 
                             String fileUrl, Integer storageType, Long uploadUserId);
}