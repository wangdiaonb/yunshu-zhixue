package com.yunshu.zhixue.modules.course.support;

/**
 * Excel 原始行（未做业务校验）。
 */
public record RawCourseRow(
        int rowNumber,
        String name,
        String subjects,
        String source,
        String duration,
        String bvid,
        String coverUrl,
        String link
) {
}
