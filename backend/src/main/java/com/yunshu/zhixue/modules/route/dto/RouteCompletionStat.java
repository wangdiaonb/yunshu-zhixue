package com.yunshu.zhixue.modules.route.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 学员已学课程数统计（跨模块只读 DTO）。
 */
@Getter
@Setter
public class RouteCompletionStat {

    private Long studentId;

    /** 已勾选完成的路线课程数 */
    private long courseCount;
}
