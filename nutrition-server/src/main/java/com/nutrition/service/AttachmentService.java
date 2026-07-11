package com.nutrition.service;

import com.nutrition.entity.Attachment;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface AttachmentService {

    Attachment upload(MultipartFile file, Long userId) throws IOException;

    Attachment upload(MultipartFile file, Long userId, String prefix) throws IOException;

    List<Attachment> uploadBatch(List<MultipartFile> files, Long userId) throws IOException;

    List<Attachment> uploadBatch(List<MultipartFile> files, Long userId, String prefix) throws IOException;

    Attachment getById(Long id);

    String getUrl(Long id);

    boolean delete(Long id);

    void deleteBatch(List<Long> ids);

    /**
     * 根据ID列表批量查询附件
     * 使用IN查询一次性获取全部附件信息，仅1次DB请求
     *
     * @param idList 附件ID列表
     * @return 附件列表
     */
    List<Attachment> batchGetByIds(List<Long> idList);
}