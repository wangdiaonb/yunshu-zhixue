package com.yunshu.zhixue.modules.auth.service;

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
import com.yunshu.zhixue.security.LoginUser;
import com.yunshu.zhixue.security.TokenAudience;

/**
 * 双端认证服务：登录、令牌刷新与登出。
 */
public interface AuthService {

    /** 学员端密码登录（P-A03）。 */
    PortalLoginResponse loginPortal(AccountPasswordLoginRequest request);

    /** 发送短信验证码（P-A01）。 */
    SmsCodeResponse sendSmsCode(SmsCodeRequest request);

    /** 学员注册，成功后直接签发双令牌（P-A02）。 */
    PortalLoginResponse register(RegisterRequest request);

    /** 学员验证码登录（P-A04）。 */
    PortalLoginResponse loginPortalBySms(SmsLoginRequest request);

    /** 找回密码·验证手机号并发放一次性重置令牌（P-A05）。 */
    ResetVerifyResponse verifyResetCode(ResetVerifyRequest request);

    /** 找回密码·重置密码（P-A06）。 */
    void resetPassword(ResetPasswordRequest request);

    /**
     * 管理端登录（A-A01）。
     *
     * @param ip 客户端 IP，用于登录操作日志
     */
    AdminLoginResponse loginAdmin(AccountPasswordLoginRequest request, String ip);

    /** 刷新令牌，旧 refreshToken 使用后立即失效（P-A07 / A-A02）。 */
    TokenPairResponse refresh(TokenAudience audience, String refreshToken);

    /**
     * 登出：accessToken 的 jti 进黑名单、refreshToken 撤销；重复登出幂等。
     *
     * @param loginUser    当前登录用户，可为 {@code null}（无有效访问令牌时）
     * @param refreshToken 请求头携带的刷新令牌，可为 {@code null}
     */
    void logout(TokenAudience audience, LoginUser loginUser, String refreshToken);
}
