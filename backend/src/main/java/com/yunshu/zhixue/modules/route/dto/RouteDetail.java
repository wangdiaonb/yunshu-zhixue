package com.yunshu.zhixue.modules.route.dto;

import java.util.List;

/**
 * 学习路线详情/预览（A-R06）。
 */
public record RouteDetail(
        Long id,
        String name,
        Long subjectId,
        String subjectName,
        String subjectIcon,
        String icon,
        String targetEducation,
        String description,
        Integer status,
        int courseCount,
        long totalDurationMinutes,
        List<RouteCourseItem> courses
) {
}
