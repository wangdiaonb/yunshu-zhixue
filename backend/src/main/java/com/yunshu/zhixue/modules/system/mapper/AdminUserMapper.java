package com.yunshu.zhixue.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshu.zhixue.modules.system.entity.AdminUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 后台用户 Mapper（仅 system 模块可访问）。
 */
@Mapper
public interface AdminUserMapper extends BaseMapper<AdminUser> {

    /**
     * 查询角色拥有的操作级权限码（type=2），供登录返回与权限注解校验使用。
     */
    @Select("""
            SELECT DISTINCT p.code
            FROM permission p
            INNER JOIN role_permission rp ON rp.permission_id = p.id
            WHERE rp.role_id = #{roleId}
              AND p.type = 2
              AND p.delete_flag = 0
            ORDER BY p.code
            """)
    List<String> selectPermissionCodesByRoleId(@Param("roleId") Long roleId);
}
