package com.yunshu.zhixue.modules.system.service;

import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.system.dto.AdminAccount;
import com.yunshu.zhixue.modules.system.dto.AdminSummary;
import com.yunshu.zhixue.modules.system.dto.AdminUserCreateRequest;
import com.yunshu.zhixue.modules.system.dto.AdminUserDetail;
import com.yunshu.zhixue.modules.system.dto.AdminUserItem;
import com.yunshu.zhixue.modules.system.dto.AdminUserQuery;
import com.yunshu.zhixue.modules.system.dto.AdminUserUpdateRequest;
import com.yunshu.zhixue.modules.system.dto.BatchIdsRequest;
import com.yunshu.zhixue.modules.system.dto.BatchUserStatusRequest;

import java.util.List;

/**
 * 后台用户服务：auth 模块只能经本接口读取账号，不得直接使用 Mapper 或实体。
 */
public interface AdminUserService {

    /**
     * 按登录账号查询后台用户：先按用户名，未命中再按手机号（A-A01 / Q4）。
     *
     * @return 未命中返回 {@code null}
     */
    AdminAccount findAccountForLogin(String account);

    /** 按主键查询账号，供刷新令牌时校验账号状态。 */
    AdminAccount findAccountById(Long adminUserId);

    /** 登录返回所需的后台用户摘要（含角色编码与名称）。 */
    AdminSummary getSummary(Long adminUserId);

    /** 查询账号所属角色的操作级权限码集合。 */
    List<String> listPermissionCodes(Long adminUserId);

    /** 记录最后登录时间（UTC）。 */
    void recordLogin(Long adminUserId);

    /** 是否为超级管理员（角色编码 super_admin）。 */
    boolean isSuperAdmin(Long adminUserId);

    // ===== 用户管理（A-M01 ~ A-M08） =====

    PageResult<AdminUserItem> pageUsers(AdminUserQuery query);

    AdminUserDetail getUserDetail(Long adminUserId);

    Long createUser(AdminUserCreateRequest request);

    void updateUser(Long adminUserId, AdminUserUpdateRequest request);

    void deleteUser(Long adminUserId);

    void batchUpdateStatus(BatchUserStatusRequest request);

    void batchDelete(BatchIdsRequest request);

    /** 重置为部署默认密码，返回明文密码（仅一次）。 */
    String resetPassword(Long adminUserId);
}
