package com.yunshu.zhixue.modules.question.support;

import lombok.Getter;
import lombok.Setter;

/**
 * 题目答题统计聚合行。
 */
@Getter
@Setter
public class QuestionPracticeStat {

    private Long questionId;

    private Long doneCount;

    private Long correctCount;
}
