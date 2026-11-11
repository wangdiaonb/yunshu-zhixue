package com.yunshu.zhixue.modules.course.dto;

import com.yunshu.zhixue.common.response.PageQuery;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 课程列表筛选参数（A-C02）。
 */
@Getter
@Setter
public class CourseQuery extends PageQuery {

    @Size(max = 100, message = "搜索关键词过长")
    private String keyword;

    /** 0 = 主学科为空（其他学科）；不传 = 全部。 */
    @Min(value = 0, message = "学科参数不合法")
    private Long subjectId;

    @Min(value = 1, message = "来源参数不合法")
    @Max(value = 2, message = "来源参数不合法")
    private Integer source;
}
