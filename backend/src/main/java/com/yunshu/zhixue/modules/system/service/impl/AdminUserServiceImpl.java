package com.yunshu.zhixue.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.common.util.TimeUtils;
import com.yunshu.zhixue.modules.system.dto.AdminAccount;
import com.yunshu.zhixue.modules.system.dto.AdminSummary;
import com.yunshu.zhixue.modules.system.dto.AdminUserCreateRequest;
import com.yunshu.zhixue.modules.system.dto.AdminUserDetail;
import com.yunshu.zhixue.modules.system.dto.AdminUserItem;
import com.yunshu.zhixue.modules.system.dto.AdminUserQuery;
import com.yunshu.zhixue.modules.system.dto.AdminUserUpdateRequest;
import com.yunshu.zhixue.modules.system.dto.BatchIdsRequest;
import com.yunshu.zhixue.modules.system.dto.BatchUserStatusRequest;
import com.yunshu.zhixue.modules.system.dto.OperationLogItem;
import com.yunshu.zhixue.modules.system.entity.AdminUser;
import com.yunshu.zhixue.modules.system.entity.OperationLog;
import com.yunshu.zhixue.modules.system.entity.Role;
import com.yunshu.zhixue.modules.system.mapper.AdminUserMapper;
import com.yunshu.zhixue.modules.system.mapper.OperationLogMapper;
import com.yunshu.zhixue.modules.system.mapper.RoleMapper;
import com.yunshu.zhixue.modules.system.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private static final String SUPER_ADMIN_CODE = "super_admin";

    private final AdminUserMapper adminUserMapper;
    private final RoleMapper roleMapper;
    private final OperationLogMapper operationLogMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.default-password:}")
    private String defaultPassword;

    // ===== 认证模块使用 =====

    @Override
    public AdminAccount findAccountForLogin(String account) {
        if (!StringUtils.hasText(account)) {
            return null;
        }
        String value = account.trim();

        AdminUser adminUser = adminUserMapper.selectOne(Wrappers.<AdminUser>lambdaQuery()
                .eq(AdminUser::getUsername, value)
                .last("LIMIT 1"));
        if (adminUser == null) {
            adminUser = adminUserMapper.selectOne(Wrappers.<AdminUser>lambdaQuery()
                    .eq(AdminUser::getMobile, value)
                    .last("LIMIT 1"));
        }
        return toAccount(adminUser);
    }

    @Override
    public AdminAccount findAccountById(Long adminUserId) {
        if (adminUserId == null) {
            return null;
        }
        return toAccount(adminUserMapper.selectById(adminUserId));
    }

    @Override
    public AdminSummary getSummary(Long adminUserId) {
        AdminUser adminUser = adminUserMapper.selectById(adminUserId);
        if (adminUser == null) {
            return null;
        }
        Role role = adminUser.getRoleId() == null ? null : roleMapper.selectById(adminUser.getRoleId());
        return new AdminSummary(
                adminUser.getId(),
                adminUser.getUsername(),
                adminUser.getRoleId(),
                role == null ? null : role.getCode(),
                role == null ? null : role.getName());
    }

    @Override
    public List<String> listPermissionCodes(Long adminUserId) {
        AdminUser adminUser = adminUserMapper.selectById(adminUserId);
        if (adminUser == null || adminUser.getRoleId() == null) {
            return List.of();
        }
        return adminUserMapper.selectPermissionCodesByRoleId(adminUser.getRoleId());
    }

    @Override
    public void recordLogin(Long adminUserId) {
        adminUserMapper.update(null, Wrappers.<AdminUser>lambdaUpdate()
                .eq(AdminUser::getId, adminUserId)
                .set(AdminUser::getLastLoginAt, LocalDateTime.now(ZoneOffset.UTC)));
    }

    @Override
    public boolean isSuperAdmin(Long adminUserId) {
        AdminSummary summary = adminUserId == null ? null : getSummary(adminUserId);
        return summary != null && SUPER_ADMIN_CODE.equals(summary.roleCode());
    }

    // ===== 用户管理 =====

    @Override
    public PageResult<AdminUserItem> pageUsers(AdminUserQuery query) {
        LambdaQueryWrapper<AdminUser> wrapper = Wrappers.<AdminUser>lambdaQuery();
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(item -> item.like(AdminUser::getUsername, keyword)
                    .or()
                    .like(AdminUser::getMobile, keyword));
        }
        if (query.getRoleId() != null) {
            wrapper.eq(AdminUser::getRoleId, query.getRoleId());
        }
        if (query.getStatus() != null) {
            wrapper.eq(AdminUser::getStatus, query.getStatus());
        }
        wrapper.orderByAsc(AdminUser::getId);

        IPage<AdminUser> page = adminUserMapper.selectPage(
                new Page<>(query.pageOrDefault(), query.pageSizeOrDefault()), wrapper);

        Map<Long, Role> roleMap = loadRoles(page.getRecords());
        List<AdminUserItem> items = page.getRecords().stream()
                .map(user -> toItem(user, roleMap.get(user.getRoleId())))
                .toList();
        return PageResult.of(items, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public AdminUserDetail getUserDetail(Long adminUserId) {
        AdminUser adminUser = requireUser(adminUserId);
        Role role = adminUser.getRoleId() == null ? null : roleMapper.selectById(adminUser.getRoleId());
        List<OperationLog> logs = operationLogMapper.selectList(Wrappers.<OperationLog>lambdaQuery()
                .eq(OperationLog::getUserId, adminUserId)
                .orderByDesc(OperationLog::getCreatedAt)
                .orderByDesc(OperationLog::getId)
                .last("LIMIT 10"));
        List<OperationLogItem> recentLogs = logs.stream().map(this::toLogItem).toList();
        return new AdminUserDetail(toItem(adminUser, role), recentLogs);
    }

    @Override
    public Long createUser(AdminUserCreateRequest request) {
        Role role = requireAssignableRole(request.roleId());
        String username = request.username().trim();
        String mobile = request.mobile().trim();
        ensureUsernameAvailable(username, null);
        ensureMobileAvailable(mobile, null);

        AdminUser adminUser = new AdminUser();
        adminUser.setUsername(username);
        adminUser.setMobile(mobile);
        adminUser.setEmail(normalizeText(request.email()));
        adminUser.setRoleId(role.getId());
        adminUser.setPasswordHash(passwordEncoder.encode(request.password()));
        adminUser.setStatus(request.status());
        adminUser.setIsBuiltin(0);
        try {
            adminUserMapper.insert(adminUser);
        } catch (DuplicateKeyException ex) {
            throw duplicateKey(ex);
        }
        return adminUser.getId();
    }

    @Override
    public void updateUser(Long adminUserId, AdminUserUpdateRequest request) {
        AdminUser existing = requireUser(adminUserId);
        boolean builtin = isBuiltin(existing);

        if (builtin) {
            if (!Objects.equals(request.roleId(), existing.getRoleId())) {
                throw BizException.of(ErrorCode.BUILTIN_IMMUTABLE, "内置超级管理员不可修改角色");
            }
            if (request.status() != null && request.status() == 0) {
                throw BizException.of(ErrorCode.BUILTIN_IMMUTABLE, "内置超级管理员不可禁用");
            }
        } else {
            requireAssignableRole(request.roleId());
        }

        String username = request.username().trim();
        String mobile = normalizeText(request.mobile());
        if (!builtin && mobile == null) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "请输入手机号");
        }
        ensureUsernameAvailable(username, adminUserId);
        if (mobile != null) {
            ensureMobileAvailable(mobile, adminUserId);
        }

        LambdaUpdateWrapper<AdminUser> update = Wrappers.<AdminUser>lambdaUpdate()
                .eq(AdminUser::getId, adminUserId)
                .set(AdminUser::getUsername, username)
                .set(AdminUser::getMobile, mobile)
                .set(AdminUser::getEmail, normalizeText(request.email()))
                .set(AdminUser::getRoleId, request.roleId())
                .set(AdminUser::getStatus, builtin ? existing.getStatus() : request.status());
        if (StringUtils.hasText(request.password())) {
            String password = request.password().trim();
            if (password.length() < 6 || password.length() > 20) {
                throw BizException.of(ErrorCode.PARAM_INVALID, "密码长度为 6~20 位");
            }
            update.set(AdminUser::getPasswordHash, passwordEncoder.encode(password));
        }
        try {
            adminUserMapper.update(null, update);
        } catch (DuplicateKeyException ex) {
            throw duplicateKey(ex);
        }
    }

    @Override
    public void deleteUser(Long adminUserId) {
        AdminUser adminUser = requireUser(adminUserId);
        if (isBuiltin(adminUser)) {
            throw BizException.of(ErrorCode.BUILTIN_IMMUTABLE, "内置超级管理员不可删除");
        }
        adminUserMapper.deleteById(adminUserId);
    }

    @Override
    public void batchUpdateStatus(BatchUserStatusRequest request) {
        List<AdminUser> users = loadUsersByIds(request.ids());
        if (request.status() == 0 && users.stream().anyMatch(this::isBuiltin)) {
            throw BizException.of(ErrorCode.BUILTIN_IMMUTABLE, "内置超级管理员不可禁用");
        }
        adminUserMapper.update(null, Wrappers.<AdminUser>lambdaUpdate()
                .in(AdminUser::getId, request.ids())
                .set(AdminUser::getStatus, request.status()));
    }

    @Override
    public void batchDelete(BatchIdsRequest request) {
        List<AdminUser> users = loadUsersByIds(request.ids());
        if (users.stream().anyMatch(this::isBuiltin)) {
            throw BizException.of(ErrorCode.BUILTIN_IMMUTABLE, "内置超级管理员不可删除");
        }
        adminUserMapper.deleteBatchIds(request.ids());
    }

    @Override
    public String resetPassword(Long adminUserId) {
        requireUser(adminUserId);
        if (!StringUtils.hasText(defaultPassword)) {
            throw BizException.of(ErrorCode.INTERNAL_ERROR, "默认密码未配置");
        }
        adminUserMapper.update(null, Wrappers.<AdminUser>lambdaUpdate()
                .eq(AdminUser::getId, adminUserId)
                .set(AdminUser::getPasswordHash, passwordEncoder.encode(defaultPassword)));
        return defaultPassword;
    }

    // ===== 内部方法 =====

    private AdminUser requireUser(Long adminUserId) {
        AdminUser adminUser = adminUserMapper.selectById(adminUserId);
        if (adminUser == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return adminUser;
    }

    private List<AdminUser> loadUsersByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "请选择用户");
        }
        List<AdminUser> users = adminUserMapper.selectBatchIds(new HashSet<>(ids));
        if (users.size() != new HashSet<>(ids).size()) {
            throw BizException.of(ErrorCode.NOT_FOUND, "部分用户不存在");
        }
        return users;
    }

    private Role requireAssignableRole(Long roleId) {
        Role role = roleId == null ? null : roleMapper.selectById(roleId);
        if (role == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "角色不存在");
        }
        if (SUPER_ADMIN_CODE.equals(role.getCode())) {
            throw BizException.of(ErrorCode.BUILTIN_IMMUTABLE, "不允许创建或切换为超级管理员");
        }
        return role;
    }

    private void ensureUsernameAvailable(String username, Long excludeId) {
        Long count = adminUserMapper.selectCount(Wrappers.<AdminUser>lambdaQuery()
                .eq(AdminUser::getUsername, username)
                .ne(excludeId != null, AdminUser::getId, excludeId));
        if (count != null && count > 0) {
            throw BizException.of(ErrorCode.DUPLICATE_NAME, "用户名已存在");
        }
    }

    private void ensureMobileAvailable(String mobile, Long excludeId) {
        Long count = adminUserMapper.selectCount(Wrappers.<AdminUser>lambdaQuery()
                .eq(AdminUser::getMobile, mobile)
                .ne(excludeId != null, AdminUser::getId, excludeId));
        if (count != null && count > 0) {
            throw BizException.of(ErrorCode.MOBILE_EXISTS);
        }
    }

    private BizException duplicateKey(DuplicateKeyException ex) {
        String message = ex.getMessage() == null ? "" : ex.getMessage();
        if (message.contains("uk_admin_user_mobile")) {
            return BizException.of(ErrorCode.MOBILE_EXISTS);
        }
        return BizException.of(ErrorCode.DUPLICATE_NAME, "用户名已存在");
    }

    private Map<Long, Role> loadRoles(List<AdminUser> users) {
        Set<Long> roleIds = users.stream()
                .map(AdminUser::getRoleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (roleIds.isEmpty()) {
            return Map.of();
        }
        return roleMapper.selectBatchIds(roleIds).stream()
                .collect(Collectors.toMap(Role::getId, Function.identity()));
    }

    private AdminUserItem toItem(AdminUser adminUser, Role role) {
        return new AdminUserItem(
                adminUser.getId(),
                adminUser.getUsername(),
                adminUser.getMobile(),
                adminUser.getEmail(),
                adminUser.getRoleId(),
                role == null ? null : role.getName(),
                role == null ? null : role.getCode(),
                adminUser.getStatus(),
                adminUser.getIsBuiltin(),
                TimeUtils.utcToBiz(adminUser.getLastLoginAt()),
                TimeUtils.utcToBiz(adminUser.getCreatedAt()));
    }

    private OperationLogItem toLogItem(OperationLog log) {
        return new OperationLogItem(log.getId(), log.getUserId(), log.getUsername(),
                log.getAction(), log.getTargetType(), log.getTargetName(), log.getIp(),
                TimeUtils.utcToBiz(log.getCreatedAt()));
    }

    private AdminAccount toAccount(AdminUser adminUser) {
        if (adminUser == null) {
            return null;
        }
        return new AdminAccount(
                adminUser.getId(),
                adminUser.getUsername(),
                adminUser.getPasswordHash(),
                adminUser.getRoleId(),
                adminUser.getStatus() == null ? 0 : adminUser.getStatus());
    }

    private boolean isBuiltin(AdminUser adminUser) {
        return adminUser.getIsBuiltin() != null && adminUser.getIsBuiltin() == 1;
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
