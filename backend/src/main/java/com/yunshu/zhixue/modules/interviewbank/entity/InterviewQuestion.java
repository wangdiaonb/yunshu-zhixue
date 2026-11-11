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
 * 面试题库题目 {@code interview_question}。
 *
 * <p>主题以字符串留痕（词表外主题允许录入），不建外键；题干与参考答案入库后只读，
 * 仅主题与难度可维护。</p>
 */
@Getter
@Setter
@TableName("interview_question")
public class InterviewQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long subjectId;

    private String content;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String referenceAnswer;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String topic;

    private Integer difficulty;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String tags;

    /** 来源：1 知识库文档解析 / 2 手工录入 / 3 手动导入。 */
    private Integer sourceType;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String sourceDocName;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer sourceSeq;

    /** 状态：1 已上架 / 2 已下架。 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleteFlag;
}
