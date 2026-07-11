package com.nutrition.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutrition.common.BusinessException;
import com.nutrition.enums.AuditSceneEnum;
import com.nutrition.enums.AuditSuggestEnum;
import com.nutrition.enums.BizMsgEnum;
import com.nutrition.exception.WxNetworkException;
import com.nutrition.entity.Attachment;
import com.nutrition.entity.ContentAuditRecord;
import com.nutrition.mapper.ContentAuditRecordMapper;
import com.nutrition.config.WxConfigProperties;
import com.nutrition.service.AttachmentService;
import com.nutrition.service.ContentAuditService;
import com.nutrition.util.WxTokenUtil;
import com.github.houbb.sensitive.word.core.SensitiveWordHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.HttpURLConnection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

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
    private final WxConfigProperties wxConfigProperties;

    /**
     * 微信审核专用线程池
     * 用于图片审核并发处理，隔离业务线程，限制最大并发数防止微信限流
     */
    @jakarta.annotation.Resource(name = "wxAuditExecutor")
    private ExecutorService wxAuditExecutor;
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

    /**
     * 文本审核
     */
    @Override
    @Transactional
    public AuditSuggestEnum auditText(Long userId, String openid, String content, AuditSceneEnum scene) {
        log.info("开始文本审核: userId={}, openid={}, scene={}, contentLength={}", userId, openid, scene.getDescription(), content != null ? content.length() : 0);

        if (userId == null || userId <= 0) {
            throw new BusinessException(BizMsgEnum.USER_NOT_LOGIN);
        }

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
            throw new BusinessException(BizMsgEnum.AUDIT_CONTENT_BLOCK);
        }

        try {
            AuditResult result = doAuditText(content, openid, scene);
            logAuditResult(userId, openid, "text", content, null, scene, result.suggest, result.label);

            if (result.suggest == AuditSuggestEnum.RISKY) {
                saveAuditRecord(createRecord(userId, openid, "text", content, null, scene, result.suggest, result.label));
            throw new BusinessException(BizMsgEnum.AUDIT_CONTENT_RISKY);
            } else if (result.suggest == AuditSuggestEnum.BLOCK) {
                saveAuditRecord(createRecord(userId, openid, "text", content, null, scene, result.suggest, result.label));
                throw new BusinessException(BizMsgEnum.AUDIT_CONTENT_BLOCK);
            }

            saveAuditRecord(createRecord(userId, openid, "text", content, null, scene, result.suggest, result.label));
            return AuditSuggestEnum.PASS;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("文本审核异常: userId={}, content={}, error={}", userId, contentSummary, e.getMessage(), e);
            throw new BusinessException(BizMsgEnum.AUDIT_SERVICE_UNAVAILABLE);
        }
    }

    /**
     * 图片审核（并发优化版）
     * 核心优化策略：
     * 1. 附件批量查询：使用IN查询一次性获取全部附件信息，仅1次DB请求
     * 2. 图片预下载：提前批量拉取全部图片二进制缓存到内存，避免循环内重复OSS网络IO
     * 3. 并行审核：使用自定义线程池CompletableFuture并行执行多张图片微信审核
     * 4. 事务合并：审核记录批量插入，共用外层业务事务，仅最终一次commit刷库
     *
     * @param userId   用户ID
     * @param openid   用户微信openid
     * @param fileIds  待审核图片附件ID列表（附件表主键ID）
     * @param scene    业务场景
     * @return 审核结果
     */
    @Override
    @Transactional
    public AuditSuggestEnum auditImages(Long userId, String openid, List<String> fileIds, AuditSceneEnum scene) {
        log.info("开始图片审核: userId={}, openid={}, scene={}, fileCount={}", userId, openid, scene.getDescription(), fileIds != null ? fileIds.size() : 0);

        if (userId == null || userId <= 0) {
            throw new BusinessException(BizMsgEnum.USER_NOT_LOGIN);
        }

        if (fileIds == null || fileIds.isEmpty()) {
            log.info("图片列表为空，跳过审核");
            return AuditSuggestEnum.PASS;
        }

        validateScene(scene);

        String allFileIds = String.join(",", fileIds);

        // ========== 第1步：批量查询附件信息（消除循环单查）==========
        List<Long> attachmentIds = fileIds.stream()
                .map(Long::parseLong)
                .collect(Collectors.toList());

        List<Attachment> attachments = attachmentService.batchGetByIds(attachmentIds);
        Map<Long, Attachment> attachmentMap = attachments.stream()
                .collect(Collectors.toMap(Attachment::getId, a -> a));

        // 校验所有附件是否存在
        for (Long attachmentId : attachmentIds) {
            if (!attachmentMap.containsKey(attachmentId)) {
                log.warn("附件不存在: attachmentId={}", attachmentId);
                throw new BusinessException(BizMsgEnum.AUDIT_ATTACHMENT_NOT_EXIST);
            }
        }

        // ========== 第2步：预下载所有图片到内存（避免循环内重复OSS网络IO）==========
        Map<String, byte[]> imageBytesMap = new HashMap<>();
        Map<String, String> fileNameMap = new HashMap<>();

        for (String fileId : fileIds) {
            Long attachmentId = Long.parseLong(fileId);
            Attachment attachment = attachmentMap.get(attachmentId);
            String imageUrl = attachment.getFileUrl();

            if (!isValidHttpsUrl(imageUrl)) {
                log.warn("图片URL格式不正确: {}", imageUrl);
                throw new BusinessException(BizMsgEnum.AUDIT_IMAGE_URL_INVALID);
            }

            byte[] imageBytes = downloadImage(imageUrl);
            if (imageBytes == null || imageBytes.length == 0) {
                log.error("图片下载为空: imageUrl={}", imageUrl);
                throw new BusinessException(BizMsgEnum.AUDIT_IMAGE_DOWNLOAD_FAILED);
            }

            imageBytesMap.put(fileId, imageBytes);
            fileNameMap.put(fileId, extractFileName(imageUrl));
        }

        log.debug("图片预下载完成: 共 {} 张，已缓存到内存", fileIds.size());

        // ========== 第3步：并行审核图片（使用自定义线程池）==========
        final String token = wxTokenUtil.getAccessToken();
        List<CompletableFuture<ImageAuditResult>> futures = new ArrayList<>();

        for (String fileId : fileIds) {
            byte[] imageBytes = imageBytesMap.get(fileId);
            String fileName = fileNameMap.get(fileId);

            CompletableFuture<ImageAuditResult> future = CompletableFuture.supplyAsync(() -> {
                try {
                    String auditToken = token;
                    String url = String.format(IMG_SEC_CHECK_URL, auditToken);

                    boolean isV2 = wxConfigProperties.isAuditVersionV2();
                    log.debug("图片审核版本: v{}, fileId={}", isV2 ? 2 : 1, fileId);

                    String response = doPostMultipart(url, imageBytes, fileName);
                    JsonNode result = parseJson(response);

                    int errCode = result.has("errcode") ? result.get("errcode").asInt() : 0;
                    String errMsg = result.has("errmsg") ? result.get("errmsg").asText() : "";

                    if (errCode != 0) {
                        log.warn("微信图片审核接口返回错误: fileId={}, errcode={}, errmsg={}", fileId, errCode, errMsg);

                        if (wxTokenUtil.isTokenExpired(errCode)) {
                            log.info("access_token 过期，强制刷新后重试: fileId={}", fileId);
                            auditToken = wxTokenUtil.forceRefreshAccessToken();
                            url = String.format(IMG_SEC_CHECK_URL, auditToken);
                            response = doPostMultipart(url, imageBytes, fileName);
                            result = parseJson(response);
                            errCode = result.has("errcode") ? result.get("errcode").asInt() : 0;
                            errMsg = result.has("errmsg") ? result.get("errmsg").asText() : "";
                        }

                        if (errCode == ERR_CODE_RISKY) {
                            String label = result.has("label") ? result.get("label").asText() : null;
                            log.info("图片审核结果: 风险内容(RISKY), fileId={}, label={}", fileId, label);
                            return new ImageAuditResult(fileId, AuditSuggestEnum.RISKY, label);
                        } else if (errCode == ERR_CODE_BLOCK) {
                            String label = result.has("label") ? result.get("label").asText() : null;
                            log.info("图片审核结果: 违规内容(BLOCK), fileId={}, label={}", fileId, label);
                            return new ImageAuditResult(fileId, AuditSuggestEnum.BLOCK, label);
                        } else if (errCode != 0) {
                            throw new WxNetworkException("微信图片审核接口返回错误码: " + errCode);
                        }
                    }

                    JsonNode resultNode = result.has("result") ? result.get("result") : result;
                    String suggest = resultNode.has("suggest") ? resultNode.get("suggest").asText() : "";
                    String label = resultNode.has("label") ? resultNode.get("label").asText() : null;

                    AuditSuggestEnum auditResult = AuditSuggestEnum.fromCode(suggest);
                    log.debug("图片审核结果: fileId={}, suggest={}, label={}", fileId, suggest, label);

                    return new ImageAuditResult(fileId, auditResult, label);

                } catch (Exception e) {
                    log.error("单张图片审核异常: fileId={}, error={}", fileId, e.getMessage(), e);
                    return new ImageAuditResult(fileId, null, null);
                }
            }, wxAuditExecutor);

            futures.add(future);
        }

        // 等待所有审核任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 收集审核结果
        List<ImageAuditResult> auditResults = futures.stream()
                .map(future -> {
                    try {
                        return future.get();
                    } catch (Exception e) {
                        log.error("获取审核结果异常: error={}", e.getMessage(), e);
                        return null;
                    }
                })
                .filter(result -> result != null)
                .collect(Collectors.toList());

        // ========== 第4步：批量保存审核记录（共用外层事务）==========
        List<ContentAuditRecord> auditRecords = new ArrayList<>();
        AuditResult firstViolationResult = null;

        for (ImageAuditResult result : auditResults) {
            if (result.suggest == null) {
                throw new BusinessException(BizMsgEnum.AUDIT_IMAGE_SERVICE_UNAVAILABLE);
            }

            ContentAuditRecord record = createRecord(userId, openid, "image", null, result.fileId, scene, result.suggest, result.label);
            auditRecords.add(record);

            logAuditResult(userId, openid, "image", null, result.fileId, scene, result.suggest, result.label);

            if (firstViolationResult == null &&
                    (result.suggest == AuditSuggestEnum.RISKY || result.suggest == AuditSuggestEnum.BLOCK)) {
                firstViolationResult = new AuditResult(result.suggest, result.label);
            }
        }

        // 批量插入审核记录（共用外层事务，仅最终一次commit）
        saveBatch(auditRecords);
        log.debug("图片审核记录批量保存成功: 共 {} 条", auditRecords.size());

        // ========== 第5步：判断审核结果 ==========
        if (firstViolationResult != null) {
            log.warn("图片审核违规: userId={}, suggest={}, label={}", userId, firstViolationResult.suggest.getLabel(), firstViolationResult.label);

            if (firstViolationResult.suggest == AuditSuggestEnum.RISKY) {
                throw new BusinessException(BizMsgEnum.AUDIT_IMAGE_RISKY);
            } else {
                throw new BusinessException(BizMsgEnum.AUDIT_IMAGE_BLOCK);
            }
        }

        log.info("图片审核全部通过: userId={}, fileCount={}", userId, fileIds.size());
        return AuditSuggestEnum.PASS;
    }

    /**
     * 单张图片审核结果内部类
     */
    private static class ImageAuditResult {
        final String fileId;
        final AuditSuggestEnum suggest;
        final String label;

        ImageAuditResult(String fileId, AuditSuggestEnum suggest, String label) {
            this.fileId = fileId;
            this.suggest = suggest;
            this.label = label;
        }
    }

    /**
     * wx文本审核
     * 根据配置的审核版本动态选择接口逻辑：
     * v1模式：请求体不携带version参数，openid非必填，兼容测试占位openid
     * v2模式：请求体携带version=2参数，仅真实合法的openid才传入请求体
     *
     * @param content 待审核文本内容
     * @param openid   用户微信openid
     * @param scene    业务场景
     * @return 审核结果
     */
    private AuditResult doAuditText(String content, String openid, AuditSceneEnum scene) {
        String token = wxTokenUtil.getAccessToken();
        String url = String.format(MSG_SEC_CHECK_URL, token);

        boolean isV2 = wxConfigProperties.isAuditVersionV2();
        log.debug("文本审核版本: v{}", isV2 ? 2 : 1);

        Map<String, Object> body = new HashMap<>();
        body.put("content", content);

        // 根据版本决定是否添加openid和version参数
        if (isV2) {
            // v2模式：仅当openid真实有效时才传入，避免测试占位openid触发40003错误
            String validOpenid = validateAndGetValidOpenid(openid);
            if (validOpenid != null) {
                body.put("openid", validOpenid);
            }
            body.put("version", 2);
            body.put("scene", scene.getCode());
        } else {
            // v1模式：openid非必填，兼容测试占位openid
            if (openid != null && !openid.isEmpty()) {
                body.put("openid", openid);
            }
            body.put("scene", scene.getCode());
            // v1模式不添加version参数
        }

        String jsonBody = toJson(body);
        log.debug("发送给微信文本审核的请求体: {}", jsonBody);
        String response = doPostJson(url, jsonBody);
        JsonNode result = parseJson(response);

        int errCode = result.has("errcode") ? result.get("errcode").asInt() : 0;
        String errMsg = result.has("errmsg") ? result.get("errmsg").asText() : "";

        if (errCode != 0) {
            log.warn("微信文本审核接口返回错误: errcode={}, errmsg={}", errCode, errMsg);

            if (wxTokenUtil.isTokenExpired(errCode)) {
                log.info("access_token 过期，强制刷新后重试");
                token = wxTokenUtil.forceRefreshAccessToken();
                url = String.format(MSG_SEC_CHECK_URL, token);
                response = doPostJson(url, jsonBody);
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
                throw new BusinessException(BizMsgEnum.AUDIT_WECHAT_ERROR);
            }
        }

        // 兼容v1扁平结构与v2嵌套结构
        JsonNode resultNode = result.has("result") ? result.get("result") : result;
        String suggest = resultNode.has("suggest") ? resultNode.get("suggest").asText() : "";
        String label = resultNode.has("label") ? resultNode.get("label").asText() : null;

        AuditSuggestEnum auditResult = AuditSuggestEnum.fromCode(suggest);
        log.debug("文本审核结果: suggest={}, label={}", suggest, label);

        return new AuditResult(auditResult, label);
    }

    /**
     * wx图片审核
     * 根据配置的审核版本动态选择接口逻辑：
     * v1模式：使用旧版img_sec_check接口
     * v2模式：使用新版img_sec_check接口（通过multipart/form-data传递image参数）
     *
     * @param imageUrl 图片URL
     * @param tokenRef Token引用（支持刷新后更新）
     * @return 审核结果
     */
    private AuditResult doAuditImage(String imageUrl, AtomicReference<String> tokenRef) {
        String token = tokenRef.get();
        String url = String.format(IMG_SEC_CHECK_URL, token);

        boolean isV2 = wxConfigProperties.isAuditVersionV2();
        log.debug("图片审核版本: v{}", isV2 ? 2 : 1);

        byte[] imageBytes = downloadImage(imageUrl);
        
        if (imageBytes == null || imageBytes.length == 0) {
            log.error("图片下载为空: imageUrl={}", imageUrl);
            throw new RuntimeException("图片下载失败，内容为空");
        }
        
        String fileName = extractFileName(imageUrl);

        log.debug("开始图片审核: imageUrl={}, fileSize={}, fileName={}", imageUrl, imageBytes.length, fileName);

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
                throw new BusinessException(BizMsgEnum.AUDIT_WECHAT_ERROR);
            }
        }

        // 兼容v1扁平结构与v2嵌套结构
        JsonNode resultNode = result.has("result") ? result.get("result") : result;
        String suggest = resultNode.has("suggest") ? resultNode.get("suggest").asText() : "";
        String label = resultNode.has("label") ? resultNode.get("label").asText() : null;

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
        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
        
        StringBuilder sb = new StringBuilder();
        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"media\"; filename=\"").append(fileName).append("\"\r\n");
        sb.append("Content-Type: application/octet-stream\r\n\r\n");
        
        try {
            byte[] headerBytes = sb.toString().getBytes("UTF-8");
            byte[] footerBytes = ("\r\n--" + boundary + "--\r\n").getBytes("UTF-8");
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(headerBytes);
            baos.write(fileBytes);
            baos.write(footerBytes);
            byte[] bodyBytes = baos.toByteArray();
            
            URL urlObj = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            conn.setRequestProperty("Content-Length", String.valueOf(bodyBytes.length));
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            
            try (OutputStream os = conn.getOutputStream()) {
                os.write(bodyBytes);
                os.flush();
            }
            
            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"))) {
                    StringBuilder errorSb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorSb.append(line);
                    }
                    log.error("微信图片审核接口返回错误码: {}, 响应: {}", responseCode, errorSb.toString());
                }
                throw new WxNetworkException("微信图片审核接口返回错误码: " + responseCode);
            }
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                StringBuilder responseSb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    responseSb.append(line);
                }
                return responseSb.toString();
            }
            
        } catch (WxNetworkException e) {
            throw e;
        } catch (Exception e) {
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
            throw new BusinessException(BizMsgEnum.AUDIT_SCENE_EMPTY);
        }
        int code = scene.getCode();
        if (code < 1 || code > 5) {
            log.warn("审核场景不在微信官方支持范围内: scene={}, code={}", scene.getDescription(), code);
        }
    }

    /**
     * 验证openid的有效性，用于v2版本审核接口
     * v2版本强制要求openid为真实有效的微信用户标识，测试环境占位openid会触发40003错误
     *
     * 验证规则：
     * 1. openid为空或null → 返回null（不传入请求体）
     * 2. openid以"wx_test"开头 → 返回null（测试占位值，不传入请求体）
     * 3. openid格式不符合微信规范 → 返回null（不传入请求体）
     * 4. 其他情况 → 返回原始openid（传入请求体）
     *
     * @param openid 用户微信openid
     * @return 有效的openid，无效则返回null
     */
    private String validateAndGetValidOpenid(String openid) {
        if (openid == null || openid.isEmpty()) {
            log.debug("openid为空，v2模式下不传入请求体");
            return null;
        }

        if (openid.startsWith("wx_test")) {
            log.debug("openid为测试占位值({})，v2模式下不传入请求体", openid);
            return null;
        }

        if (!isValidWxOpenid(openid)) {
            log.debug("openid格式无效({})，v2模式下不传入请求体", openid);
            return null;
        }

        return openid;
    }

    /**
     * 判断openid是否符合微信官方格式规范
     * 微信openid通常为28位左右的字符串，由字母和数字组成
     *
     * @param openid 用户微信openid
     * @return true表示格式有效，false表示格式无效
     */
    private boolean isValidWxOpenid(String openid) {
        if (openid == null || openid.isEmpty()) {
            return false;
        }
        return openid.matches("^[a-zA-Z0-9_-]{20,50}$");
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
            throw new BusinessException(BizMsgEnum.AUDIT_RECORD_NOT_EXIST);
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
