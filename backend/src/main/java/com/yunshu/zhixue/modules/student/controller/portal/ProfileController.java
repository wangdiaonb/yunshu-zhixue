package com.yunshu.zhixue.modules.student.controller.portal;

import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.modules.auth.service.AuthService;
import com.yunshu.zhixue.modules.student.dto.ChangePasswordRequest;
import com.yunshu.zhixue.modules.student.dto.ProfileUpdateRequest;
import com.yunshu.zhixue.modules.student.dto.StudentProfileResponse;
import com.yunshu.zhixue.modules.student.service.StudentProfileService;
import com.yunshu.zhixue.security.LoginUser;
import com.yunshu.zhixue.security.TokenAudience;
import com.yunshu.zhixue.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学员端个人中心接口（P-M01 ~ P-M04）。
 */
@RestController
@RequestMapping(AppConstants.PORTAL_API_PREFIX + "/profile")
@RequiredArgsConstructor
@Tag(name = "学员端-个人中心")
public class ProfileController {

    private final StudentProfileService studentProfileService;
    private final AuthService authService;

    @GetMapping
    @Operation(summary = "个人信息")
    public ApiResponse<StudentProfileResponse> getProfile() {
        return ApiResponse.ok(studentProfileService.getProfile(UserContext.requireStudentId()));
    }

    @PutMapping
    @Operation(summary = "修改资料")
    public ApiResponse<StudentProfileResponse> updateProfile(
            @Valid @RequestBody ProfileUpdateRequest request) {
        return ApiResponse.ok(studentProfileService.updateProfile(UserContext.requireStudentId(), request));
    }

    @PutMapping("/password")
    @Operation(summary = "修改密码（成功后当前登录态失效）")
    public ApiResponse<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @RequestHeader(name = AppConstants.REFRESH_TOKEN_HEADER, required = false) String refreshToken) {
        Long studentId = UserContext.requireStudentId();
        studentProfileService.changePassword(studentId, request);

        LoginUser loginUser = UserContext.getRequired();
        authService.logout(TokenAudience.PORTAL, loginUser, refreshToken);
        return ApiResponse.ok();
    }

    @PostMapping("/guide")
    @Operation(summary = "完成新手引导（幂等）")
    public ApiResponse<Void> finishGuide() {
        studentProfileService.finishGuide(UserContext.requireStudentId());
        return ApiResponse.ok();
    }
}
