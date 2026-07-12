package com.nutrition.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nutrition.entity.UserFeedback;

import java.util.List;

/**
 * 用户问题反馈服务接口
 */
public interface UserFeedbackService extends IService<UserFeedback> {

    /**
     * 提交用户反馈（含内容审核）
     * @param userId 用户ID
     * @param content 反馈内容
     */
    void submitFeedback(Long userId, String content);

    /**
     * 查询用户反馈列表（分页）
     * @param userId 用户ID
     * @param page 分页参数
     * @return 分页反馈列表
     */
    IPage<UserFeedback> getFeedbackList(Long userId, Page<UserFeedback> page);

    /**
     * 查询用户反馈列表（不分页）
     * @param userId 用户ID
     * @return 反馈列表
     */
    List<UserFeedback> getFeedbackList(Long userId);
}