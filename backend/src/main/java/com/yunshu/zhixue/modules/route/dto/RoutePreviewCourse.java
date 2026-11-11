package com.yunshu.zhixue.modules.route.dto;

/**
 * 路线卡片课程预览（前 4 门）。
 */
public record RoutePreviewCourse(Long routeCourseId, Long courseId, String name,
                                 String bvid, String coverUrl, Integer durationMinutes,
                                 Integer sortOrder) {
}
