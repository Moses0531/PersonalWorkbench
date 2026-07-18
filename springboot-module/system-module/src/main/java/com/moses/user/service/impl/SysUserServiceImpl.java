package com.moses.user.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moses.config.ResultConfig;
import com.moses.user.entity.SysUser;
import com.moses.user.mapper.SysUserMapper;
import com.moses.user.service.SysUserService;
import com.moses.utils.FormatUtil;
import com.moses.utils.UploadFileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.Set;

/**
 * @author Administrator
 * @description 针对表【sys_user(系统用户表)】的数据库操作Service实现
 * @createDate 2026-07-12 22:01:57
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser>
        implements SysUserService {

    private static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_AVATAR_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    @Autowired
    private UploadFileUtil uploadFileUtil;

    @Override
    public SysUser getCurrentUserInfo(Long userId) {
        SysUser sysUser = getById(userId);
        if (sysUser == null) {
            throw new RuntimeException("用户不存在");
        }
        sysUser.setPassword(null);
        return sysUser;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserInfo(Long userId, SysUser userProfile) {
        if (userProfile == null) {
            throw new RuntimeException("个人信息不能为空");
        }

        SysUser existingUser = getById(userId);
        if (existingUser == null) {
            throw new RuntimeException("用户不存在");
        }

        String phone = userProfile.getPhone();
        if (StringUtils.hasText(phone)) {
            phone = phone.trim();
            if (!FormatUtil.isValidPhone(phone)) {
                throw new RuntimeException("手机号格式不正确");
            }
            if (count(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getPhone, phone)
                    .ne(SysUser::getUserId, userId)) > 0) {
                throw new RuntimeException("该手机号已被使用");
            }
            existingUser.setPhone(phone);
        }

        String email = userProfile.getEmail();
        if (StringUtils.hasText(email)) {
            email = email.trim();
            if (!FormatUtil.isValidEmail(email)) {
                throw new RuntimeException("邮箱格式不正确");
            }
            if (count(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getEmail, email)
                    .ne(SysUser::getUserId, userId)) > 0) {
                throw new RuntimeException("该邮箱已被使用");
            }
            existingUser.setEmail(email);
        }

        if (StringUtils.hasText(userProfile.getUsername())) {
            existingUser.setUsername(userProfile.getUsername().trim());
        }
        if (userProfile.getRealName() != null) {
            existingUser.setRealName(userProfile.getRealName().trim());
        }
        if (userProfile.getSex() != null) {
            existingUser.setSex(userProfile.getSex());
        }
        if (userProfile.getBirthday() != null) {
            existingUser.setBirthday(userProfile.getBirthday());
        }

        existingUser.setUpdateTime(new Date());
        updateById(existingUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        if (!StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword)) {
            throw new RuntimeException("旧密码和新密码不能为空");
        }
        if (newPassword.length() < 6 || newPassword.length() > 20) {
            throw new RuntimeException("新密码长度需在6-20位之间");
        }
        if (oldPassword.equals(newPassword)) {
            throw new RuntimeException("新密码不能与旧密码相同");
        }

        SysUser sysUser = getById(userId);
        if (sysUser == null) {
            throw new RuntimeException("用户不存在");
        }
        if (!BCrypt.checkpw(oldPassword, sysUser.getPassword())) {
            throw new RuntimeException("旧密码错误");
        }

        sysUser.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        sysUser.setUpdateTime(new Date());
        updateById(sysUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String updateAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("头像文件不能为空");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new RuntimeException("头像文件大小不能超过5MB");
        }

        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !ALLOWED_AVATAR_TYPES.contains(contentType)) {
            throw new RuntimeException("仅支持jpg、png、gif、webp格式的图片");
        }

        SysUser sysUser = getById(userId);
        if (sysUser == null) {
            throw new RuntimeException("用户不存在");
        }

        try {
            ResultConfig uploadResult = uploadFileUtil.uploadToAliYun(file);
            if (uploadResult.getCode() == null || uploadResult.getCode() != 1) {
                throw new RuntimeException(uploadResult.getMsg() != null ? uploadResult.getMsg() : "头像上传失败");
            }
            String avatarUrl = (String) uploadResult.getData();
            sysUser.setAvatar(avatarUrl);
            sysUser.setUpdateTime(new Date());
            updateById(sysUser);
            return avatarUrl;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("头像上传失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createManagedUser(SysUser user) {
        if (user == null) {
            throw new RuntimeException("用户信息不能为空");
        }
        String account = user.getAccount();
        if (!StringUtils.hasText(account)) {
            throw new RuntimeException("登录账号不能为空");
        }
        account = account.trim();
        if (count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getAccount, account)) > 0) {
            throw new RuntimeException("该账号已存在");
        }

        String password = user.getPassword();
        if (!StringUtils.hasText(password)) {
            throw new RuntimeException("密码不能为空");
        }
        if (password.length() < 6 || password.length() > 20) {
            throw new RuntimeException("密码长度需在6-20位之间");
        }

        Date now = new Date();
        user.setAccount(account);
        user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        if (!StringUtils.hasText(user.getStatus())) {
            user.setStatus("0");
        }
        user.setCreateTime(now);
        user.setUpdateTime(now);
        save(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateManagedUser(SysUser user) {
        if (user == null || user.getUserId() == null) {
            throw new RuntimeException("用户ID不能为空");
        }

        SysUser existingUser = getById(user.getUserId());
        if (existingUser == null) {
            throw new RuntimeException("用户不存在");
        }

        if (StringUtils.hasText(user.getUsername())) {
            existingUser.setUsername(user.getUsername().trim());
        }
        if (user.getRealName() != null) {
            existingUser.setRealName(user.getRealName().trim());
        }
        if (user.getPhone() != null) {
            existingUser.setPhone(StringUtils.hasText(user.getPhone()) ? user.getPhone().trim() : null);
        }
        if (user.getEmail() != null) {
            existingUser.setEmail(StringUtils.hasText(user.getEmail()) ? user.getEmail().trim() : null);
        }
        if (user.getSex() != null) {
            existingUser.setSex(user.getSex());
        }
        if (user.getRoleId() != null) {
            existingUser.setRoleId(user.getRoleId());
        }

        String password = user.getPassword();
        if (StringUtils.hasText(password)) {
            if (password.length() < 6 || password.length() > 20) {
                throw new RuntimeException("密码长度需在6-20位之间");
            }
            existingUser.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        }

        existingUser.setUpdateTime(new Date());
        updateById(existingUser);
    }
}
