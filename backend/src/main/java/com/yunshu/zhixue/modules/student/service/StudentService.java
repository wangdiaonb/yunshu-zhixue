package com.yunshu.zhixue.modules.student.service;

import com.yunshu.zhixue.modules.student.dto.StudentAccount;
import com.yunshu.zhixue.modules.student.dto.StudentSummary;

/**
 * 学员账号服务：auth 模块只能经本接口读写学员账号，不得直接使用 Mapper 或实体。
 */
public interface StudentService {

    /**
     * 按登录账号（手机号或邮箱）查询学员。
     *
     * <p>邮箱命中多条时按查询失败处理，避免账号枚举（P-A03）。</p>
     *
     * @return 未命中返回 {@code null}
     */
    StudentAccount findAccountForLogin(String account);

    /** 按主键查询账号，供刷新令牌时校验账号状态。 */
    StudentAccount findAccountById(Long studentId);

    /** 按手机号查询账号（验证码登录 / 找回密码使用）。 */
    StudentAccount findAccountByMobile(String mobile);

    /** 手机号是否已注册（注册与找回密码前置校验）。 */
    boolean existsByMobile(String mobile);

    /** 创建学员账号，返回新账号（id 已回填）。 */
    StudentAccount createStudent(String mobile, String passwordHash, String nickname);

    /** 更新密码哈希（找回密码使用）。 */
    void updatePasswordHash(Long studentId, String passwordHash);

    /** 登录返回所需的学员摘要。 */
    StudentSummary getSummary(Long studentId);

    /** 记录最后登录时间（UTC）；账号不存在时静默忽略。 */
    void recordLogin(Long studentId);
}
