package com.yunshu.zhixue.modules.course.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 课程列表项。
 */
public record CourseItem(
        Long id,
        String name,
        String coverUrl,
        Integer durationMinutes,
        Integer source,
        String link,
        String bvid,
        Long subjectId,
        String subjectName,
        List<Long> subjectIds,
        List<String> subjectNames,
        LocalDateTime createdAt
) {
}
