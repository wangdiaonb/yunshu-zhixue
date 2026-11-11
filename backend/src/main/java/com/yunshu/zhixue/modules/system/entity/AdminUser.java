package com.yunshu.zhixue.modules.system.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 后台用户表 {@code admin_user}。
 */
@Getter
@Setter
@TableName("admin_user")
public class AdminUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** BCrypt 密码哈希 */
    private String passwordHash;

    private String mobile;

    private String email;

    /** 单角色 → role.id */
    private Long roleId;

    /** 1 启用 / 0 禁用 */
    private Integer status;

    /** 1 内置超级管理员（不可删除、不可改角色） */
    private Integer isBuiltin;

    private LocalDateTime lastLoginAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleteFlag;
}
