package com.yunshu.zhixue.modules.question.dto;

import java.util.List;

/**
 * 题库导入错误行。
 */
public record QuestionImportErrorRow(int rowNumber, String content, List<String> messages) {
}
