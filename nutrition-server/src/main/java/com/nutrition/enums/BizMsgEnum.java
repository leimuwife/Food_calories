package com.nutrition.enums;

import lombok.Getter;

/**
 * 业务提示消息枚举
 * 按业务模块分组管理所有对外展示的中文提示文案
 * 禁止在业务代码中直接写中文字符串，必须引用此枚举
 */
@Getter
public enum BizMsgEnum {

    // ========== 内容审核模块 (AUDIT) ==========
    AUDIT_CONTENT_BLOCK(400, "内容包含违规信息，请修改后重新提交"),
    AUDIT_CONTENT_RISKY(400, "内容需要人工审核，请稍后重试"),
    AUDIT_IMAGE_BLOCK(400, "图片包含违规内容，请更换图片后重新提交"),
    AUDIT_IMAGE_RISKY(400, "图片需要人工审核，请稍后重试"),
    AUDIT_ATTACHMENT_NOT_EXIST(400, "附件不存在"),
    AUDIT_IMAGE_URL_INVALID(400, "图片地址格式不正确，请使用公网HTTPS地址"),
    AUDIT_ATTACHMENT_ID_FORMAT_ERROR(400, "附件ID格式错误"),
    AUDIT_SCENE_EMPTY(400, "审核场景不能为空"),
    AUDIT_RECORD_NOT_EXIST(404, "审核记录不存在"),
    AUDIT_SERVICE_UNAVAILABLE(500, "内容审核服务暂时不可用，请稍后重试"),
    AUDIT_IMAGE_SERVICE_UNAVAILABLE(500, "图片审核服务暂时不可用，请稍后重试"),
    AUDIT_WECHAT_ERROR(500, "微信审核接口调用失败"),
    AUDIT_IMAGE_DOWNLOAD_FAILED(500, "图片下载失败，请稍后重试"),
    AUDIT_TEXT_BLOCKED(400, "文本内容审核未通过，请修改后重新提交"),

    // ========== 动态模块 (FEED) ==========
    FEED_NOT_BIND_WECHAT(400, "请先绑定微信账号"),
    FEED_PUBLISH_FAILED(500, "发布失败，请重试"),

    // ========== 饮食记录模块 (DIET) ==========
    DIET_USER_ID_INVALID(400, "用户ID无效"),
    DIET_DATE_INVALID(400, "记录日期不能大于今天"),
    DIET_QUERY_DATE_EMPTY(400, "查询日期不能为空"),
    DIET_QUERY_DATE_INVALID(400, "查询日期不能大于今天"),
    DIET_QUERY_DATE_OUT_OF_RANGE(400, "查询日期不能超过90天前"),
    DIET_ITEM_ID_EMPTY(400, "饮食项ID不能为空"),
    DIET_DATE_FORMAT_ERROR(400, "日期格式错误，应为YYYY-MM-DD"),
    DIET_MEAL_TYPE_INVALID(400, "无效的餐次类型"),
    DIET_RECORD_NOT_EXIST(404, "饮食记录不存在"),
    DIET_ITEM_NOT_EXIST(404, "饮食项不存在"),
    DIET_NO_PERMISSION_DELETE(403, "无权删除该记录"),
    DIET_NO_PERMISSION_VIEW(403, "无权访问该饮食项"),
    DIET_NO_PERMISSION_UPDATE(403, "无权修改该饮食项"),

    // ========== 用户模块 (USER) ==========
    USER_LOGIN_FAILED(400, "用户名或密码错误"),
    USER_NAME_EXIST(400, "用户名已存在"),
    USER_NOT_EXIST(404, "用户不存在"),
    USER_NOT_LOGIN(401, "请先登录"),

    // ========== 打卡模块 (CHECKIN) ==========
    CHECKIN_RECORD_NOT_EXIST(404, "打卡记录不存在"),


    // ========== 管理员模块 (ADMIN) ==========
    ADMIN_NOT_EXIST(400, "账号不存在"),
    ADMIN_DISABLED(400, "账号已被禁用，请联系超级管理员"),
    ADMIN_PASSWORD_ERROR(400, "密码错误"),

    // ========== 通用错误 ==========
    SYSTEM_ERROR(500, "系统繁忙，请稍后再试");

    private final int code;
    private final String message;

    BizMsgEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
