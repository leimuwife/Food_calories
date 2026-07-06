package com.nutrition.controller;

import com.nutrition.common.Result;
import com.nutrition.entity.Attachment;
import com.nutrition.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 附件控制器
 * 提供附件上传、下载、查询等接口
 */
@RestController
@RequestMapping("/attachment")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping("/upload")
    public Result<Attachment> upload(@RequestParam("file") MultipartFile file,
                                     @RequestParam(value = "userId", required = false) Long userId) {
        if (file.isEmpty()) {
            return Result.fail("请选择要上传的文件");
        }

        String originalFilename = file.getOriginalFilename();
        String fileSuffix = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
        Long fileSize = file.getSize();

        String storageFileName = UUID.randomUUID().toString() + fileSuffix;
        String storagePath = System.getProperty("user.dir") + "/uploads/";
        File storageDir = new File(storagePath);
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        File targetFile = new File(storagePath + storageFileName);
        try {
            file.transferTo(targetFile);
        } catch (IOException e) {
            return Result.fail("文件上传失败");
        }

        String fileUrl = "/uploads/" + storageFileName;

        Attachment attachment = attachmentService.saveAttachment(
                originalFilename,
                fileSuffix.substring(1),
                fileSize,
                fileUrl,
                1,
                userId
        );

        return Result.ok("上传成功", attachment);
    }

    @GetMapping("/{id}/url")
    public Result<Map<String, String>> getAttachmentUrl(@PathVariable Long id) {
        Attachment attachment = attachmentService.getById(id);
        if (attachment == null) {
            return Result.fail("附件不存在");
        }
        Map<String, String> result = new HashMap<>();
        result.put("url", attachment.getFileUrl());
        return Result.ok(result);
    }
}