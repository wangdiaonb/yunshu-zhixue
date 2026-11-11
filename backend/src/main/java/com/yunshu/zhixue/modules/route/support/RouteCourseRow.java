package com.yunshu.zhixue.modules.route.support;

import lombok.Getter;
import lombok.Setter;

/**
 * 路线课程联表查询行。
 */
@Getter
@Setter
public class RouteCourseRow {

    private Long routeCourseId;

    private Long courseId;

    private String name;

    private String bvid;

    private String coverUrl;

    private Integer durationMinutes;

    private Integer source;

    private Integer sortOrder;

    private Long subjectId;

    /** 1 = 属于路线本学科（主学科或额外学科），0 = 跨学科补充。 */
    private Integer localSubject;
}
