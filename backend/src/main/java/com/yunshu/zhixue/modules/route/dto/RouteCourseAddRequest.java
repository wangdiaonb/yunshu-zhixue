package com.yunshu.zhixue.modules.route.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 从课程库批量添加课程（A-R07）。
 */
public record RouteCourseAddRequest(

        @NotEmpty(message = "请选择要添加的课程")
        @Size(max = 200, message = "单次最多添加 200 门课程")
        List<Long> courseIds
) {
}
