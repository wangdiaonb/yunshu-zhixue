package com.yunshu.zhixue.modules.course.dto;

import java.math.BigDecimal;

/**
 * 课程库统计卡（A-C01）。
 */
public record CourseStatsResponse(long totalCourses, BigDecimal totalHours, long relatedSubjectCount) {
}
