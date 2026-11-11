package com.yunshu.zhixue.modules.course.dto;

import java.util.List;

/**
 * Excel 导入的合法课程行；同时用于 Redis 预览暂存。
 */
public record CourseImportRow(
        int rowNumber,
        String name,
        Long subjectId,
        List<Long> subjectIds,
        List<String> subjectNames,
        int durationMinutes,
        int source,
        String bvid,
        String link,
        String coverUrl,
        List<String> warnings
) {
}
