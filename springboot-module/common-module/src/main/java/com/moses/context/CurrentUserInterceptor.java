package com.moses.context;

import com.moses.exception.BusinessException;
import com.moses.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 在进入 Controller 前解析一次当前用户。已封禁的登录态在这里被拒绝。
 */
public class CurrentUserInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        CurrentUser current = UserContext.resolveCurrentRequest();
        if (current != null && current.banned()) {
            throw new BusinessException(ErrorCode.USER_BANNED, "账号已被封禁");
        }
        return true;
    }
}
