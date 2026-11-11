package com.yunshu.zhixue.modules.student.service;

import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.student.dto.StudentBatchIdsRequest;
import com.yunshu.zhixue.modules.student.dto.StudentBatchStatusRequest;
import com.yunshu.zhixue.modules.student.dto.StudentDetail;
import com.yunshu.zhixue.modules.student.dto.StudentItem;
import com.yunshu.zhixue.modules.student.dto.StudentQuery;

/**
 * 管理端学员管理服务（A-U01 ~ A-U06）。
 *
 * <p>学习/面试/课程完成等跨模块统计经 study / interview / route 的只读服务获取，
 * 不直接访问对方表。</p>
 */
public interface StudentAdminService {

    PageResult<StudentItem> pageStudents(StudentQuery query);

    StudentDetail getStudentDetail(Long studentId);

    /** 启用（1）/ 禁用（0）；禁用后学员无法登录（登录校验读取 status）。 */
    void changeStatus(Long studentId, Integer status);

    /** @return 实际更新的学员数 */
    int batchChangeStatus(StudentBatchStatusRequest request);

    void deleteStudent(Long studentId);

    /** @return 实际删除的学员数 */
    int batchDelete(StudentBatchIdsRequest request);
}
