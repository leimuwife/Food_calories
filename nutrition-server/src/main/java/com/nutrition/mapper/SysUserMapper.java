package com.nutrition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nutrition.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 系统用户数据访问层
 * 负责系统用户表的数据库操作
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 分页查询所有用户，包含已禁用用户。
     *
     * @param offset 起始偏移
     * @param limit  每页条数
     * @return 用户列表
     */
    @Select("""
            SELECT id, openid, nickname, username, password_hash, password_encrypted,
                   file_ids, create_time, update_time, delete_flag
            FROM sys_user
            ORDER BY create_time DESC
            LIMIT #{offset}, #{limit}
            """)
    List<SysUser> selectUserPage(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 查询全部用户总数，包含已禁用用户。
     *
     * @return 用户总数
     */
    @Select("SELECT COUNT(*) FROM sys_user")
    long countAllUsers();

    /**
     * 查询任意状态用户，包含已禁用用户。
     *
     * @param id 用户ID
     * @return 用户实体
     */
    @Select("""
            SELECT id, openid, nickname, username, password_hash, password_encrypted,
                   file_ids, create_time, update_time, delete_flag
            FROM sys_user
            WHERE id = #{id}
            """)
    SysUser selectUserByIdIncludingDisabled(@Param("id") Long id);

    /**
     * 更新用户启用/禁用状态。
     *
     * @param id         用户ID
     * @param deleteFlag 0启用，1禁用
     * @return 更新行数
     */
    @Update("UPDATE sys_user SET delete_flag = #{deleteFlag}, update_time = NOW() WHERE id = #{id}")
    int updateUserStatus(@Param("id") Long id, @Param("deleteFlag") int deleteFlag);
}
