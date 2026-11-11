package com.yunshu.zhixue.modules.interview.service;

import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.interview.dto.InterviewRecordItem;
import com.yunshu.zhixue.modules.interview.dto.InterviewStat;

import java.util.Collection;
import java.util.List;

/**
 * 面试记录只读服务：供学员管理、数据看板等模块调用（不跨模块访问 interview_record）。
 */
public interface InterviewRecordService {

    /** 批量查询学员面试次数。 */
    List<InterviewStat> countByStudents(Collection<Long> studentIds);

    /** 学员面试记录分页（A-U07）。 */
    PageResult<InterviewRecordItem> pageByStudent(Long studentId, long page, long pageSize);
}
