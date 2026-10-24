package com.moses.security;

import com.moses.context.CurrentUser;
import com.moses.context.UserContext;
import com.moses.exception.BusinessException;
import com.moses.exception.ErrorCode;
import com.moses.user.entity.SysUser;
import com.moses.user.mapper.SysUserMapper;
import org.springframework.stereotype.Component;

/**
 * 登录用户必须存在。封禁状态交给请求内的 {@link CurrentUser}，由拦截器统一拒绝。
 */
@Component
public class ActiveUserCheckerImpl implements UserContext.ActiveUserChecker {

    private final SysUserMapper sysUserMapper;

    public ActiveUserCheckerImpl(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
        UserContext.setActiveUserChecker(this);
    }

    @Override
    public CurrentUser load(long userId) {
        SysUser sysUser = sysUserMapper.selectById(userId);
        if (sysUser == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        return new CurrentUser(userId, "1".equals(sysUser.getStatus()));
    }
}
