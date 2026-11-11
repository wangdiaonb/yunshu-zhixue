package com.yunshu.zhixue.modules.interviewbank.dto;

/**
 * 主题词表项（A-I10）。
 *
 * @param id            词表主键；{@code null} 表示词表外主题（题目在用但未维护进词表）
 * @param name          主题名称
 * @param subjectId     所属学科；空表示全局主题或词表外主题
 * @param subjectName   学科名称
 * @param sortOrder     排序号
 * @param questionCount 该主题下题目数（可为 0）
 * @param inVocabulary  是否在主题词表内
 */
public record InterviewTopicItem(
        Long id,
        String name,
        Long subjectId,
        String subjectName,
        Integer sortOrder,
        long questionCount,
        boolean inVocabulary
) {
}
