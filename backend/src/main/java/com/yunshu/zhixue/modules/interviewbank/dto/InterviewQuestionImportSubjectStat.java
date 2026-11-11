package com.yunshu.zhixue.modules.interviewbank.dto;

/**
 * 本次导入涉及到的学科及行数，供预览确认。
 */
public record InterviewQuestionImportSubjectStat(
        Long subjectId,
        String subjectName,
        int count
) {
}
