package com.nutrition.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 参数校验异常 */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(Exception ex) {
        String message = "参数校验失败";
        if (ex instanceof MethodArgumentNotValidException validEx) {
            message = validEx.getBindingResult().getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.joining("; "));
        } else if (ex instanceof BindException bindEx) {
            message = bindEx.getBindingResult().getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.joining("; "));
        }
        log.error("参数校验异常: {}", message);
        return Result.badRequest(message);
    }

    /** 业务异常 */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException ex) {
        log.error("业务异常: code={}, message={}", ex.getCode(), ex.getMessage());
        return Result.fail(ex.getCode(), ex.getMessage());
    }

    /** 其他未知异常 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleUnknownException(Exception ex) {
        log.error("系统异常: ", ex);
        return Result.fail("系统繁忙，请稍后再试");
    }
}
