package com.yunshu.zhixue.common.exception;

import com.yunshu.zhixue.common.response.FieldErrorItem;
import com.yunshu.zhixue.common.response.FileRowError;
import lombok.Getter;

import java.util.List;

/**
 * 业务异常：由全局异常处理器映射为真实 HTTP 状态码 + 业务码。
 */
@Getter
public class BizException extends RuntimeException {

    private final ErrorCode errorCode;

    private List<FieldErrorItem> fieldErrors;

    private List<FileRowError> fileErrors;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public static BizException of(ErrorCode errorCode) {
        return new BizException(errorCode);
    }

    public static BizException of(ErrorCode errorCode, String message) {
        return new BizException(errorCode, message);
    }

    /** 参数校验失败（业务码 10000），可携带字段级错误。 */
    public static BizException invalidParams(List<FieldErrorItem> fieldErrors) {
        BizException exception = new BizException(ErrorCode.PARAM_INVALID);
        exception.fieldErrors = fieldErrors;
        return exception;
    }

    /** 批量导入校验失败（业务码 30005），可携带行级错误。 */
    public static BizException invalidImport(List<FileRowError> fileErrors) {
        BizException exception = new BizException(ErrorCode.IMPORT_VALIDATION_FAILED);
        exception.fileErrors = fileErrors;
        return exception;
    }
}
