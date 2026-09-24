package com.nutrition.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理端修改用户启停状态参数。
 */
@Data
@Schema(description = "用户启停状态参数")
public class AdminUserStatusParam {

    /** true启用，false禁用 */
    @NotNull(message = "用户状态不能为空")
    private Boolean enabled;
}
