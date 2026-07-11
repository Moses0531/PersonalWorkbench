package com.moses.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moses.auth.entity.Login;
import com.moses.auth.entity.Register;
import com.moses.auth.entity.SysAuth;
import com.moses.auth.mapper.SysAuthMapper;
import com.moses.auth.service.SysAuthService;
import com.moses.config.ResultConfig;
import com.moses.user.entity.SysUser;
import com.moses.user.service.SysUserService;
import com.moses.utils.CaptchaUtil;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

@Service
public class SysAuthServiceImpl extends ServiceImpl<SysAuthMapper, SysAuth>
        implements SysAuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.-]+@[\\w.-]+\\.\\w+$");
    private static final long DEFAULT_ROLE_ID = 2L;

    private final CaptchaUtil captchaUtil;
    private final SysUserService sysUserService;

    public SysAuthServiceImpl(CaptchaUtil captchaUtil, SysUserService sysUserService) {
        this.captchaUtil = captchaUtil;
        this.sysUserService = sysUserService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResultConfig register(Register register) {
        if (register == null) {
            return ResultConfig.error("注册信息不能为空");
        }

        String phone = register.getPhone();
        String email = register.getEmail();
        String password = register.getPassword();
        String confirmPassword = register.getConfirmPassword();

        if (!captchaUtil.verifyCaptcha(register.getCaptchaCode(), register.getCaptchaToken(), register.getCaptchaTimestamp())) {
            return ResultConfig.error("验证码错误或已过期");
        }

        boolean hasPhone = StringUtils.hasText(phone);
        boolean hasEmail = StringUtils.hasText(email);
        if (hasPhone == hasEmail) {
            return ResultConfig.error("请填写手机号或邮箱其中一项");
        }
        if (hasEmail && !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            return ResultConfig.error("邮箱格式不正确");
        }
        if (!StringUtils.hasText(password) || !StringUtils.hasText(confirmPassword)) {
            return ResultConfig.error("密码不能为空");
        }
        if (!password.equals(confirmPassword)) {
            return ResultConfig.error("两次输入的密码不一致");
        }
        if (password.length() < 6 || password.length() > 20) {
            return ResultConfig.error("密码长度需在6-20位之间");
        }

        phone = hasPhone ? phone.trim() : null;
        email = hasEmail ? email.trim() : null;

        if (hasPhone && sysUserService.count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhone, phone)) > 0) {
            return ResultConfig.error("该手机号已注册");
        }
        if (hasEmail && sysUserService.count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, email)) > 0) {
            return ResultConfig.error("该邮箱已注册");
        }

        String account = generateUniqueAccount();
        Date now = new Date();

        SysAuth sysAuth = new SysAuth();
        sysAuth.setAccount(account);
        sysAuth.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        sysAuth.setStatus("0");
        sysAuth.setCreateTime(now);
        sysAuth.setUpdateTime(now);
        save(sysAuth);

        SysUser sysUser = new SysUser();
        sysUser.setAccountId(sysAuth.getAccountId());
        sysUser.setRoleId(DEFAULT_ROLE_ID);
        sysUser.setUsername("用户" + account);
        sysUser.setPhone(phone != null ? phone : "");
        sysUser.setEmail(email != null ? email : "");
        sysUser.setCreateTime(now);
        sysUser.setUpdateTime(now);
        sysUserService.save(sysUser);

        Map<String, Object> data = new HashMap<>();
        data.put("account", account);
        data.put("userId", sysUser.getUserId());
        return ResultConfig.success(data);
    }

    private String generateUniqueAccount() {
        for (int i = 0; i < 20; i++) {
            String account = String.valueOf(ThreadLocalRandom.current().nextInt(10000000, 100000000));
            long count = count(new LambdaQueryWrapper<SysAuth>().eq(SysAuth::getAccount, account));
            if (count == 0) {
                return account;
            }
        }
        throw new RuntimeException("账号生成失败，请稍后重试");
    }

    @Override
    public ResultConfig login(Login login) {
        if (login == null) {
            return ResultConfig.error("登录信息不能为空");
        }

        if (!captchaUtil.verifyCaptcha(login.getCaptchaCode(), login.getCaptchaToken(), login.getCaptchaTimestamp())) {
            return ResultConfig.error("验证码错误或已过期");
        }

        String account = login.getAccount();
        String password = login.getPassword();
        if (!StringUtils.hasText(account) || !StringUtils.hasText(password)) {
            return ResultConfig.error("账号和密码不能为空");
        }

        account = account.trim();
        SysAuth sysAuth = findAuthByAccount(account);
        if (sysAuth == null) {
            return ResultConfig.error("账号不存在");
        }
        if ("1".equals(sysAuth.getStatus())) {
            return ResultConfig.error("账号已被封禁");
        }
        if (!BCrypt.checkpw(password, sysAuth.getPassword())) {
            return ResultConfig.error("密码错误");
        }

        StpUtil.login(sysAuth.getAccountId());

        Date now = new Date();
        sysAuth.setLastLoginTime(now);
        sysAuth.setUpdateTime(now);
        updateById(sysAuth);

        SysUser sysUser = sysUserService.getOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getAccountId, sysAuth.getAccountId()));

        Map<String, Object> data = new HashMap<>();
        data.put("token", StpUtil.getTokenValue());
        data.put("account", sysAuth.getAccount());
        if (sysUser != null) {
            data.put("userId", sysUser.getUserId());
            data.put("username", sysUser.getUsername());
        }
        return ResultConfig.success(data);
    }

    private SysAuth findAuthByAccount(String identifier) {
        SysAuth sysAuth = getOne(new LambdaQueryWrapper<SysAuth>().eq(SysAuth::getAccount, identifier));
        if (sysAuth != null) {
            return sysAuth;
        }

        SysUser sysUser = sysUserService.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getPhone, identifier)
                .or()
                .eq(SysUser::getEmail, identifier));
        if (sysUser == null) {
            return null;
        }
        return getById(sysUser.getAccountId());
    }
}
