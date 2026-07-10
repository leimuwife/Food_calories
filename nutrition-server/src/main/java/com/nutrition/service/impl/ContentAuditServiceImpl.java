package com.nutrition.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutrition.common.BusinessException;
import com.nutrition.enums.AuditSceneEnum;
import com.nutrition.enums.AuditSuggestEnum;
import com.nutrition.exception.WxNetworkException;
import com.nutrition.entity.Attachment;
import com.nutrition.entity.ContentAuditRecord;
import com.nutrition.mapper.ContentAuditRecordMapper;
import com.nutrition.service.AttachmentService;
import com.nutrition.service.ContentAuditService;
import com.nutrition.util.WxTokenUtil;
import com.github.houbb.sensitive.word.core.SensitiveWordHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 内容审核服务实现类
 * 调用微信官方内容安全接口进行文本和图片审核
 *
 * 微信官方 scene 取值对照表：
 * 1 = 资料（个人资料、头像、昵称等）
 * 2 = 评论（评论、回复等）
 * 3 = 论坛（帖子、动态等）
 * 4 = 社交日志（朋友圈、说说等）
 * 5 = 私信（私聊消息等）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContentAuditServiceImpl extends ServiceImpl<ContentAuditRecordMapper, ContentAuditRecord> implements ContentAuditService {

    private final WxTokenUtil wxTokenUtil;
    private final ObjectMapper objectMapper;
    private final AttachmentService attachmentService;
    private final RestTemplate restTemplate;
    private final TransactionTemplate transactionTemplate;
    /**
     * 微信官方 msgSecCheck 接口URL
     */
    private static final String MSG_SEC_CHECK_URL = "https://api.weixin.qq.com/wxa/msg_sec_check?access_token=%s";
    private static final String IMG_SEC_CHECK_URL = "https://api.weixin.qq.com/wxa/img_sec_check?access_token=%s";

    /**
     * 连接超时时间，单位毫秒，10000ms = 10 秒。
     * 作用：请求微信接口、下载图片时，建立 TCP 连接最多等 10 秒
     */
    private static final int CONNECTION_TIMEOUT_MS = 10000;
    /**
     * 读取超时时间，单位毫秒，15000ms = 15 秒。
     * 作用：请求微信接口、下载图片时，读取响应体最多等 15 秒
     */
    private static final int READ_TIMEOUT_MS = 15000;
    /**
     * 最大图片大小，单位字节，2MB。
     * 作用：审核图片时，限制图片大小，防止内存溢出
     */
    private static final int MAX_IMAGE_SIZE_BYTES = 2 * 1024 * 1024;

    private static final int ERR_CODE_RISKY = 87014;
    private static final int ERR_CODE_BLOCK = 87001;

    @Override
    @Transactional
    public AuditSuggestEnum auditText(Long userId, String openid, String content, AuditSceneEnum scene) {
        log.info("开始文本审核: userId={}, openid={}, scene={}, contentLength={}", userId, openid, scene.getDescription(), content != null ? content.length() : 0);

        if (content == null || content.trim().isEmpty()) {
            log.info("文本内容为空，跳过审核");
            return AuditSuggestEnum.PASS;
        }

        validateScene(scene);

        String contentSummary = content.length() > 50 ? content.substring(0, 50) + "..." : content;

        List<String> sensitiveWords = SensitiveWordHelper.findAll(content);
        if (!sensitiveWords.isEmpty()) {
            log.warn("本地敏感词命中: userId={}, words={}", userId, sensitiveWords);
            String label = String.join(",", sensitiveWords);
            saveAuditRecord(createRecord(userId, openid, "text", content, null, scene, AuditSuggestEnum.BLOCK, label));
            throw new BusinessException(400, "内容包含违规信息，请修改后重新提交");
        }

        try {
            AuditResult result = doAuditText(content, openid, scene);
            logAuditResult(userId, openid, "text", content, null, scene, result.suggest, result.label);

            if (result.suggest == AuditSuggestEnum.RISKY) {
                saveAuditRecord(createRecord(userId, openid, "text", content, null, scene, result.suggest, result.label));
                throw new BusinessException(400, "内容需要人工审核，请稍后重试");
            } else if (result.suggest == AuditSuggestEnum.BLOCK) {
                saveAuditRecord(createRecord(userId, openid, "text", content, null, scene, result.suggest, result.label));
                throw new BusinessException(400, "内容包含违规信息，请修改后重新提交");
            }

            saveAuditRecord(createRecord(userId, openid, "text", content, null, scene, result.suggest, result.label));
            return AuditSuggestEnum.PASS;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("文本审核异常: userId={}, content={}, error={}", userId, contentSummary, e.getMessage(), e);
            throw new BusinessException(500, "内容审核服务暂时不可用，请稍后重试");
        }
    }

    @Override
    @Transactional
    public AuditSuggestEnum auditImages(Long userId, String openid, List<String> fileIds, AuditSceneEnum scene) {
        log.info("开始图片审核: userId={}, openid={}, scene={}, fileCount={}", userId, openid, scene.getDescription(), fileIds != null ? fileIds.size() : 0);

        if (fileIds == null || fileIds.isEmpty()) {
            log.info("图片列表为空，跳过审核");
            return AuditSuggestEnum.PASS;
        }

        validateScene(scene);

        String allFileIds = String.join(",", fileIds);

        ContentAuditRecord batchRecord = createRecord(userId, openid, "image", null, allFileIds, scene, AuditSuggestEnum.PASS, null);
        saveAuditRecord(batchRecord);
        Long recordId = batchRecord.getId();
        log.debug("图片批次审核记录已创建: recordId={}, fileIds={}", recordId, allFileIds);

        AtomicReference<String> tokenRef = new AtomicReference<>(wxTokenUtil.getAccessToken());

        AuditResult firstViolationResult = null;

        try {
            for (String fileId : fileIds) {
                Long attachmentId = Long.parseLong(fileId);
                Attachment attachment = attachmentService.getById(attachmentId);

                if (attachment == null) {
                    log.warn("附件不存在: attachmentId={}", attachmentId);
                    throw new BusinessException(400, "附件不存在");
                }

                String imageUrl = attachment.getFileUrl();
                if (!isValidHttpsUrl(imageUrl)) {
                    log.warn("图片URL格式不正确: {}", imageUrl);
                    throw new BusinessException(400, "图片地址格式不正确，请使用公网HTTPS地址");
                }

                AuditResult result = doAuditImage(imageUrl, tokenRef);
                logAuditResult(userId, openid, "image", null, fileId, scene, result.suggest, result.label);

                if (result.suggest == AuditSuggestEnum.RISKY || result.suggest == AuditSuggestEnum.BLOCK) {
                    firstViolationResult = result;
                    break;
                }
            }

            if (firstViolationResult != null) {
                updateAuditRecord(recordId, firstViolationResult.suggest, firstViolationResult.label, 1);
                log.warn("图片审核违规: recordId={}, suggest={}, label={}", recordId, firstViolationResult.suggest.getLabel(), firstViolationResult.label);

                if (firstViolationResult.suggest == AuditSuggestEnum.RISKY) {
                    throw new BusinessException(400, "图片需要人工审核，请稍后重试");
                } else {
                    throw new BusinessException(400, "图片包含违规内容，请更换图片后重新提交");
                }
            }

            return AuditSuggestEnum.PASS;

        } catch (NumberFormatException e) {
            log.error("附件ID格式错误: {}", fileIds, e);
            throw new BusinessException(400, "附件ID格式错误");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("图片审核异常: userId={}, fileIds={}, error={}", userId, fileIds, e.getMessage(), e);
            throw new BusinessException(500, "图片审核服务暂时不可用，请稍后重试");
        }
    }

    private AuditResult doAuditText(String content, String openid, AuditSceneEnum scene) {
        String token = wxTokenUtil.getAccessToken();
        String url = String.format(MSG_SEC_CHECK_URL, token);

        Map<String, Object> body = new HashMap<>();
        body.put("content", content);
        if (openid != null && !openid.isEmpty()) {
            body.put("openid", openid);
        }
        body.put("scene", scene.getCode());

        String response = doPostJson(url, toJson(body));
        JsonNode result = parseJson(response);

        int errCode = result.has("errcode") ? result.get("errcode").asInt() : 0;
        String errMsg = result.has("errmsg") ? result.get("errmsg").asText() : "";

        if (errCode != 0) {
            log.warn("微信文本审核接口返回错误: errcode={}, errmsg={}", errCode, errMsg);

            if (wxTokenUtil.isTokenExpired(errCode)) {
                log.info("access_token 过期，强制刷新后重试");
                token = wxTokenUtil.forceRefreshAccessToken();
                url = String.format(MSG_SEC_CHECK_URL, token);
                response = doPostJson(url, toJson(body));
                result = parseJson(response);
                errCode = result.has("errcode") ? result.get("errcode").asInt() : 0;
                errMsg = result.has("errmsg") ? result.get("errmsg").asText() : "";
            }

            if (errCode == ERR_CODE_RISKY) {
                String label = result.has("label") ? result.get("label").asText() : null;
                log.info("文本审核结果: 风险内容(RISKY), label={}", label);
                return new AuditResult(AuditSuggestEnum.RISKY, label);
            } else if (errCode == ERR_CODE_BLOCK) {
                String label = result.has("label") ? result.get("label").asText() : null;
                log.info("文本审核结果: 违规内容(BLOCK), label={}", label);
                return new AuditResult(AuditSuggestEnum.BLOCK, label);
            } else if (errCode != 0) {
                throw new BusinessException(500, "微信文本审核失败: errcode=" + errCode + ", errmsg=" + errMsg);
            }
        }

        String suggest = result.has("suggest") ? result.get("suggest").asText() : "";
        String label = result.has("label") ? result.get("label").asText() : null;

        AuditSuggestEnum auditResult = AuditSuggestEnum.fromCode(suggest);
        log.debug("文本审核结果: suggest={}, label={}", suggest, label);

        return new AuditResult(auditResult, label);
    }

    private AuditResult doAuditImage(String imageUrl, AtomicReference<String> tokenRef) {
        String token = tokenRef.get();
        String url = String.format(IMG_SEC_CHECK_URL, token);

        byte[] imageBytes = downloadImage(imageUrl);
        String fileName = extractFileName(imageUrl);

        String response = doPostMultipart(url, imageBytes, fileName);
        JsonNode result = parseJson(response);

        int errCode = result.has("errcode") ? result.get("errcode").asInt() : 0;
        String errMsg = result.has("errmsg") ? result.get("errmsg").asText() : "";

        if (errCode != 0) {
            log.warn("微信图片审核接口返回错误: errcode={}, errmsg={}", errCode, errMsg);

            if (wxTokenUtil.isTokenExpired(errCode)) {
                log.info("access_token 过期，强制刷新后重试");
                String newToken = wxTokenUtil.forceRefreshAccessToken();
                tokenRef.set(newToken);
                url = String.format(IMG_SEC_CHECK_URL, newToken);
                response = doPostMultipart(url, imageBytes, fileName);
                result = parseJson(response);
                errCode = result.has("errcode") ? result.get("errcode").asInt() : 0;
                errMsg = result.has("errmsg") ? result.get("errmsg").asText() : "";
            }

            if (errCode == ERR_CODE_RISKY) {
                String label = result.has("label") ? result.get("label").asText() : null;
                log.info("图片审核结果: 风险内容(RISKY), label={}, imageUrl={}", label, imageUrl);
                return new AuditResult(AuditSuggestEnum.RISKY, label);
            } else if (errCode == ERR_CODE_BLOCK) {
                String label = result.has("label") ? result.get("label").asText() : null;
                log.info("图片审核结果: 违规内容(BLOCK), label={}, imageUrl={}", label, imageUrl);
                return new AuditResult(AuditSuggestEnum.BLOCK, label);
            } else if (errCode != 0) {
                throw new BusinessException(500, "微信图片审核失败: errcode=" + errCode + ", errmsg=" + errMsg);
            }
        }

        String suggest = result.has("suggest") ? result.get("suggest").asText() : "";
        String label = result.has("label") ? result.get("label").asText() : null;

        AuditSuggestEnum auditResult = AuditSuggestEnum.fromCode(suggest);
        log.debug("图片审核结果: suggest={}, label={}, imageUrl={}", suggest, label, imageUrl);

        return new AuditResult(auditResult, label);
    }

    private byte[] downloadImage(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            try (InputStream is = url.openStream();
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int len;
                int totalRead = 0;
                while ((len = is.read(buffer)) != -1) {
                    totalRead += len;
                    if (totalRead > MAX_IMAGE_SIZE_BYTES) {
                        throw new RuntimeException("图片大小超过限制，最大支持2MB");
                    }
                    baos.write(buffer, 0, len);
                }
                byte[] bytes = baos.toByteArray();
                log.debug("图片下载完成: url={}, size={}bytes", imageUrl, bytes.length);
                return bytes;
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("下载图片失败: url={}, error={}", imageUrl, e.getMessage(), e);
            throw new RuntimeException("下载图片失败: " + imageUrl, e);
        }
    }

    private String extractFileName(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return "image.jpg";
        }
        int lastSlash = imageUrl.lastIndexOf('/');
        String fileName = lastSlash >= 0 ? imageUrl.substring(lastSlash + 1) : "image.jpg";
        int queryIndex = fileName.indexOf('?');
        if (queryIndex >= 0) {
            fileName = fileName.substring(0, queryIndex);
        }
        if (!fileName.contains(".")) {
            fileName = fileName + ".jpg";
        }
        return fileName;
    }

    private String doPostJson(String url, String jsonBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        try {
            return restTemplate.postForObject(url, entity, String.class);
        } catch (RestClientException e) {
            throw new WxNetworkException("HTTP POST 请求失败: " + url, e);
        }
    }

    private String doPostMultipart(String url, byte[] fileBytes, String fileName) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("media", new ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() {
                return fileName;
            }
        });

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            return response.getBody();
        } catch (RestClientException e) {
            throw new WxNetworkException("HTTP multipart 请求失败: " + url, e);
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("JSON序列化失败", e);
        }
    }

    private JsonNode parseJson(String json) {
        if (json == null || json.isEmpty()) {
            throw new RuntimeException("响应为空");
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            log.error("JSON解析失败: json={}", json);
            throw new RuntimeException("JSON解析失败", e);
        }
    }

    private void validateScene(AuditSceneEnum scene) {
        if (scene == null) {
            throw new BusinessException(400, "审核场景不能为空");
        }
        int code = scene.getCode();
        if (code < 1 || code > 5) {
            log.warn("审核场景不在微信官方支持范围内: scene={}, code={}", scene.getDescription(), code);
        }
    }

    private ContentAuditRecord createRecord(Long userId, String openid, String auditType,
                                             String contentText, String fileIds,
                                             AuditSceneEnum scene, AuditSuggestEnum suggest, String label) {
        ContentAuditRecord record = new ContentAuditRecord();
        record.setUserId(userId);
        record.setOpenid(openid);
        record.setAuditType("text".equals(auditType) ? 1 : 2);
        record.setContentText(contentText);
        record.setFileIds(fileIds);
        record.setScene(scene.getCode());
        record.setSuggest(suggest.getCode());
        record.setLabel(label);
        record.setAuditTime(LocalDateTime.now());
        record.setReviewStatus(suggest == AuditSuggestEnum.RISKY ? 1 : 0);
        record.setDeleteFlag(0);
        return record;
    }

    private void logAuditResult(Long userId, String openid, String auditType,
                                 String content, String fileIds,
                                 AuditSceneEnum scene, AuditSuggestEnum suggest, String label) {
        String contentInfo = "text".equals(auditType)
                ? (content != null && content.length() > 50 ? content.substring(0, 50) + "..." : content)
                : fileIds;

        log.info("审核完成: userId={}, openid={}, type={}, scene={}, suggest={}, label={}, content={}",
                userId, openid, auditType, scene.getDescription(), suggest.getLabel(), label, contentInfo);
    }

    private boolean isValidHttpsUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        try {
            java.net.URL u = new java.net.URL(url);
            return "https".equalsIgnoreCase(u.getProtocol());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContentAuditRecord> getPendingReviewRecords() {
        return getBaseMapper().selectPendingReviewRecords();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContentAuditRecord> getRecordsByUserId(Long userId) {
        return getBaseMapper().selectByUserId(userId);
    }

    @Override
    public void saveAuditRecord(ContentAuditRecord record) {
        transactionTemplate.setPropagationBehavior(Propagation.REQUIRES_NEW.value());
        transactionTemplate.execute(status -> {
            save(record);
            log.debug("审核记录已保存(独立事务): id={}, type={}, suggest={}", record.getId(), record.getAuditType(), record.getSuggest());
            return null;
        });
    }

    public void updateAuditRecord(Long recordId, AuditSuggestEnum suggest, String label, Integer reviewStatus) {
        transactionTemplate.setPropagationBehavior(Propagation.REQUIRES_NEW.value());
        transactionTemplate.execute(status -> {
            LambdaUpdateWrapper<ContentAuditRecord> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(ContentAuditRecord::getId, recordId)
                    .set(ContentAuditRecord::getSuggest, suggest.getCode())
                    .set(ContentAuditRecord::getLabel, label)
                    .set(ContentAuditRecord::getReviewStatus, reviewStatus);

            update(wrapper);
            log.debug("审核记录已更新(独立事务): recordId={}, suggest={}, label={}, reviewStatus={}", recordId, suggest.getCode(), label, reviewStatus);
            return null;
        });
    }

    @Override
    @Transactional
    public void processReview(Long recordId, Integer reviewResult, String reviewRemark) {
        ContentAuditRecord record = getById(recordId);
        if (record == null) {
            throw new BusinessException(404, "审核记录不存在");
        }

        LambdaUpdateWrapper<ContentAuditRecord> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ContentAuditRecord::getId, recordId)
                .set(ContentAuditRecord::getReviewStatus, 2)
                .set(ContentAuditRecord::getReviewResult, reviewResult)
                .set(ContentAuditRecord::getReviewRemark, reviewRemark);

        update(wrapper);
        log.info("人工复审完成: recordId={}, reviewResult={}, reviewRemark={}", recordId, reviewResult, reviewRemark);
    }

    private static class AuditResult {
        final AuditSuggestEnum suggest;
        final String label;

        AuditResult(AuditSuggestEnum suggest, String label) {
            this.suggest = suggest;
            this.label = label;
        }
    }
}
