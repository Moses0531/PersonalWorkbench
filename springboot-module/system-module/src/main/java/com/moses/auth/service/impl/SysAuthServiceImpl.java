package com.moses.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moses.auth.entity.Login;
import com.moses.auth.entity.Register;
import com.moses.auth.entity.SysAuth;
import com.moses.auth.mapper.SysAuthMapper;
import com.moses.auth.service.SysAuthService;
import com.moses.user.entity.SysUser;
import com.moses.user.service.SysUserService;
import com.moses.utils.CaptchaUtil;
import com.moses.utils.FormatUtil;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class SysAuthServiceImpl extends ServiceImpl<SysAuthMapper, SysAuth>
        implements SysAuthService {

    private static final long DEFAULT_ROLE_ID = 2L;

    private final CaptchaUtil captchaUtil;
    private final SysUserService sysUserService;

    public SysAuthServiceImpl(CaptchaUtil captchaUtil, SysUserService sysUserService) {
        this.captchaUtil = captchaUtil;
        this.sysUserService = sysUserService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Register register(Register register) {
        if (register == null) {
            throw new RuntimeException("注册信息不能为空");
        }

        String phone = register.getPhone();
        String email = register.getEmail();
        String password = register.getPassword();
        String confirmPassword = register.getConfirmPassword();

        if (!captchaUtil.verifyCaptcha(register.getCaptchaCode(), register.getCaptchaToken(), register.getCaptchaTimestamp())) {
            throw new RuntimeException("验证码错误或已过期");
        }

        boolean hasPhone = StringUtils.hasText(phone);
        boolean hasEmail = StringUtils.hasText(email);
        if (hasPhone == hasEmail) {
            throw new RuntimeException("请填写手机号或邮箱其中一项");
        }
        if (hasPhone && !FormatUtil.isValidPhone(phone)) {
            throw new RuntimeException("手机号格式不正确");
        }
        if (hasEmail && !FormatUtil.isValidEmail(email)) {
            throw new RuntimeException("邮箱格式不正确");
        }
        if (!StringUtils.hasText(password) || !StringUtils.hasText(confirmPassword)) {
            throw new RuntimeException("密码不能为空");
        }
        if (!password.equals(confirmPassword)) {
            throw new RuntimeException("两次输入的密码不一致");
        }
        if (password.length() < 6 || password.length() > 20) {
            throw new RuntimeException("密码长度需在6-20位之间");
        }

        phone = hasPhone ? phone.trim() : null;
        email = hasEmail ? email.trim() : null;

        if (hasPhone && sysUserService.count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhone, phone)) > 0) {
            throw new RuntimeException("该手机号已注册");
        }
        if (hasEmail && sysUserService.count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, email)) > 0) {
            throw new RuntimeException("该邮箱已注册");
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

        Register result = new Register();
        result.setAccount(account);
        result.setUserId(sysUser.getUserId());
        return result;
    }

    @Override
    public Login login(Login login) {
        if (login == null) {
            throw new RuntimeException("登录信息不能为空");
        }

        if (!captchaUtil.verifyCaptcha(login.getCaptchaCode(), login.getCaptchaToken(), login.getCaptchaTimestamp())) {
            throw new RuntimeException("验证码错误或已过期");
        }

        String account = login.getAccount();
        String password = login.getPassword();
        if (!StringUtils.hasText(account) || !StringUtils.hasText(password)) {
            throw new RuntimeException("账号和密码不能为空");
        }

        account = account.trim();
        validateLoginAccount(account);
        SysAuth sysAuth = findAuthByAccount(account);
        if (sysAuth == null) {
            throw new RuntimeException("账号不存在");
        }
        if ("1".equals(sysAuth.getStatus())) {
            throw new RuntimeException("账号已被封禁");
        }
        if (!BCrypt.checkpw(password, sysAuth.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        StpUtil.login(sysAuth.getAccountId());

        Date now = new Date();
        sysAuth.setLastLoginTime(now);
        sysAuth.setUpdateTime(now);
        updateById(sysAuth);

        SysUser sysUser = sysUserService.getOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getAccountId, sysAuth.getAccountId()));

        Login result = new Login();
        result.setToken(StpUtil.getTokenValue());
        result.setAccount(sysAuth.getAccount());
        if (sysUser != null) {
            result.setUserId(sysUser.getUserId());
            result.setUsername(sysUser.getUsername());
        }
        return result;
    }

    private void validateLoginAccount(String account) {
        if (account.contains("@")) {
            if (!FormatUtil.isValidEmail(account)) {
                throw new RuntimeException("邮箱格式不正确");
            }
            return;
        }
        if (account.matches("^\\d{11}$") && !FormatUtil.isValidPhone(account)) {
            throw new RuntimeException("手机号格式不正确");
        }
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
