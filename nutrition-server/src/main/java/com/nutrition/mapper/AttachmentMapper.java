package com.nutrition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nutrition.entity.Attachment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 附件数据访问层
 * 负责附件表的数据库操作
 */
@Mapper
public interface AttachmentMapper extends BaseMapper<Attachment> {
}