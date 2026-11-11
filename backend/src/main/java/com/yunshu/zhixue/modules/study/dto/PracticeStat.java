package com.yunshu.zhixue.modules.study.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 学员刷题统计（跨模块只读 DTO）。
 *
 * @see com.yunshu.zhixue.modules.study.service.StudyStatService
 */
@Getter
@Setter
public class PracticeStat {

    private Long studentId;

    /** 刷题总数（practice_record 行数） */
    private long practiceCount;

    /** 答对题数 */
    private long correctCount;

    /** 学习天数：有刷题记录的日期数（Asia/Shanghai 自然日） */
    private long studyDays;

    public double accuracy() {
        if (practiceCount <= 0) {
            return 0.0;
        }
        return Math.round(correctCount * 1000.0 / practiceCount) / 10.0;
    }
}
