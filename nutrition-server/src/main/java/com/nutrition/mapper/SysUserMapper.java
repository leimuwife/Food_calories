package com.nutrition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nutrition.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统用户数据访问层
 * 负责系统用户表的数据库操作
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
