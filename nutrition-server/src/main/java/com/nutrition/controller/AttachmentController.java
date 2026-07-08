package com.nutrition.controller;

import com.nutrition.common.Result;
import com.nutrition.entity.Attachment;
import com.nutrition.service.AttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/attachment")
@RequiredArgsConstructor
@Tag(name = "附件管理", description = "文件上传、下载、删除接口")
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping("/upload")
    @Operation(summary = "单文件上传", description = "上传单个文件到阿里云OSS")
    public Result<Map<String, Object>> upload(
            @Parameter(description = "上传文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "上传用户ID") @RequestParam(value = "userId", required = false) Long userId,
            @Parameter(description = "文件前缀路径，如 diet/、avatar/") @RequestParam(value = "prefix", required = false) String prefix) throws IOException {

        Attachment attachment = attachmentService.upload(file, userId, prefix);

        Map<String, Object> result = new HashMap<>();
        result.put("id", attachment.getId());
        result.put("fileUrl", attachment.getFileUrl());
        result.put("fileName", attachment.getFileName());
        result.put("fileSize", attachment.getFileSize());

        return Result.ok(result);
    }

    @PostMapping("/upload/batch")
    @Operation(summary = "多文件上传", description = "上传多个文件到阿里云OSS")
    public Result<List<Map<String, Object>>> uploadBatch(
            @Parameter(description = "上传文件列表") @RequestParam("files") List<MultipartFile> files,
            @Parameter(description = "上传用户ID") @RequestParam(value = "userId", required = false) Long userId,
            @Parameter(description = "文件前缀路径，如 diet/、avatar/") @RequestParam(value = "prefix", required = false) String prefix) throws IOException {

        List<Attachment> attachments = attachmentService.uploadBatch(files, userId, prefix);

        List<Map<String, Object>> result = attachments.stream().map(attachment -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", attachment.getId());
            item.put("fileUrl", attachment.getFileUrl());
            item.put("fileName", attachment.getFileName());
            item.put("fileSize", attachment.getFileSize());
            return item;
        }).collect(Collectors.toList());

        return Result.ok(result);
    }

    @GetMapping("/{id}/url")
    @Operation(summary = "获取文件URL", description = "根据附件ID获取文件访问地址")
    public Result<Map<String, String>> getUrl(
            @Parameter(description = "附件ID") @PathVariable("id") Long id) {

        String url = attachmentService.getUrl(id);

        Map<String, String> result = new HashMap<>();
        result.put("url", url != null ? url : "");

        return Result.ok(result);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除附件", description = "删除指定附件（同时删除OSS文件）")
    public Result<Void> delete(
            @Parameter(description = "附件ID") @PathVariable("id") Long id) {

        boolean success = attachmentService.delete(id);
        if (success) {
            return Result.ok();
        } else {
            return Result.fail("附件不存在");
        }
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除附件", description = "批量删除指定附件")
    public Result<Void> deleteBatch(
            @Parameter(description = "附件ID列表") @RequestBody List<Long> ids) {

        attachmentService.deleteBatch(ids);
        return Result.ok();
    }
}