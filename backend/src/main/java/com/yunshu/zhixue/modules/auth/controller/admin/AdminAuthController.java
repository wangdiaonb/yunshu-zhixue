package com.yunshu.zhixue.modules.auth.controller.admin;

import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.common.util.IpUtils;
import com.yunshu.zhixue.modules.auth.dto.AccountPasswordLoginRequest;
import com.yunshu.zhixue.modules.auth.dto.AdminLoginResponse;
import com.yunshu.zhixue.modules.auth.dto.TokenPairResponse;
import com.yunshu.zhixue.modules.auth.service.AuthService;
import com.yunshu.zhixue.security.TokenAudience;
import com.yunshu.zhixue.security.UserContext;
import com.yunshu.zhixue.support.log.OperationAction;
import com.yunshu.zhixue.support.log.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端认证接口（A-A01 ~ A-A03）。
 */
@RestController
@RequestMapping(AppConstants.ADMIN_API_PREFIX + "/auth")
@RequiredArgsConstructor
@Tag(name = "管理端-认证")
public class AdminAuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "后台登录（用户名或手机号）")
    public ApiResponse<AdminLoginResponse> login(@Valid @RequestBody AccountPasswordLoginRequest request,
                                                 HttpServletRequest servletRequest) {
        return ApiResponse.ok(authService.loginAdmin(request, IpUtils.clientIp(servletRequest)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌（旧 refreshToken 立即失效）")
    public ApiResponse<TokenPairResponse> refresh(
            @RequestHeader(name = AppConstants.REFRESH_TOKEN_HEADER, required = false) String refreshToken) {
        return ApiResponse.ok(authService.refresh(TokenAudience.ADMIN, refreshToken));
    }

    @PostMapping("/logout")
    @OperationLog(action = OperationAction.LOGOUT, targetType = "admin_user", targetName = "后台登出")
    @Operation(summary = "登出（幂等）")
    public ApiResponse<Void> logout(
            @RequestHeader(name = AppConstants.REFRESH_TOKEN_HEADER, required = false) String refreshToken) {
        authService.logout(TokenAudience.ADMIN, UserContext.get(), refreshToken);
        return ApiResponse.ok();
    }
}
