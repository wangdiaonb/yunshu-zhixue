package com.yunshu.zhixue.modules.interviewbank.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 面试题主题词表 {@code interview_topic}。
 *
 * <p>{@code subjectId} 为空表示全局主题；词表仅用于筛选与提示，题目本身以主题名留痕。</p>
 */
@Getter
@Setter
@TableName("interview_topic")
public class InterviewTopic {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 允许改为 NULL（全局主题）。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long subjectId;

    private String name;

    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleteFlag;
}
