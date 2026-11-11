package com.yunshu.zhixue.modules.auth.service;

import com.yunshu.zhixue.modules.auth.dto.AccountPasswordLoginRequest;
import com.yunshu.zhixue.modules.auth.dto.AdminLoginResponse;
import com.yunshu.zhixue.modules.auth.dto.PortalLoginResponse;
import com.yunshu.zhixue.modules.auth.dto.TokenPairResponse;
import com.yunshu.zhixue.security.LoginUser;
import com.yunshu.zhixue.security.TokenAudience;

/**
 * 双端认证服务：登录、令牌刷新与登出。
 */
public interface AuthService {

    /** 学员端密码登录（P-A03）。 */
    PortalLoginResponse loginPortal(AccountPasswordLoginRequest request);

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
