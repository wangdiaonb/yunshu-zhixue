package com.yunshu.zhixue.modules.student.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * 修改个人资料入参（P-M02）。字段为 null 表示不修改；邮箱/头像传空字符串表示清空。
 */
public record ProfileUpdateRequest(

        @Size(max = 20, message = "昵称不能超过 20 个字符")
        String nickname,

        @Email(message = "邮箱格式不正确")
        @Size(max = 100, message = "邮箱长度不能超过 100 个字符")
        String email,

        @Min(value = 0, message = "性别取值不正确")
        @Max(value = 2, message = "性别取值不正确")
        Integer gender,

        Long preferredSubjectId,

        @Size(max = 500, message = "头像地址过长")
        String avatar
) {
}
