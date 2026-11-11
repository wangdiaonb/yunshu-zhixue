package com.yunshu.zhixue.modules.route.dto;

/**
 * 从课程库添加课程的候选项。
 */
public record RouteCourseOption(
        Long courseId,
        String name,
        String bvid,
        String coverUrl,
        Integer durationMinutes,
        Integer source,
        Long subjectId,
        String subjectName,
        boolean localSubject
) {
}
