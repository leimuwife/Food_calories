package com.nutrition.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页结果VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageVO<T> {

    /** 数据列表 */
    private List<T> records;

    /** 总记录数 */
    private long total;

    /** 当前页码 */
    private int pageNum;

    /** 每页大小 */
    private int pageSize;

    public PageVO(List<T> records, long total) {
        this.records = records;
        this.total = total;
    }
}
