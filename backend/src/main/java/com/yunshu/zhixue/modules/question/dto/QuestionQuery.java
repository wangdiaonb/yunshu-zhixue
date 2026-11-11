package com.yunshu.zhixue.modules.question.dto;

import com.yunshu.zhixue.common.response.PageQuery;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 题库列表筛选（A-Q02）。
 */
@Getter
@Setter
public class QuestionQuery extends PageQuery {

    @Size(max = 100, message = "搜索关键词过长")
    private String keyword;

    @Min(value = 1, message = "学科参数不合法")
    private Long subjectId;

    @Min(value = 1, message = "阶段参数不合法")
    private Long stageId;

    @Min(value = 1, message = "题型参数不合法")
    @Max(value = 4, message = "题型参数不合法")
    private Integer type;

    @Min(value = 1, message = "难度参数不合法")
    @Max(value = 3, message = "难度参数不合法")
    private Integer difficulty;

    @Min(value = 1, message = "状态参数不合法")
    @Max(value = 5, message = "状态参数不合法")
    private Integer status;
}
