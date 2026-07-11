package com.nutrition.common;

import com.nutrition.enums.BizMsgEnum;
import lombok.Getter;

/**
 * 业务异常
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(BizMsgEnum msg) {
        super(msg.getMessage());
        this.code = msg.getCode();
    }
}
