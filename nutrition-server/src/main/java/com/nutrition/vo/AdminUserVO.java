package com.nutrition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端用户视图对象。
 * 普通用户不可访问该对象对应的接口。
 */
@Data
@Builder
@Schema(description = "管理端用户信息")
public class AdminUserVO {

    /** 用户ID */
    private Long id;

    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 明文密码；旧用户无加密值时不可查看 */
    private String password;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 修改时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除/禁用状态：0启用，1禁用 */
    private Integer deleteFlag;

    /** 是否启用 */
    private Boolean enabled;
}
