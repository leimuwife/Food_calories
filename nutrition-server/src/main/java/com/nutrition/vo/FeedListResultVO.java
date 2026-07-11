package com.nutrition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 动态列表查询结果视图对象
 */
@Data
@Schema(description = "动态列表查询结果视图对象")
public class FeedListResultVO {

    @Schema(description = "动态列表")
    private List<FeedItemVO> list;

    @Schema(description = "总记录数")
    private Long total;

    @Schema(description = "是否有更多数据")
    private Boolean hasMore;
}