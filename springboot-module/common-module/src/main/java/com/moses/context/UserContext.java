package com.moses.context;

import cn.dev33.satoken.stp.StpUtil;
import com.moses.exception.BusinessException;
import com.moses.exception.ErrorCode;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * 当前登录用户。同一请求只查一次账号状态，结果放在请求属性里。
 * 系统模块通过 {@link #setActiveUserChecker(ActiveUserChecker)} 接入。
 */
public final class UserContext {

    static final String REQUEST_ATTR = CurrentUser.class.getName();

    private static volatile ActiveUserChecker activeUserChecker;

    private UserContext() {
    }

    public static void setActiveUserChecker(ActiveUserChecker activeUserChecker) {
        UserContext.activeUserChecker = activeUserChecker;
    }

    /**
     * 已登录时解析并缓存当前用户；未登录返回 null，不抛异常。
     */
    public static CurrentUser resolveCurrentRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            Object cached = attrs.getAttribute(REQUEST_ATTR, RequestAttributes.SCOPE_REQUEST);
            if (cached instanceof CurrentUser current) {
                return current;
            }
        }
        if (!StpUtil.isLogin()) {
            return null;
        }
        CurrentUser current = load(StpUtil.getLoginIdAsLong());
        if (attrs != null) {
            attrs.setAttribute(REQUEST_ATTR, current, RequestAttributes.SCOPE_REQUEST);
        }
        return current;
    }

    /**
     * 当前登录用户。未登录或已封禁时抛异常。
     */
    public static CurrentUser currentUser() {
        CurrentUser current = resolveCurrentRequest();
        if (current == null) {
            StpUtil.checkLogin();
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录，请先登录");
        }
        if (current.banned()) {
            throw new BusinessException(ErrorCode.USER_BANNED, "账号已被封禁");
        }
        return current;
    }

    public static long currentUserId() {
        return currentUser().userId();
    }

    private static CurrentUser load(long userId) {
        ActiveUserChecker checker = activeUserChecker;
        if (checker == null) {
            return new CurrentUser(userId, false);
        }
        return checker.load(userId);
    }

    /**
     * 由系统模块实现。用户不存在时抛 {@link ErrorCode#USER_NOT_FOUND}。
     * 封禁只体现在 {@link CurrentUser#banned()}，不要在这里抛封禁异常。
     */
    public interface ActiveUserChecker {

        CurrentUser load(long userId);
    }
}
