package com.yunshu.zhixue.modules.student.entity;

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
 * 学员表 {@code student}。
 */
@Getter
@Setter
@TableName("student")
public class Student {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 手机号（11 位，登录账号） */
    private String mobile;

    /** BCrypt 密码哈希 */
    private String passwordHash;

    private String nickname;

    private String avatar;

    private String email;

    /** 0 保密 / 1 男 / 2 女 */
    private Integer gender;

    private Long preferredSubjectId;

    private Integer guideFinished;

    /** 1 已启用 / 0 已禁用 */
    private Integer status;

    private LocalDateTime lastLoginAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleteFlag;
}
