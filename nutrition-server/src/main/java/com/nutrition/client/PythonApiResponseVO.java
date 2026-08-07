package com.nutrition.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Python AI服务通用响应封装
 * 统一处理 code、msg、data 结构
 */
@Data
@NoArgsConstructor
public class PythonApiResponseVO<T> {

    /** 响应码，200为成功 */
    @JsonProperty("code")
    private Integer code;

    /** 响应消息 */
    @JsonProperty("msg")
    private String msg;

    /** 响应数据 */
    @JsonProperty("data")
    private T data;

    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return code != null && code == SUCCESS_CODE;
    }

    /**
     * 响应码静态常量
     */
    public static final int SUCCESS_CODE = 200;
}
