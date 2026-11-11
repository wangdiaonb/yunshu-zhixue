package com.yunshu.zhixue.modules.interviewbank.dto;

import java.time.LocalDateTime;

/**
 * 面试题列表项（A-I01）。
 */
public record InterviewQuestionItem(
        Long id,
        Long subjectId,
        String subjectName,
        String content,
        String topic,
        Integer difficulty,
        String tags,
        Integer sourceType,
        String sourceDocName,
        Integer sourceSeq,
        Integer status,
        LocalDateTime createdAt
) {
}
