package com.yunshu.zhixue.modules.route.dto;

/**
 * 路线详情课程项。
 */
public record RouteCourseItem(
        Long routeCourseId,
        Long courseId,
        String name,
        String bvid,
        String coverUrl,
        Integer durationMinutes,
        Integer source,
        Integer sortOrder,
        Long subjectId,
        String subjectName,
        boolean localSubject
) {
}
