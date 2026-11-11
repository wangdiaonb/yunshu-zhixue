package com.yunshu.zhixue.modules.student.dto;

/**
 * 登录取返回的学员基本信息。
 *
 * @param id       学员主键
 * @param nickname 昵称
 * @param avatar   头像 URL
 */
public record StudentSummary(Long id, String nickname, String avatar) {
}
