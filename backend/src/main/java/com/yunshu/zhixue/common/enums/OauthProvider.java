package com.yunshu.zhixue.common.enums;

import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import lombok.Getter;

/**
 * 第三方登录平台（{@code student_oauth.provider}）：1 微信 / 2 QQ / 3 GitHub。
 */
@Getter
public enum OauthProvider {

    WECHAT(1, "微信"),
    QQ(2, "QQ"),
    GITHUB(3, "GitHub");

    private final int code;
    private final String label;

    OauthProvider(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static OauthProvider fromCode(Integer code) {
        if (code != null) {
            for (OauthProvider provider : values()) {
                if (provider.code == code) {
                    return provider;
                }
            }
        }
        throw BizException.of(ErrorCode.PARAM_INVALID, "第三方平台不合法");
    }
}
