package com.yunshu.zhixue.modules.interviewbank.dto;

import com.yunshu.zhixue.common.response.PageQuery;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 面试题列表筛选（A-I01）。
 */
@Getter
@Setter
public class InterviewQuestionQuery extends PageQuery {

    @Size(max = 100, message = "搜索关键词不能超过 100 个字符")
    private String keyword;

    private Long subjectId;

    /** 主题词表主键；与 {@code unclassified} 互斥。 */
    private Long topicId;

    /** 主题名称；用于词表外主题（无词表主键）的精确筛选。 */
    @Size(max = 50, message = "主题不能超过 50 个字符")
    private String topic;

    @Min(value = 1, message = "难度不合法")
    @Max(value = 3, message = "难度不合法")
    private Integer difficulty;

    /** 只看未分类（主题为空）；与主题筛选互斥。 */
    private Boolean unclassified;

    @Min(value = 1, message = "状态不合法")
    @Max(value = 2, message = "状态不合法")
    private Integer status;
}
