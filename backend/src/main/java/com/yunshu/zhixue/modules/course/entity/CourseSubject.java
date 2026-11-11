package com.yunshu.zhixue.modules.course.entity;

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
 * 课程-额外学科关联 {@code course_subject}。
 *
 * <p>唯一键不含 delete_flag，更新关联时使用物理删除，避免逻辑删除后无法重新关联。</p>
 */
@Getter
@Setter
@TableName("course_subject")
public class CourseSubject {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;

    private Long subjectId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleteFlag;
}
