package com.yunshu.zhixue.modules.interviewbank.dto;

import java.time.LocalDateTime;

/**
 * 面试题详情（A-I02）：在列表项基础上补参考答案与更新时间。
 */
public record InterviewQuestionDetail(
        Long id,
        Long subjectId,
        String subjectName,
        String content,
        String referenceAnswer,
        String topic,
        Integer difficulty,
        String tags,
        Integer sourceType,
        String sourceDocName,
        Integer sourceSeq,
        Integer status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
