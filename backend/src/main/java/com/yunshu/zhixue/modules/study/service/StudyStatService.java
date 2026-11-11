package com.yunshu.zhixue.modules.study.service;

import com.yunshu.zhixue.modules.study.dto.PracticeStat;

import java.util.Collection;
import java.util.List;

/**
 * 刷题统计只读服务：供学员管理、数据看板等模块调用（不跨模块访问 practice_record）。
 */
public interface StudyStatService {

    /**
     * 批量查询学员刷题统计。
     *
     * @param studentIds 学员主键集合，空集合返回空列表
     * @return 仅有刷题记录的学员条目，调用方按 studentId 兜底默认值
     */
    List<PracticeStat> statByStudents(Collection<Long> studentIds);
}
