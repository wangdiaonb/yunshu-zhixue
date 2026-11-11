package com.yunshu.zhixue.modules.route.entity;

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
 * 学习路线 {@code learning_route}。
 */
@Getter
@Setter
@TableName("learning_route")
public class LearningRoute {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private Long subjectId;

    private Integer status;

    private String icon;

    private String targetEducation;

    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleteFlag;
}
