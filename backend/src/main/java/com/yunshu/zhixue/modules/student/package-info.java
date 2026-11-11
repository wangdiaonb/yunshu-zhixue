/**
 * 学员模块：学员账号与资料（个人中心）、后台学员管理（列表 / 详情 / 启停 / 删除）。
 *
 * <p>表归属：student / student_oauth。其他模块只能经本模块 Service 与 DTO 取数。</p>
 *
 * <p>学员管理的学习与面试统计经 study / interview / route 的只读服务聚合，
 * 本模块不直接访问 practice_record / interview_record / route_course_completion。
 * 不提供「导出学员」（需求 4.4 与原型均无入口，评审 R-02 裁决）。</p>
 */
package com.yunshu.zhixue.modules.student;
