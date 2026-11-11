package com.yunshu.zhixue.modules.system.support;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yunshu.zhixue.modules.system.entity.AdminUser;
import com.yunshu.zhixue.modules.system.mapper.AdminUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 内置超级管理员密码初始化。
 *
 * <p>建库脚本中的 hash 为占位符，首次启动时按 {@code app.default-password} 生成有效 BCrypt
 * 并写回（Q9：默认密码走应用配置项，不使用 system_setting）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountInitializer implements ApplicationRunner {

    /** 与 scripts/sql/schema.sql 中的占位符保持一致 */
    private static final String PLACEHOLDER_HASH = "__PLACEHOLDER_INVALID_HASH__RESET_AT_DEPLOYMENT__";

    private final AdminUserMapper adminUserMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.default-password:}")
    private String defaultPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(defaultPassword)) {
            return;
        }
        try {
            AdminUser builtin = adminUserMapper.selectOne(Wrappers.<AdminUser>lambdaQuery()
                    .eq(AdminUser::getIsBuiltin, 1)
                    .last("LIMIT 1"));
            if (builtin == null || !PLACEHOLDER_HASH.equals(builtin.getPasswordHash())) {
                return;
            }
            int updated = adminUserMapper.update(null, Wrappers.<AdminUser>lambdaUpdate()
                    .eq(AdminUser::getId, builtin.getId())
                    .eq(AdminUser::getPasswordHash, PLACEHOLDER_HASH)
                    .set(AdminUser::getPasswordHash, passwordEncoder.encode(defaultPassword)));
            if (updated > 0) {
                log.warn("内置超级管理员 [{}] 的密码已初始化为部署默认密码，请登录后尽快修改", builtin.getUsername());
            }
        } catch (Exception ex) {
            // 数据库尚未就绪时不阻塞应用启动，由后续登录失败暴露问题
            log.warn("内置管理员密码初始化未完成：{}", ex.getMessage());
        }
    }
}
