package com.yunshu.zhixue.modules.auth.service.impl;

import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.modules.auth.dto.AccountPasswordLoginRequest;
import com.yunshu.zhixue.modules.auth.dto.AdminLoginResponse;
import com.yunshu.zhixue.modules.auth.dto.PortalLoginResponse;
import com.yunshu.zhixue.modules.auth.dto.RegisterRequest;
import com.yunshu.zhixue.modules.auth.dto.ResetPasswordRequest;
import com.yunshu.zhixue.modules.auth.dto.ResetVerifyRequest;
import com.yunshu.zhixue.modules.auth.dto.ResetVerifyResponse;
import com.yunshu.zhixue.modules.auth.dto.SmsCodeRequest;
import com.yunshu.zhixue.modules.auth.dto.SmsCodeResponse;
import com.yunshu.zhixue.modules.auth.dto.SmsLoginRequest;
import com.yunshu.zhixue.modules.auth.dto.TokenPairResponse;
import com.yunshu.zhixue.modules.auth.service.AuthService;
import com.yunshu.zhixue.modules.auth.support.LoginAttemptGuard;
import com.yunshu.zhixue.modules.auth.support.PasswordResetTokenStore;
import com.yunshu.zhixue.modules.auth.support.RefreshTokenStore;
import com.yunshu.zhixue.modules.auth.support.SmsCodeDelivery;
import com.yunshu.zhixue.modules.auth.support.SmsCodeStore;
import com.yunshu.zhixue.modules.student.dto.StudentAccount;
import com.yunshu.zhixue.modules.student.dto.StudentSummary;
import com.yunshu.zhixue.modules.student.service.StudentService;
import com.yunshu.zhixue.modules.system.dto.AdminAccount;
import com.yunshu.zhixue.modules.system.dto.AdminSummary;
import com.yunshu.zhixue.modules.system.service.AdminUserService;
import com.yunshu.zhixue.security.JwtTokenService;
import com.yunshu.zhixue.security.LoginUser;
import com.yunshu.zhixue.security.TokenAudience;
import com.yunshu.zhixue.security.TokenBlacklistService;
import com.yunshu.zhixue.support.log.OperationAction;
import com.yunshu.zhixue.support.log.OperationLogCommand;
import com.yunshu.zhixue.support.log.OperationLogRecorder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 双端认证实现。
 *
 * <p>规则：密码连续错误 5 次锁定 5 分钟（D18）；禁用账号不可登录；
 * 登录成功更新最后登录时间并清空失败计数；管理端登录写入操作日志。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final StudentService studentService;
    private final AdminUserService adminUserService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenStore refreshTokenStore;
    private final LoginAttemptGuard loginAttemptGuard;
    private final SmsCodeStore smsCodeStore;
    private final PasswordResetTokenStore passwordResetTokenStore;
    private final TokenBlacklistService tokenBlacklistService;
    private final OperationLogRecorder operationLogRecorder;

    /** 开发环境回显验证码；生产环境为 false，不通过接口返回验证码 */
    @Value("${app.sms.log-code:false}")
    private boolean exposeSmsCode;

    @Override
    public PortalLoginResponse loginPortal(AccountPasswordLoginRequest request) {
        String account = normalize(request.account());
        if (loginAttemptGuard.isLocked(TokenAudience.PORTAL, account)) {
            throw locked(ErrorCode.ACCOUNT_LOCKED);
        }

        StudentAccount student = studentService.findAccountForLogin(account);
        if (student == null || !passwordMatches(request.password(), student.passwordHash())) {
            throw failure(TokenAudience.PORTAL, account, ErrorCode.BAD_CREDENTIALS, ErrorCode.ACCOUNT_LOCKED);
        }
        if (!student.isEnabled()) {
            throw BizException.of(ErrorCode.ACCOUNT_DISABLED);
        }

        loginAttemptGuard.clear(TokenAudience.PORTAL, account);
        studentService.recordLogin(student.id());
        TokenPairResponse tokens = issue(TokenAudience.PORTAL, student.id(), student.mobile());
        StudentSummary summary = new StudentSummary(student.id(), student.nickname(), student.avatar());
        return new PortalLoginResponse(tokens.accessToken(), tokens.refreshToken(), summary);
    }

    @Override
    public SmsCodeResponse sendSmsCode(SmsCodeRequest request) {
        String mobile = normalizeMobile(request.mobile());
        switch (request.type()) {
            case "register" -> {
                if (studentService.existsByMobile(mobile)) {
                    throw BizException.of(ErrorCode.CONTACT_OCCUPIED, "该手机号已注册，请直接登录");
                }
            }
            case "reset" -> {
                if (!studentService.existsByMobile(mobile)) {
                    throw BizException.of(ErrorCode.NOT_FOUND, "该手机号尚未注册");
                }
            }
            case "login" -> {
                // 验证码登录不校验注册态，登录时再给出统一提示，避免账号枚举
            }
            default -> throw BizException.of(ErrorCode.PARAM_INVALID, "验证码用途不正确");
        }
        SmsCodeDelivery delivery = smsCodeStore.send(request.type(), mobile);
        return new SmsCodeResponse(
                delivery.expireSeconds(),
                exposeSmsCode ? delivery.code() : null);
    }

    @Override
    public PortalLoginResponse register(RegisterRequest request) {
        String mobile = normalizeMobile(request.mobile());
        if (studentService.existsByMobile(mobile)) {
            throw BizException.of(ErrorCode.CONTACT_OCCUPIED, "该手机号已注册，请直接登录");
        }
        smsCodeStore.verify("register", mobile, request.code().trim());

        String nickname = "用户" + mobile.substring(mobile.length() - 4);
        StudentAccount student;
        try {
            student = studentService.createStudent(mobile, passwordEncoder.encode(request.password()), nickname);
        } catch (DuplicateKeyException ex) {
            throw BizException.of(ErrorCode.CONTACT_OCCUPIED, "该手机号已注册，请直接登录");
        }
        if (student == null) {
            throw BizException.of(ErrorCode.INTERNAL_ERROR, "注册失败，请稍后重试");
        }

        loginAttemptGuard.clear(TokenAudience.PORTAL, mobile);
        studentService.recordLogin(student.id());
        TokenPairResponse tokens = issue(TokenAudience.PORTAL, student.id(), student.mobile());
        StudentSummary summary = new StudentSummary(student.id(), student.nickname(), student.avatar());
        return new PortalLoginResponse(tokens.accessToken(), tokens.refreshToken(), summary);
    }

    @Override
    public PortalLoginResponse loginPortalBySms(SmsLoginRequest request) {
        String mobile = normalizeMobile(request.mobile());
        smsCodeStore.verify("login", mobile, request.code().trim());

        StudentAccount student = studentService.findAccountByMobile(mobile);
        if (student == null) {
            throw BizException.of(ErrorCode.BAD_CREDENTIALS, "该手机号尚未注册，请先注册");
        }
        if (!student.isEnabled()) {
            throw BizException.of(ErrorCode.ACCOUNT_DISABLED);
        }

        loginAttemptGuard.clear(TokenAudience.PORTAL, mobile);
        studentService.recordLogin(student.id());
        TokenPairResponse tokens = issue(TokenAudience.PORTAL, student.id(), student.mobile());
        StudentSummary summary = new StudentSummary(student.id(), student.nickname(), student.avatar());
        return new PortalLoginResponse(tokens.accessToken(), tokens.refreshToken(), summary);
    }

    @Override
    public ResetVerifyResponse verifyResetCode(ResetVerifyRequest request) {
        String mobile = normalizeMobile(request.mobile());
        if (!studentService.existsByMobile(mobile)) {
            throw BizException.of(ErrorCode.NOT_FOUND, "该手机号尚未注册");
        }
        smsCodeStore.verify("reset", mobile, request.code().trim());

        StudentAccount student = studentService.findAccountByMobile(mobile);
        if (student == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "该手机号尚未注册");
        }
        return new ResetVerifyResponse(passwordResetTokenStore.issue(student.id()));
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        Long studentId = passwordResetTokenStore.consume(request.resetToken());
        studentService.updatePasswordHash(studentId, passwordEncoder.encode(request.newPassword()));
    }

    @Override
    public AdminLoginResponse loginAdmin(AccountPasswordLoginRequest request, String ip) {
        String account = normalize(request.account());
        if (loginAttemptGuard.isLocked(TokenAudience.ADMIN, account)) {
            throw locked(ErrorCode.ADMIN_ACCOUNT_LOCKED);
        }

        AdminAccount admin = adminUserService.findAccountForLogin(account);
        if (admin == null || !passwordMatches(request.password(), admin.passwordHash())) {
            throw failure(TokenAudience.ADMIN, account,
                    ErrorCode.ADMIN_BAD_CREDENTIALS, ErrorCode.ADMIN_ACCOUNT_LOCKED);
        }
        if (!admin.isEnabled()) {
            throw BizException.of(ErrorCode.ADMIN_ACCOUNT_DISABLED);
        }

        loginAttemptGuard.clear(TokenAudience.ADMIN, account);
        adminUserService.recordLogin(admin.id());

        AdminSummary summary = adminUserService.getSummary(admin.id());
        List<String> permissions = adminUserService.listPermissionCodes(admin.id());
        TokenPairResponse tokens = issue(TokenAudience.ADMIN, admin.id(), admin.username());

        operationLogRecorder.record(new OperationLogCommand(
                admin.id(), admin.username(), OperationAction.LOGIN.getLabel(),
                "admin_user", "后台登录", ip));

        return new AdminLoginResponse(tokens.accessToken(), tokens.refreshToken(), summary, permissions);
    }

    @Override
    public TokenPairResponse refresh(TokenAudience audience, String refreshToken) {
        JwtTokenService.TokenPayload payload = parseRefresh(audience, refreshToken);
        if (!refreshTokenStore.consume(payload)) {
            // 令牌已被轮换或登出，防止重放
            throw BizException.of(ErrorCode.UNAUTHORIZED);
        }

        if (audience == TokenAudience.PORTAL) {
            StudentAccount student = studentService.findAccountById(payload.userId());
            if (student == null) {
                throw BizException.of(ErrorCode.UNAUTHORIZED);
            }
            if (!student.isEnabled()) {
                throw BizException.of(ErrorCode.ACCOUNT_DISABLED);
            }
            return issue(TokenAudience.PORTAL, student.id(), student.mobile());
        }

        AdminAccount admin = adminUserService.findAccountById(payload.userId());
        if (admin == null) {
            throw BizException.of(ErrorCode.UNAUTHORIZED);
        }
        if (!admin.isEnabled()) {
            throw BizException.of(ErrorCode.ADMIN_ACCOUNT_DISABLED);
        }
        return issue(TokenAudience.ADMIN, admin.id(), admin.username());
    }

    @Override
    public void logout(TokenAudience audience, LoginUser loginUser, String refreshToken) {
        if (loginUser != null && loginUser.audience() == audience) {
            tokenBlacklistService.blacklist(loginUser.tokenId(), loginUser.expiresAt());
        }
        if (StringUtils.hasText(refreshToken)) {
            try {
                refreshTokenStore.revoke(jwtTokenService.parseRefreshToken(refreshToken.trim(), audience));
            } catch (BizException ex) {
                // 令牌已失效：登出保持幂等
                log.debug("登出时忽略无效刷新令牌：{}", ex.getMessage());
            }
        }
    }

    private TokenPairResponse issue(TokenAudience audience, Long userId, String account) {
        String accessToken = jwtTokenService.createAccessToken(userId, account, audience);
        String refreshToken = jwtTokenService.createRefreshToken(userId, account, audience);
        refreshTokenStore.store(jwtTokenService.parseRefreshToken(refreshToken, audience));
        return new TokenPairResponse(accessToken, refreshToken);
    }

    private JwtTokenService.TokenPayload parseRefresh(TokenAudience audience, String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw BizException.of(ErrorCode.UNAUTHORIZED);
        }
        try {
            return jwtTokenService.parseRefreshToken(refreshToken.trim(), audience);
        } catch (BizException ex) {
            throw BizException.of(ErrorCode.UNAUTHORIZED);
        }
    }

    /**
     * 记录一次密码错误；达到上限时返回锁定错误，否则返回账号或密码错误。
     */
    private BizException failure(TokenAudience audience, String account,
                                 ErrorCode credentialError, ErrorCode lockedError) {
        int failures = loginAttemptGuard.recordFailure(audience, account);
        if (failures >= AppConstants.LOGIN_MAX_FAILURES) {
            return locked(lockedError);
        }
        return BizException.of(credentialError);
    }

    private BizException locked(ErrorCode errorCode) {
        return BizException.of(errorCode, "连续 " + AppConstants.LOGIN_MAX_FAILURES
                + " 次密码错误，账号已锁定 " + AppConstants.LOGIN_LOCK_DURATION.toMinutes() + " 分钟");
    }

    private boolean passwordMatches(String rawPassword, String passwordHash) {
        return StringUtils.hasText(passwordHash)
                && passwordHash.startsWith("$2")
                && passwordEncoder.matches(rawPassword, passwordHash);
    }

    private String normalize(String account) {
        return account == null ? "" : account.trim();
    }

    private String normalizeMobile(String mobile) {
        return mobile == null ? "" : mobile.trim();
    }
}
