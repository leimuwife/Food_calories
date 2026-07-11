package com.nutrition.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "发布动态请求参数")
public class FeedPublishParam {

    @NotBlank(message = "动态内容不能为空")
    @Schema(description = "动态文字内容")
    private String content;

    @NotEmpty(message = "图片列表不能为空")
    @Schema(description = "图片附件ID数组")
    private List<String> fileIds;
}
