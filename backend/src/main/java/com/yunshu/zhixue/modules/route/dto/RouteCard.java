package com.yunshu.zhixue.modules.route.dto;

import java.util.List;

/**
 * 学习路线卡片（A-R01）。
 */
public record RouteCard(
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
        List<RoutePreviewCourse> previewCourses
) {
}
