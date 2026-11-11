package com.yunshu.zhixue.modules.question.entity;

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
 * 题库题目 {@code question}。
 */
@Getter
@Setter
@TableName("question")
public class Question {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long stageId;

    private Integer type;

    private Integer difficulty;

    private String content;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String answer;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String analysis;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String tags;

    private Integer status;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String rejectReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleteFlag;
}
