package com.yunshu.zhixue.modules.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.modules.system.dto.PermissionNode;
import com.yunshu.zhixue.modules.system.dto.RoleCard;
import com.yunshu.zhixue.modules.system.dto.RoleCreateRequest;
import com.yunshu.zhixue.modules.system.dto.RoleOption;
import com.yunshu.zhixue.modules.system.dto.RolePermissionTag;
import com.yunshu.zhixue.modules.system.entity.AdminUser;
import com.yunshu.zhixue.modules.system.entity.Permission;
import com.yunshu.zhixue.modules.system.entity.Role;
import com.yunshu.zhixue.modules.system.entity.RolePermission;
import com.yunshu.zhixue.modules.system.mapper.AdminUserMapper;
import com.yunshu.zhixue.modules.system.mapper.PermissionMapper;
import com.yunshu.zhixue.modules.system.mapper.RoleMapper;
import com.yunshu.zhixue.modules.system.mapper.RolePermissionMapper;
import com.yunshu.zhixue.modules.system.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private static final String SUPER_ADMIN_CODE = "super_admin";
    private static final int TYPE_GROUP = 1;
    private static final int TYPE_OPERATION = 2;

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final AdminUserMapper adminUserMapper;

    @Override
    public List<RoleCard> listRoles() {
        List<Role> roles = roleMapper.selectList(Wrappers.<Role>lambdaQuery().orderByAsc(Role::getId));
        List<Permission> permissions = listPermissions();
        List<Permission> operations = permissions.stream()
                .filter(item -> Objects.equals(item.getType(), TYPE_OPERATION))
                .toList();
        Map<Long, Permission> permissionById = operations.stream()
                .collect(Collectors.toMap(Permission::getId, Function.identity()));
        Map<Long, String> groupNameById = permissions.stream()
                .filter(item -> Objects.equals(item.getType(), TYPE_GROUP))
                .collect(Collectors.toMap(Permission::getId, Permission::getName));

        List<RoleCard> cards = new ArrayList<>();
        for (Role role : roles) {
            boolean superAdmin = SUPER_ADMIN_CODE.equals(role.getCode());
            Set<Long> rolePermissionIds = superAdmin
                    ? operations.stream().map(Permission::getId).collect(Collectors.toCollection(LinkedHashSet::new))
                    : loadRolePermissionIds(role.getId());

            List<RolePermissionTag> tags = rolePermissionIds.stream()
                    .map(permissionById::get)
                    .filter(Objects::nonNull)
                    .map(permission -> new RolePermissionTag(
                            permission.getId(),
                            permission.getCode(),
                            permission.getName(),
                            groupNameById.getOrDefault(permission.getParentId(), "")))
                    .toList();

            Long userCount = adminUserMapper.selectCount(Wrappers.<AdminUser>lambdaQuery()
                    .eq(AdminUser::getRoleId, role.getId()));

            cards.add(new RoleCard(
                    role.getId(),
                    role.getName(),
                    role.getCode(),
                    role.getDescription(),
                    userCount == null ? 0 : userCount,
                    tags,
                    operations.size(),
                    superAdmin));
        }
        return cards;
    }

    @Override
    public Long createRole(RoleCreateRequest request) {
        String code = request.code().trim();
        Long exists = roleMapper.selectCount(Wrappers.<Role>lambdaQuery().eq(Role::getCode, code));
        if (exists != null && exists > 0) {
            throw BizException.of(ErrorCode.DUPLICATE_NAME, "角色编码已存在");
        }

        Role role = new Role();
        role.setName(request.name().trim());
        role.setCode(code);
        role.setDescription(StringUtils.hasText(request.description()) ? request.description().trim() : null);
        try {
            roleMapper.insert(role);
        } catch (DuplicateKeyException ex) {
            throw BizException.of(ErrorCode.DUPLICATE_NAME, "角色编码已存在");
        }
        return role.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long roleId) {
        Role role = requireRole(roleId);
        if (SUPER_ADMIN_CODE.equals(role.getCode())) {
            throw BizException.of(ErrorCode.BUILTIN_IMMUTABLE, "超级管理员角色不可删除");
        }
        Long userCount = adminUserMapper.selectCount(Wrappers.<AdminUser>lambdaQuery()
                .eq(AdminUser::getRoleId, roleId));
        if (userCount != null && userCount > 0) {
            throw BizException.of(ErrorCode.ROLE_IN_USE);
        }
        rolePermissionMapper.delete(Wrappers.<RolePermission>lambdaQuery()
                .eq(RolePermission::getRoleId, roleId));
        roleMapper.deleteById(roleId);
    }

    @Override
    public List<PermissionNode> permissionTree(Long roleId) {
        Role role = roleId == null ? null : requireRole(roleId);
        List<Permission> permissions = listPermissions();
        Set<Long> checkedIds = new HashSet<>();
        if (role != null) {
            if (SUPER_ADMIN_CODE.equals(role.getCode())) {
                permissions.stream()
                        .filter(item -> Objects.equals(item.getType(), TYPE_OPERATION))
                        .map(Permission::getId)
                        .forEach(checkedIds::add);
            } else {
                checkedIds.addAll(loadRolePermissionIds(roleId));
            }
        }

        Map<Long, List<Permission>> childrenByParent = permissions.stream()
                .filter(item -> Objects.equals(item.getType(), TYPE_OPERATION) && item.getParentId() != null)
                .collect(Collectors.groupingBy(Permission::getParentId));

        List<PermissionNode> tree = new ArrayList<>();
        for (Permission group : permissions) {
            if (!Objects.equals(group.getType(), TYPE_GROUP)) {
                continue;
            }
            List<Permission> children = childrenByParent.getOrDefault(group.getId(), List.of());
            List<PermissionNode> childNodes = children.stream()
                    .map(child -> new PermissionNode(child.getId(), child.getName(), child.getCode(),
                            checkedIds.contains(child.getId()), List.of()))
                    .toList();
            boolean groupChecked = !childNodes.isEmpty()
                    && childNodes.stream().allMatch(PermissionNode::checked);
            tree.add(new PermissionNode(group.getId(), group.getName(), group.getCode(),
                    groupChecked, childNodes));
        }
        return tree;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void savePermissions(Long roleId, List<Long> permissionIds) {
        Role role = requireRole(roleId);
        if (SUPER_ADMIN_CODE.equals(role.getCode())) {
            throw BizException.of(ErrorCode.BUILTIN_IMMUTABLE, "超级管理员角色权限不可编辑");
        }

        Set<Long> ids = permissionIds == null ? Set.of()
                : permissionIds.stream().filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (!ids.isEmpty()) {
            List<Permission> operations = permissionMapper.selectList(Wrappers.<Permission>lambdaQuery()
                    .in(Permission::getId, ids)
                    .eq(Permission::getType, TYPE_OPERATION));
            if (operations.size() != ids.size()) {
                throw BizException.of(ErrorCode.PERMISSION_SAVE_FAILED);
            }
        }

        rolePermissionMapper.delete(Wrappers.<RolePermission>lambdaQuery()
                .eq(RolePermission::getRoleId, roleId));
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        for (Long permissionId : ids) {
            RolePermission relation = new RolePermission();
            relation.setRoleId(roleId);
            relation.setPermissionId(permissionId);
            relation.setCreatedAt(now);
            rolePermissionMapper.insert(relation);
        }
    }

    @Override
    public List<RoleOption> listOptions() {
        return roleMapper.selectList(Wrappers.<Role>lambdaQuery().orderByAsc(Role::getId)).stream()
                .map(role -> new RoleOption(role.getId(), role.getName(), role.getCode()))
                .toList();
    }

    private List<Permission> listPermissions() {
        return permissionMapper.selectList(Wrappers.<Permission>lambdaQuery()
                .orderByAsc(Permission::getSortOrder)
                .orderByAsc(Permission::getId));
    }

    private Set<Long> loadRolePermissionIds(Long roleId) {
        return rolePermissionMapper.selectList(Wrappers.<RolePermission>lambdaQuery()
                        .eq(RolePermission::getRoleId, roleId))
                .stream()
                .map(RolePermission::getPermissionId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Role requireRole(Long roleId) {
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "角色不存在");
        }
        return role;
    }
}
