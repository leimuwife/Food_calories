package com.nutrition.param;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户问题反馈参数类
 * 用于接收用户反馈的请求参数
 */
@Data
public class FeedbackParam {

    @NotBlank(message = "反馈内容不能为空")
    @Size(max = 2000, message = "反馈内容最长2000字")
    private String content;
}