package com.moses.auth.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moses.auth.entity.Login;
import com.moses.auth.entity.Register;
import com.moses.auth.service.SysAuthService;
import com.moses.user.entity.SysUser;
import com.moses.user.service.SysUserService;
import com.moses.utils.CaptchaUtil;
import com.moses.utils.FormatUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class SysAuthServiceImpl implements SysAuthService {

    @Autowired
    private  CaptchaUtil captchaUtil;

    @Autowired
    private  SysUserService sysUserService;


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

        SysUser sysUser = new SysUser();
        sysUser.setAccount(account);
        sysUser.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        sysUser.setStatus("0");
        sysUser.setRoleId(2L);
        sysUser.setUsername("用户" + account);
        sysUser.setPhone(phone);
        sysUser.setEmail(email);
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

        SysUser sysUser = findUserByIdentifier(account);
        if (sysUser == null) {
            throw new RuntimeException("账号不存在");
        }
        if ("1".equals(sysUser.getStatus())) {
            throw new RuntimeException("账号已被封禁");
        }
        if (!BCrypt.checkpw(password, sysUser.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        StpUtil.login(sysUser.getUserId());

        Date now = new Date();
        sysUser.setLastLoginTime(now);
        sysUser.setUpdateTime(now);
        sysUserService.updateById(sysUser);

        Login result = new Login();
        result.setToken(StpUtil.getTokenValue());
        result.setAccount(sysUser.getAccount());
        result.setUserId(sysUser.getUserId());
        result.setUsername(sysUser.getUsername());
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
            long count = sysUserService.count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getAccount, account));
            if (count == 0) {
                return account;
            }
        }
        throw new RuntimeException("账号生成失败，请稍后重试");
    }

    private SysUser findUserByIdentifier(String identifier) {
        SysUser sysUser = sysUserService.getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getAccount, identifier));
        if (sysUser != null) {
            return sysUser;
        }
        return sysUserService.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getPhone, identifier)
                .or()
                .eq(SysUser::getEmail, identifier));
    }
}
