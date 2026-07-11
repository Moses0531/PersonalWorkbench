package com.moses.utils;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.moses.config.ResultConfig;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandlerUtil {
    
    /**
     * 处理未登录异常
     */
    @ExceptionHandler(NotLoginException.class)
    public ResultConfig handleNotLoginException(NotLoginException e) {
        return ResultConfig.error("未登录，请先登录");
    }
    
    /**
     * 处理权限不足异常
     */
    @ExceptionHandler(NotPermissionException.class)
    public ResultConfig handleNotPermissionException(NotPermissionException e) {
        return ResultConfig.error("权限不足，无法访问该资源");
    }
    
    /**
     * 处理角色不足异常
     */
    @ExceptionHandler(NotRoleException.class)
    public ResultConfig handleNotRoleException(NotRoleException e) {
        return ResultConfig.error("角色权限不足，无法访问该资源");
    }
    
    /**
     * 处理其他运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResultConfig handleRuntimeException(RuntimeException e) {
        return ResultConfig.error(e.getMessage());
    }
}
