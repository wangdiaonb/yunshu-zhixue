package com.yunshu.zhixue.modules.route.service;

import com.yunshu.zhixue.modules.route.dto.RouteCompletionStat;

import java.util.Collection;
import java.util.List;

/**
 * 路线学习进度只读服务：供学员管理、数据看板等模块调用。
 */
public interface RouteProgressService {

    /** 批量查询学员已学课程数。 */
    List<RouteCompletionStat> completedCountByStudents(Collection<Long> studentIds);
}
