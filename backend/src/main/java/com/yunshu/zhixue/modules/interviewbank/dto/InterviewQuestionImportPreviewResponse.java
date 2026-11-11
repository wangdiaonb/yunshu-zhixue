package com.yunshu.zhixue.modules.interviewbank.dto;

import java.util.List;

/**
 * Excel 导入预览（A-I07 两段式①）。
 *
 * @param token                 预览令牌，确认导入时回传；无有效行时为空
 * @param total                 解析到的数据行数
 * @param validCount            校验通过行数
 * @param invalidCount          校验失败行数
 * @param subjects              涉及学科及行数
 * @param topicsNotInVocabulary 词表外主题（已入库但未维护进主题词表）
 * @param rows                  有效行预览（最多前 50 条）
 * @param errors                失败行明细
 */
public record InterviewQuestionImportPreviewResponse(
        String token,
        int total,
        int validCount,
        int invalidCount,
        List<InterviewQuestionImportSubjectStat> subjects,
        List<String> topicsNotInVocabulary,
        List<InterviewQuestionImportRow> rows,
        List<InterviewQuestionImportErrorRow> errors
) {
}
