package com.yunshu.zhixue.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.yunshu.zhixue.common.exception.ErrorCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 统一响应体（API 设计 D4-1 方案 A）。
 *
 * <p>成功：HTTP 2xx + {@code {code:0, message:"ok", data:...}}；
 * 失败：真实 HTTP 状态码 + {@code {code:业务码, message, data:null, fieldErrors?/fileErrors?}}。</p>
 */
@Getter
@Setter
public class ApiResponse<T> {

    /** 业务码：0 表示成功，其余见 {@link ErrorCode}。 */
    private int code;

    private String message;

    private T data;

    /** 参数校验失败时的字段级错误（业务码 10000），其余情况不返回。 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<FieldErrorItem> fieldErrors;

    /** 批量导入校验失败时的行级错误（业务码 30005），非导入接口不返回。 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<FileRowError> fileErrors;

    private ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "ok", data);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(0, "ok", null);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(0, message, data);
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getCode(), errorCode.getDefaultMessage(), null);
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode, String message) {
        return new ApiResponse<>(errorCode.getCode(), message, null);
    }

    /** 参数校验失败：HTTP 400 + 业务码 10000 + 字段级错误。 */
    public static ApiResponse<Void> invalidParams(List<FieldErrorItem> fieldErrors) {
        ApiResponse<Void> response = fail(ErrorCode.PARAM_INVALID);
        response.setFieldErrors(fieldErrors);
        return response;
    }

    /** 批量导入校验失败：HTTP 400 + 业务码 30005 + 行级错误。 */
    public static ApiResponse<Void> invalidImport(List<FileRowError> fileErrors) {
        ApiResponse<Void> response = fail(ErrorCode.IMPORT_VALIDATION_FAILED);
        response.setFileErrors(fileErrors);
        return response;
    }
}
