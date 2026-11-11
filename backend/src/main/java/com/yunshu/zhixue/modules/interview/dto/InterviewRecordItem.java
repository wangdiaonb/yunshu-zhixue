package com.yunshu.zhixue.modules.interview.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 面试记录列表项（管理端学员详情·面试记录，A-U07）。
 *
 * <p>字段与 {@code interview_record} 列别名一一对应，由 Mapper 直接映射，
 * {@code subjectName} 由 Service 通过学科模块补齐。</p>
 */
@Getter
@Setter
public class InterviewRecordItem {

    private Long id;

    private Long subjectId;

    private String subjectName;

    /** 1 简单 / 2 中等 / 3 困难 */
    private Integer difficulty;

    private Integer questionCount;

    /** 综合评分 0.0~10.0 */
    private Double overallScore;

    /** 累计时长（秒） */
    private Integer totalDuration;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;
}
