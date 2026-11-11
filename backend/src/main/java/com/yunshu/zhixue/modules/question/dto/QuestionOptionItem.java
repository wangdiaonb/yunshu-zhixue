package com.yunshu.zhixue.modules.question.dto;

/**
 * 题目选项响应项。
 */
public record QuestionOptionItem(Long id, String key, String text, boolean correct, Integer sortOrder) {
}
