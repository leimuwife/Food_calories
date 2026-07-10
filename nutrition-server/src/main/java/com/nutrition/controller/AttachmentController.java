package com.nutrition.controller;

import com.nutrition.common.BusinessException;
import com.nutrition.common.Result;
import com.nutrition.entity.Attachment;
import com.nutrition.entity.SysUser;
import com.nutrition.enums.AuditSceneEnum;
import com.nutrition.enums.AuditSuggestEnum;
import com.nutrition.service.AttachmentService;
import com.nutrition.service.ContentAuditService;
import com.nutrition.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
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
    private final ContentAuditService contentAuditService;
    private final UserService userService;

    @PostMapping("/upload")
    @Operation(summary = "单文件上传", description = "上传单个文件到阿里云OSS，包含微信内容安全审核")
    public Result<Map<String, Object>> upload(
            @Parameter(description = "上传文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "上传用户ID") @RequestParam(value = "userId", required = false) Long userId,
            @Parameter(description = "文件前缀路径，如 diet/、avatar/") @RequestParam(value = "prefix", required = false) String prefix,
            HttpServletRequest request) throws IOException {

        if (userId == null) {
            userId = (Long) request.getAttribute("userId");
        }

        Attachment attachment = attachmentService.upload(file, userId, prefix);

        if (isImageFile(file.getOriginalFilename())) {
            String openid = getOpenid(userId);
            try {
                AuditSuggestEnum imageAuditResult = contentAuditService.auditImages(userId, openid, java.util.Collections.singletonList(String.valueOf(attachment.getId())), AuditSceneEnum.PROFILE);
                if (imageAuditResult == AuditSuggestEnum.RISKY) {
                    log.warn("图片审核需要人工审核: userId={}, attachmentId={}", userId, attachment.getId());
                    attachmentService.delete(attachment.getId());
                    throw new BusinessException(400, "图片需要人工审核，请稍后重试");
                } else if (imageAuditResult == AuditSuggestEnum.BLOCK) {
                    log.warn("图片审核未通过: userId={}, attachmentId={}", userId, attachment.getId());
                    attachmentService.delete(attachment.getId());
                    throw new BusinessException(400, "图片包含违规内容，请更换图片后重新提交");
                }
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                attachmentService.delete(attachment.getId());
                log.error("图片审核异常: userId={}, attachmentId={}, error={}", userId, attachment.getId(), e.getMessage(), e);
                throw new BusinessException(500, "图片审核服务暂时不可用，请稍后重试");
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", attachment.getId());
        result.put("fileUrl", attachment.getFileUrl());
        result.put("fileName", attachment.getFileName());
        result.put("fileSize", attachment.getFileSize());

        return Result.ok(result);
    }

    @PostMapping("/upload/batch")
    @Operation(summary = "多文件上传", description = "上传多个文件到阿里云OSS，包含微信内容安全审核")
    public Result<List<Map<String, Object>>> uploadBatch(
            @Parameter(description = "上传文件列表") @RequestParam("files") List<MultipartFile> files,
            @Parameter(description = "上传用户ID") @RequestParam(value = "userId", required = false) Long userId,
            @Parameter(description = "文件前缀路径，如 diet/、avatar/") @RequestParam(value = "prefix", required = false) String prefix,
            HttpServletRequest request) throws IOException {

        if (userId == null) {
            userId = (Long) request.getAttribute("userId");
        }

        List<Attachment> attachments = attachmentService.uploadBatch(files, userId, prefix);
        List<Long> uploadedAttachmentIds = new ArrayList<>();
        List<String> imageFileIds = new ArrayList<>();

        for (int i = 0; i < attachments.size(); i++) {
            Attachment attachment = attachments.get(i);
            uploadedAttachmentIds.add(attachment.getId());
            if (isImageFile(files.get(i).getOriginalFilename())) {
                imageFileIds.add(String.valueOf(attachment.getId()));
            }
        }

        if (!imageFileIds.isEmpty()) {
            String openid = getOpenid(userId);
            try {
                AuditSuggestEnum imageAuditResult = contentAuditService.auditImages(userId, openid, imageFileIds, AuditSceneEnum.PROFILE);
                if (imageAuditResult == AuditSuggestEnum.RISKY) {
                    log.warn("批量图片审核需要人工审核: userId={}, attachmentIds={}", userId, imageFileIds);
                    for (Long id : uploadedAttachmentIds) {
                        attachmentService.delete(id);
                    }
                    throw new BusinessException(400, "图片需要人工审核，请稍后重试");
                } else if (imageAuditResult == AuditSuggestEnum.BLOCK) {
                    log.warn("批量图片审核未通过: userId={}, attachmentIds={}", userId, imageFileIds);
                    for (Long id : uploadedAttachmentIds) {
                        attachmentService.delete(id);
                    }
                    throw new BusinessException(400, "图片包含违规内容，请更换图片后重新提交");
                }
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                for (Long id : uploadedAttachmentIds) {
                    attachmentService.delete(id);
                }
                log.error("批量图片审核异常: userId={}, attachmentIds={}, error={}", userId, uploadedAttachmentIds, e.getMessage(), e);
                throw new BusinessException(500, "图片审核服务暂时不可用，请稍后重试");
            }
        }

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

    private boolean isImageFile(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }
        String lowerFileName = fileName.toLowerCase();
        return lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg") ||
                lowerFileName.endsWith(".png") || lowerFileName.endsWith(".gif") ||
                lowerFileName.endsWith(".bmp") || lowerFileName.endsWith(".webp");
    }

    private String getOpenid(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            SysUser user = userService.getCurrentUser(userId);
            return user != null ? user.getOpenid() : null;
        } catch (Exception e) {
            log.warn("获取用户openid失败: userId={}, error={}", userId, e.getMessage());
            return null;
        }
    }
}