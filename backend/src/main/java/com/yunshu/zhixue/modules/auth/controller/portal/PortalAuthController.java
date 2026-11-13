package com.yunshu.zhixue.modules.auth.controller.portal;

import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.modules.auth.dto.AccountPasswordLoginRequest;
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
import com.yunshu.zhixue.security.TokenAudience;
import com.yunshu.zhixue.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学员端认证接口（P-A03 / P-A07 / P-A08）。
 */
@RestController
@RequestMapping(AppConstants.PORTAL_API_PREFIX + "/auth")
@RequiredArgsConstructor
@Tag(name = "学员端-认证")
public class PortalAuthController {

    private final AuthService authService;

    @PostMapping("/login/password")
    @Operation(summary = "密码登录（手机号或邮箱）")
    public ApiResponse<PortalLoginResponse> login(@Valid @RequestBody AccountPasswordLoginRequest request) {
        return ApiResponse.ok(authService.loginPortal(request));
    }

    @PostMapping("/sms-code")
    @Operation(summary = "发送短信验证码（注册 / 登录 / 找回密码）")
    public ApiResponse<SmsCodeResponse> sendSmsCode(@Valid @RequestBody SmsCodeRequest request) {
        return ApiResponse.ok(authService.sendSmsCode(request));
    }

    @PostMapping("/register")
    @Operation(summary = "学员注册")
    public ApiResponse<PortalLoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    @PostMapping("/login/sms")
    @Operation(summary = "验证码登录")
    public ApiResponse<PortalLoginResponse> loginBySms(@Valid @RequestBody SmsLoginRequest request) {
        return ApiResponse.ok(authService.loginPortalBySms(request));
    }

    @PostMapping("/reset/verify")
    @Operation(summary = "找回密码·验证手机号")
    public ApiResponse<ResetVerifyResponse> verifyResetCode(@Valid @RequestBody ResetVerifyRequest request) {
        return ApiResponse.ok(authService.verifyResetCode(request));
    }

    @PostMapping("/reset")
    @Operation(summary = "找回密码·重置密码")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.ok();
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌（旧 refreshToken 立即失效）")
    public ApiResponse<TokenPairResponse> refresh(
            @RequestHeader(name = AppConstants.REFRESH_TOKEN_HEADER, required = false) String refreshToken) {
        return ApiResponse.ok(authService.refresh(TokenAudience.PORTAL, refreshToken));
    }

    @PostMapping("/logout")
    @Operation(summary = "登出（幂等）")
    public ApiResponse<Void> logout(
            @RequestHeader(name = AppConstants.REFRESH_TOKEN_HEADER, required = false) String refreshToken) {
        authService.logout(TokenAudience.PORTAL, UserContext.get(), refreshToken);
        return ApiResponse.ok();
    }
}
