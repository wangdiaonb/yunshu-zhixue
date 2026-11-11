package com.yunshu.zhixue.modules.interviewbank.support;

/**
 * Excel 解析出的原始行（未校验）。
 *
 * @param rowNumber       Excel 中的行号（从 1 起，含表头）
 * @param subjectName     所属学科名称（Excel 未填时为空）
 * @param content         题干
 * @param difficulty      难度文本（简单 / 中等 / 困难 / 1~3）
 * @param topic           主题
 * @param tags            标签（逗号分隔）
 * @param referenceAnswer 参考答案
 */
public record RawInterviewQuestionRow(
        int rowNumber,
        String subjectName,
        String content,
        String difficulty,
        String topic,
        String tags,
        String referenceAnswer) {
}
