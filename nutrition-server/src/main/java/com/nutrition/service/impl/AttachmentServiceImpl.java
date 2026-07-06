package com.nutrition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nutrition.entity.Attachment;
import com.nutrition.mapper.AttachmentMapper;
import com.nutrition.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 附件服务实现类
 */
@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl extends ServiceImpl<AttachmentMapper, Attachment> implements AttachmentService {

    @Override
    public List<Attachment> getAttachmentsByUser(Long userId) {
        return this.list(new LambdaQueryWrapper<Attachment>()
                .eq(Attachment::getUploadUserId, userId)
                .eq(Attachment::getDeleteFlag, "0")
                .orderByDesc(Attachment::getCreateTime));
    }

    @Override
    public Attachment saveAttachment(String fileName, String fileSuffix, Long fileSize,
                                     String fileUrl, Integer storageType, Long uploadUserId) {
        Attachment attachment = new Attachment();
        attachment.setFileName(fileName);
        attachment.setFileSuffix(fileSuffix);
        attachment.setFileSize(fileSize);
        attachment.setFileUrl(fileUrl);
        attachment.setStorageType(storageType);
        attachment.setUploadUserId(uploadUserId);
        this.save(attachment);
        return attachment;
    }
}