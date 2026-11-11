package com.yunshu.zhixue.modules.interview.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 学员面试次数统计（跨模块只读 DTO）。
 */
@Getter
@Setter
public class InterviewStat {

    private Long studentId;

    private long interviewCount;
}
