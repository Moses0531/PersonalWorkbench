package com.moses.utils;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.moses.config.ResultConfig;
import com.moses.exception.BusinessException;
import com.moses.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 全局异常处理器。业务异常带回 {@link ErrorCode}，其余异常只返回固定文案。
 * 所有响应体保持 ResultConfig 结构不变，HTTP 状态码按 {@link ErrorCode#httpStatus()} 映射。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandlerUtil {

    private static final String SYSTEM_BUSY = "系统繁忙，请稍后重试";

    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<ResultConfig> handleNotLoginException(NotLoginException e) {
        return respond(ErrorCode.UNAUTHORIZED, "未登录，请先登录");
    }

    @ExceptionHandler(NotPermissionException.class)
    public ResponseEntity<ResultConfig> handleNotPermissionException(NotPermissionException e) {
        return respond(ErrorCode.FORBIDDEN, "权限不足，无法访问该资源");
    }

    @ExceptionHandler(NotRoleException.class)
    public ResponseEntity<ResultConfig> handleNotRoleException(NotRoleException e) {
        return respond(ErrorCode.FORBIDDEN, "角色权限不足，无法访问该资源");
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ResultConfig> handleBusinessException(BusinessException e) {
        return respond(e.getErrorCode(), e.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ResultConfig> handleMissingParam(MissingServletRequestParameterException e) {
        return respond(ErrorCode.BAD_REQUEST, "缺少参数：" + e.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ResultConfig> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return respond(ErrorCode.BAD_REQUEST, "参数格式不正确：" + e.getName());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, BindException.class})
    public ResponseEntity<ResultConfig> handleBadRequest(Exception e) {
        return respond(ErrorCode.BAD_REQUEST, "请求参数不正确");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ResultConfig> handleMaxUpload(MaxUploadSizeExceededException e) {
        return respond(ErrorCode.BAD_REQUEST, "上传文件过大");
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ResultConfig> handleRuntimeException(RuntimeException e) {
        log.error("未处理的运行时异常", e);
        return respond(ErrorCode.SYSTEM_ERROR, SYSTEM_BUSY);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResultConfig> handleException(Exception e) {
        log.error("未处理的异常", e);
        return respond(ErrorCode.SYSTEM_ERROR, SYSTEM_BUSY);
    }

    /** 按错误码映射 HTTP 状态码，同时保留统一响应体 */
    private ResponseEntity<ResultConfig> respond(ErrorCode errorCode, String msg) {
        ErrorCode code = errorCode == null ? ErrorCode.BAD_REQUEST : errorCode;
        return ResponseEntity.status(HttpStatus.valueOf(code.httpStatus()))
                .body(ResultConfig.error(code, msg));
    }
}
