package com.yunshu.zhixue.modules.route.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 学习路线列表筛选。
 */
@Getter
@Setter
public class RouteQuery {

    @Size(max = 100, message = "搜索关键词过长")
    private String keyword;

    @Min(value = 1, message = "学科参数不合法")
    private Long subjectId;

    @Min(value = 0, message = "状态参数不合法")
    @Max(value = 1, message = "状态参数不合法")
    private Integer status;
}
