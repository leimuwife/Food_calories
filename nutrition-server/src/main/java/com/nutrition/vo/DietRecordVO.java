package com.nutrition.vo;

import lombok.Data;
import java.util.List;

/**
 * 饮食记录视图对象
 * 用于返回用户饮食记录的详细信息
 */
@Data
public class DietRecordVO {

    private Long id;

    private String recordDate;

    private String mealType;

    private String remark;

    private List<DietItemVO> items;
}
