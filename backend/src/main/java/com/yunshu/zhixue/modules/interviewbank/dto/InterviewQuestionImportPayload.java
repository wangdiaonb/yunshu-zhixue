package com.yunshu.zhixue.modules.interviewbank.dto;

import java.util.List;

/**
 * 预览结果暂存内容（Redis，TTL 30min）。
 *
 * @param sourceDocName 源 Excel 文件名（文件不入库，仅留名）
 * @param rows          校验通过的行
 */
public record InterviewQuestionImportPayload(
        String sourceDocName,
        List<InterviewQuestionImportRow> rows
) {
}
