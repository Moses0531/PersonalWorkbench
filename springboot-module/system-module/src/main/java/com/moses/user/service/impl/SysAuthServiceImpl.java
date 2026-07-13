package com.moses.user.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moses.rabc.entity.SysPermission;
import com.moses.rabc.mapper.SysPermissionMapper;
import com.moses.user.entity.Login;
import com.moses.user.entity.Register;
import com.moses.user.service.SysAuthService;
import com.moses.user.entity.SysUser;
import com.moses.user.service.SysUserService;
import com.moses.utils.CaptchaUtil;
import com.moses.utils.FormatUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class SysAuthServiceImpl implements SysAuthService {

    @Autowired
    private  CaptchaUtil captchaUtil;

    @Autowired
    private  SysUserService sysUserService;

    @Autowired
    private SysPermissionMapper sysPermissionMapper;


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
        result.setAvatar(sysUser.getAvatar());
        result.setMenuList(loadMenusForUser(sysUser.getUserId()));
        return result;
    }

    @Override
    public List<SysPermission> getCurrentUserMenus() {
        return loadMenusForUser(StpUtil.getLoginIdAsLong());
    }

    private List<SysPermission> loadMenusForUser(Long userId) {
        List<SysPermission> menuList = sysPermissionMapper.selectMenusByUserId(userId);
        return fillParentMenus(menuList != null ? menuList : new ArrayList<>());
    }

    /**
     * 向上递归补全父目录节点，避免只分配叶子菜单时导航树断裂。
     */
    private List<SysPermission> fillParentMenus(List<SysPermission> assigned) {
        if (assigned.isEmpty()) {
            return assigned;
        }
        Map<Long, SysPermission> merged = new LinkedHashMap<>();
        for (SysPermission permission : assigned) {
            if (permission.getPermissionId() != null) {
                merged.put(permission.getPermissionId(), permission);
            }
        }
        Set<Long> pendingParentIds = new HashSet<>();
        for (SysPermission permission : assigned) {
            collectMissingParentIds(permission.getParentId(), merged, pendingParentIds);
        }
        while (!pendingParentIds.isEmpty()) {
            List<Long> batch = new ArrayList<>(pendingParentIds);
            pendingParentIds.clear();
            List<SysPermission> parents = sysPermissionMapper.selectBatchIds(batch);
            if (parents == null || parents.isEmpty()) {
                break;
            }
            for (SysPermission parent : parents) {
                if (parent.getPermissionId() == null || merged.containsKey(parent.getPermissionId())) {
                    continue;
                }
                if (!"0".equals(parent.getStatus())) {
                    continue;
                }
                merged.put(parent.getPermissionId(), parent);
                collectMissingParentIds(parent.getParentId(), merged, pendingParentIds);
            }
        }
        return new ArrayList<>(merged.values());
    }

    private void collectMissingParentIds(Long parentId, Map<Long, SysPermission> merged, Set<Long> pending) {
        if (parentId == null || parentId <= 0 || merged.containsKey(parentId)) {
            return;
        }
        pending.add(parentId);
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
