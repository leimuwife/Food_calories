package com.nutrition.param;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 个人信息更新参数类
 * 用于接收用户个人信息更新的请求参数
 * 包含昵称、头像审核及问题反馈功能
 */
@Data
public class ProfileUpdateParam {

    @Size(max = 32, message = "昵称最长32位")
    private String nickname;

    /** 头像附件ID */
    private String fileIds;

    /** 问题反馈内容 */
    @Size(max = 2000, message = "反馈内容最长2000字")
    private String feedbackContent;
}
