package com.yunshu.zhixue.modules.course.dto;

import java.util.List;

/**
 * Excel 导入预览（A-C06）。
 *
 * @param token       确认导入令牌；全部无效时为 null
 * @param rows        有效行预览（最多 50 条）
 * @param errors      全部错误行
 */
public record CourseImportPreviewResponse(
        String token,
        int total,
        int validCount,
        int invalidCount,
        List<CourseImportRow> rows,
        List<CourseImportErrorRow> errors
) {
}
