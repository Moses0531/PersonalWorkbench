package com.moses.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moses.user.entity.SysUser;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author Administrator
 * @description 针对表【sys_user(系统用户表)】的数据库操作Service
 * @createDate 2026-07-12 22:01:57
 */
public interface SysUserService extends IService<SysUser> {

    SysUser getCurrentUserInfo(Long userId);

    void updateUserInfo(Long userId, SysUser userProfile);

    void changePassword(Long userId, String oldPassword, String newPassword);

    String updateAvatar(Long userId, MultipartFile file);

    void deleteAvatar(Long userId);

    void createManagedUser(SysUser user);

    void updateManagedUser(SysUser user);
}
