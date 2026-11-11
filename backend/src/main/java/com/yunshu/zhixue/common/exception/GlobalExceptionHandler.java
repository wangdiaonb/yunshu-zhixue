package com.yunshu.zhixue.common.exception;

import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.common.response.FieldErrorItem;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.ArrayList;
import java.util.List;

/**
 * 全局异常处理：统一响应结构、HTTP 状态码与业务码（api-rules §4、API 设计 §2.7）。
 *
 * <p>错误信息面向用户可读，不返回堆栈、SQL 与内部主机信息。</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Void>> handleBizException(BizException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        ApiResponse<Void> body = ApiResponse.fail(errorCode, ex.getMessage());
        if (ex.getFieldErrors() != null && !ex.getFieldErrors().isEmpty()) {
            body.setFieldErrors(ex.getFieldErrors());
        }
        if (ex.getFileErrors() != null && !ex.getFileErrors().isEmpty()) {
            body.setFileErrors(ex.getFileErrors());
        }
        return ResponseEntity.status(errorCode.getHttpStatus()).body(body);
    }

    /** @RequestBody 上的 Bean Validation 校验失败。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.invalidParams(toFieldErrors(ex.getBindingResult().getAllErrors())));
    }

    /** 表单/查询参数绑定校验失败。 */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.invalidParams(toFieldErrors(ex.getBindingResult().getAllErrors())));
    }

    /** 方法参数（@RequestParam / @PathVariable）校验失败。 */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        List<FieldErrorItem> errors = ex.getConstraintViolations().stream()
                .map(this::toFieldError)
                .toList();
        return ResponseEntity.badRequest().body(ApiResponse.invalidParams(errors));
    }

    /** Spring 6.1 方法级参数校验失败。 */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleHandlerMethodValidation(HandlerMethodValidationException ex) {
        List<FieldErrorItem> errors = new ArrayList<>();
        ex.getAllValidationResults().forEach(result ->
                result.getResolvableErrors().forEach(error -> errors.add(new FieldErrorItem(
                        result.getMethodParameter().getParameterName(),
                        error.getDefaultMessage()))));
        return ResponseEntity.badRequest().body(ApiResponse.invalidParams(errors));
    }

    /** JSON 解析失败（业务码 10007）。 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        log.debug("请求体解析失败：{}", ex.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.fail(ErrorCode.MALFORMED_BODY));
    }

    /** 缺少必填查询参数或参数类型不匹配，统一指向具体字段。 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.invalidParams(
                List.of(new FieldErrorItem(ex.getParameterName(), "缺少必填参数"))));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.invalidParams(
                List.of(new FieldErrorItem(ex.getName(), "参数类型不正确"))));
    }

    /** 上传体积超限（业务码 10008）。 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(ErrorCode.FILE_INVALID, "文件大小超出限制"));
    }

    /** 请求方法不支持：保留真实 HTTP 405。 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.fail(ErrorCode.PARAM_INVALID, "请求方法不支持"));
    }

    /** 静态资源/接口路径不存在。 */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail(ErrorCode.NOT_FOUND));
    }

    /** 过滤器链之外抛出的鉴权异常兜底。 */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.fail(ErrorCode.FORBIDDEN));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.fail(ErrorCode.UNAUTHORIZED));
    }

    /** 未预期异常：记录服务端日志，对外只返回通用错误。 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("未处理异常", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail(ErrorCode.INTERNAL_ERROR));
    }

    private List<FieldErrorItem> toFieldErrors(List<ObjectError> objectErrors) {
        List<FieldErrorItem> errors = new ArrayList<>();
        for (ObjectError error : objectErrors) {
            if (error instanceof FieldError fieldError) {
                errors.add(new FieldErrorItem(fieldError.getField(),
                        fieldError.getDefaultMessage() == null ? "参数不合法" : fieldError.getDefaultMessage()));
            } else {
                errors.add(new FieldErrorItem(error.getObjectName(),
                        error.getDefaultMessage() == null ? "参数不合法" : error.getDefaultMessage()));
            }
        }
        return errors;
    }

    private FieldErrorItem toFieldError(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
        return new FieldErrorItem(field, violation.getMessage());
    }
}
