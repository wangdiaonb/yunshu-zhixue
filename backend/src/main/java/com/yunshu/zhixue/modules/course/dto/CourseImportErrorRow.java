package com.yunshu.zhixue.modules.course.dto;

import java.util.List;

/**
 * Excel 导入错误行。
 */
public record CourseImportErrorRow(int rowNumber, String name, List<String> messages) {
}
