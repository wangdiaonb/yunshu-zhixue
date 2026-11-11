/**
 * 认证模块：学员端与管理端注册、登录、找回密码、第三方登录、令牌刷新与登出。
 *
 * <p>依赖 {@code student}/{@code system} 的服务接口读写账号，不直接持有对方 Mapper；
 * 登录态、验证码、失败锁定等状态存 Redis。</p>
 */
package com.yunshu.zhixue.modules.auth;
