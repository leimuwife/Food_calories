package com.nutrition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nutrition.common.BusinessException;
import com.nutrition.entity.UserFeedback;
import com.nutrition.enums.AuditSceneEnum;
import com.nutrition.enums.AuditSuggestEnum;
import com.nutrition.enums.BizMsgEnum;
import com.nutrition.enums.FeedbackStatusEnum;
import com.nutrition.mapper.UserFeedbackMapper;
import com.nutrition.service.ContentAuditService;
import com.nutrition.service.UserFeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户问题反馈服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserFeedbackServiceImpl extends ServiceImpl<UserFeedbackMapper, UserFeedback> implements UserFeedbackService {

    private final ContentAuditService contentAuditService;

    /**
     * 提交用户反馈（含内容审核）
     * <p>流程：先进行文本内容安全审核，审核通过后保存反馈记录</p>
     * @param userId 用户ID
     * @param content 反馈内容
     */
    @Override
    @Transactional
    public void submitFeedback(Long userId, String content) {
        log.info("用户{}提交反馈，内容长度={}", userId, content.length());

        AuditSuggestEnum auditResult = contentAuditService.auditText(userId, null, content, AuditSceneEnum.PROFILE);

        if (AuditSuggestEnum.BLOCK.equals(auditResult)) {
            log.warn("用户{}反馈内容审核未通过", userId);
            throw new BusinessException(BizMsgEnum.AUDIT_TEXT_BLOCKED);
        }

        UserFeedback feedback = new UserFeedback();
        feedback.setUserId(userId);
        feedback.setFeedbackContent(content);
        feedback.setFeedbackStatus(FeedbackStatusEnum.PENDING.getCode());
        feedback.setDeleteFlag(0);

        this.save(feedback);
        log.info("用户{}反馈提交成功，反馈ID={}", userId, feedback.getId());
    }

    /**
     * 查询用户反馈列表（分页）
     * <p>按创建时间倒序排列，只查询未删除的反馈记录</p>
     * @param userId 用户ID
     * @param page 分页参数
     * @return 分页反馈列表
     */
    @Override
    @Transactional(readOnly = true)
    public IPage<UserFeedback> getFeedbackList(Long userId, Page<UserFeedback> page) {
        log.info("查询用户{}反馈列表，页码={}，每页条数={}", userId, page.getCurrent(), page.getSize());

        LambdaQueryWrapper<UserFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFeedback::getUserId, userId)
               .eq(UserFeedback::getDeleteFlag, 0)
               .orderByDesc(UserFeedback::getCreateTime);

        IPage<UserFeedback> result = this.page(page, wrapper);
        log.info("查询完成，共{}条记录", result.getTotal());
        return result;
    }

    /**
     * 查询用户反馈列表（不分页）
     * <p>按创建时间倒序排列，只查询未删除的反馈记录</p>
     * @param userId 用户ID
     * @return 反馈列表
     */
    @Override
    @Transactional(readOnly = true)
    public List<UserFeedback> getFeedbackList(Long userId) {
        log.info("查询用户{}反馈列表（不分页）", userId);

        LambdaQueryWrapper<UserFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFeedback::getUserId, userId)
               .eq(UserFeedback::getDeleteFlag, 0)
               .orderByDesc(UserFeedback::getCreateTime);

        return this.list(wrapper);
    }
}