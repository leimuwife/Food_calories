package com.nutrition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nutrition.entity.ContentAuditRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 内容审核记录 Mapper 接口
 * 提供审核记录的数据访问操作
 */
@Mapper
public interface ContentAuditRecordMapper extends BaseMapper<ContentAuditRecord> {

    /**
     * 查询待复审的审核记录
     *
     * @return 待复审记录列表
     */
    List<ContentAuditRecord> selectPendingReviewRecords();

    /**
     * 根据用户ID查询审核记录
     *
     * @param userId 用户ID
     * @return 审核记录列表
     */
    List<ContentAuditRecord> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据审核状态查询记录
     *
     * @param reviewStatus 复审状态
     * @return 审核记录列表
     */
    List<ContentAuditRecord> selectByReviewStatus(@Param("reviewStatus") Integer reviewStatus);
}
