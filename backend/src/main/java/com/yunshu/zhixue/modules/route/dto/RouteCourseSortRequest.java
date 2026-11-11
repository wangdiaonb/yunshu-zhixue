package com.yunshu.zhixue.modules.route.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 路线课程排序（A-R09）；orderedIds 为 route_course.id。
 */
public record RouteCourseSortRequest(

        @NotEmpty(message = "排序列表不能为空")
        @Size(max = 200, message = "排序列表不合法")
        List<Long> orderedIds
) {
}
