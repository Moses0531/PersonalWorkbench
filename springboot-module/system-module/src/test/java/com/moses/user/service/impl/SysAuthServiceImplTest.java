package com.moses.user.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import com.moses.rabc.entity.SysPermission;
import com.moses.rabc.mapper.SysPermissionMapper;
import com.moses.user.config.AppAuthProperties;
import com.moses.user.entity.Login;
import com.moses.user.entity.Register;
import com.moses.user.entity.SysUser;
import com.moses.user.service.SysUserService;
import com.moses.utils.CaptchaUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SysAuthServiceImplTest {

    @Mock
    private CaptchaUtil captchaUtil;

    @Mock
    private SysUserService sysUserService;

    @Mock
    private SysPermissionMapper sysPermissionMapper;

    @Mock
    private AppAuthProperties appAuthProperties;

    @InjectMocks
    private SysAuthServiceImpl authService;

    @BeforeEach
    void disableCaptcha() {
        lenient().when(appAuthProperties.isCaptchaOnRegister()).thenReturn(false);
        lenient().when(appAuthProperties.isCaptchaOnLogin()).thenReturn(false);
        lenient().when(appAuthProperties.isRegisterEnabled()).thenReturn(true);
    }

    @Test
    void register_disabled_throws() {
        when(appAuthProperties.isRegisterEnabled()).thenReturn(false);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.register(validRegisterPhone()));
        assertEquals("当前系统未开放注册", ex.getMessage());
    }

    @Test
    void register_bothPhoneAndEmail_throws() {
        Register reg = validRegisterPhone();
        reg.setEmail("a@b.com");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.register(reg));
        assertEquals("请填写手机号或邮箱其中一项", ex.getMessage());
    }

    @Test
    void register_neitherPhoneNorEmail_throws() {
        Register reg = validRegisterPhone();
        reg.setPhone(null);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.register(reg));
        assertEquals("请填写手机号或邮箱其中一项", ex.getMessage());
    }

    @Test
    void register_badPhone_throws() {
        Register reg = validRegisterPhone();
        reg.setPhone("123");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.register(reg));
        assertEquals("手机号格式不正确", ex.getMessage());
    }

    @Test
    void register_passwordMismatch_throws() {
        Register reg = validRegisterPhone();
        reg.setConfirmPassword("other");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.register(reg));
        assertEquals("两次输入的密码不一致", ex.getMessage());
    }

    @Test
    void register_passwordTooShort_throws() {
        Register reg = validRegisterPhone();
        reg.setPassword("123");
        reg.setConfirmPassword("123");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.register(reg));
        assertEquals("密码长度需在6-20位之间", ex.getMessage());
    }

    @Test
    void register_duplicatePhone_throws() {
        when(sysUserService.count(any())).thenReturn(1L);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.register(validRegisterPhone()));
        assertEquals("该手机号已注册", ex.getMessage());
    }

    @Test
    void register_success_byPhone() {
        when(sysUserService.count(any())).thenReturn(0L);
        when(sysUserService.save(any(SysUser.class))).thenAnswer(inv -> {
            SysUser u = inv.getArgument(0);
            u.setUserId(100L);
            return true;
        });

        Register result = authService.register(validRegisterPhone());
        assertEquals(100L, result.getUserId());
        assertNotNull(result.getAccount());
        verify(sysUserService).save(argThat(u ->
                "13800138000".equals(u.getPhone())
                        && u.getEmail() == null
                        && "0".equals(u.getStatus())
                        && Long.valueOf(2L).equals(u.getRoleId())
        ));
    }

    @Test
    void register_success_byEmail() {
        when(sysUserService.count(any())).thenReturn(0L);
        when(sysUserService.save(any(SysUser.class))).thenAnswer(inv -> {
            SysUser u = inv.getArgument(0);
            u.setUserId(101L);
            return true;
        });

        Register reg = new Register();
        reg.setEmail("user@example.com");
        reg.setPassword("abcdef");
        reg.setConfirmPassword("abcdef");
        Register result = authService.register(reg);
        assertEquals(101L, result.getUserId());
        verify(sysUserService).save(argThat(u -> "user@example.com".equals(u.getEmail())));
    }

    @Test
    void login_missingCredentials_throws() {
        Login login = new Login();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(login));
        assertEquals("账号和密码不能为空", ex.getMessage());
    }

    @Test
    void login_userNotFound_throws() {
        when(sysUserService.getOne(any())).thenReturn(null);
        Login login = new Login();
        login.setAccount("nouser");
        login.setPassword("abcdef");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(login));
        assertEquals("账号不存在", ex.getMessage());
    }

    @Test
    void login_banned_throws() {
        SysUser user = baseUser();
        user.setStatus("1");
        when(sysUserService.getOne(any())).thenReturn(user);
        Login login = new Login();
        login.setAccount("demo");
        login.setPassword("abcdef");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(login));
        assertEquals("账号已被封禁", ex.getMessage());
    }

    @Test
    void login_wrongPassword_throws() {
        SysUser user = baseUser();
        when(sysUserService.getOne(any())).thenReturn(user);
        Login login = new Login();
        login.setAccount("demo");
        login.setPassword("wrongpw");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(login));
        assertEquals("密码错误", ex.getMessage());
    }

    @Test
    void login_success() {
        SysUser user = baseUser();
        when(sysUserService.getOne(any())).thenReturn(user);
        when(sysUserService.updateById(any(SysUser.class))).thenReturn(true);
        when(sysPermissionMapper.selectMenusByUserId(10L)).thenReturn(new ArrayList<>());

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(() -> StpUtil.login(10L)).thenAnswer(inv -> null);
            stp.when(StpUtil::getTokenValue).thenReturn("token-xyz");

            Login login = new Login();
            login.setAccount("demo");
            login.setPassword("abcdef");
            Login result = authService.login(login);

            assertEquals("token-xyz", result.getToken());
            assertEquals(10L, result.getUserId());
            assertEquals("demo", result.getAccount());
            assertNotNull(result.getMenuList());
            stp.verify(() -> StpUtil.login(10L));
            verify(sysUserService).updateById(any(SysUser.class));
        }
    }

    private static Register validRegisterPhone() {
        Register reg = new Register();
        reg.setPhone("13800138000");
        reg.setPassword("abcdef");
        reg.setConfirmPassword("abcdef");
        return reg;
    }

    private static SysUser baseUser() {
        SysUser user = new SysUser();
        user.setUserId(10L);
        user.setAccount("demo");
        user.setUsername("演示");
        user.setStatus("0");
        user.setPassword(BCrypt.hashpw("abcdef", BCrypt.gensalt()));
        return user;
    }
}
