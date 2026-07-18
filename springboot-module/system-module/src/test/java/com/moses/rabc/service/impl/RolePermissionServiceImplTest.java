package com.moses.rabc.service.impl;

import com.moses.rabc.entity.RolePermission;
import com.moses.rabc.entity.SysPermission;
import com.moses.rabc.entity.SysRole;
import com.moses.rabc.mapper.RolePermissionMapper;
import com.moses.rabc.service.SysPermissionService;
import com.moses.rabc.service.SysRoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RolePermissionServiceImplTest {

    @Mock
    private SysRoleService sysRoleService;

    @Mock
    private SysPermissionService sysPermissionService;

    @Mock
    private RolePermissionMapper rolePermissionMapper;

    @Spy
    @InjectMocks
    private RolePermissionServiceImpl service;

    @BeforeEach
    void injectBaseMapper() {
        ReflectionTestUtils.setField(service, "baseMapper", rolePermissionMapper);
    }

    @Test
    void getPermissionsByRoleId_roleMissing_throws() {
        when(sysRoleService.getById(9L)).thenReturn(null);
        when(sysRoleService.getOne(any(), eq(false))).thenReturn(null);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.getPermissionsByRoleId(9L));
        assertEquals("角色不存在", ex.getMessage());
    }

    @Test
    void getPermissionsByRoleId_success() {
        SysRole role = new SysRole();
        role.setRoleId(2L);
        when(sysRoleService.getById(2L)).thenReturn(role);
        when(rolePermissionMapper.selectPermissionsByRoleId(2L)).thenReturn(List.of(new SysPermission()));

        List<SysPermission> list = service.getPermissionsByRoleId(2L);
        assertEquals(1, list.size());
    }

    @Test
    void addRolePermission_nullIds_throws() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.addRolePermission(new RolePermission()));
        assertEquals("角色ID和权限ID不能为空", ex.getMessage());
    }

    @Test
    void addRolePermission_permissionMissing_throws() {
        SysRole role = new SysRole();
        role.setRoleId(2L);
        when(sysRoleService.getById(2L)).thenReturn(role);
        when(sysPermissionService.getById(8L)).thenReturn(null);

        RolePermission rp = new RolePermission();
        rp.setRoleId(2L);
        rp.setPermissionId(8L);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.addRolePermission(rp));
        assertEquals("权限不存在", ex.getMessage());
    }

    @Test
    void addRolePermission_alreadyLinked_throws() {
        SysRole role = new SysRole();
        role.setRoleId(2L);
        when(sysRoleService.getById(2L)).thenReturn(role);
        when(sysPermissionService.getById(8L)).thenReturn(new SysPermission());
        doReturn(1L).when(service).count(any());

        RolePermission rp = new RolePermission();
        rp.setRoleId(2L);
        rp.setPermissionId(8L);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.addRolePermission(rp));
        assertEquals("该角色已拥有此权限", ex.getMessage());
    }

    @Test
    void addRolePermission_success() {
        SysRole role = new SysRole();
        role.setRoleId(2L);
        when(sysRoleService.getById(2L)).thenReturn(role);
        when(sysPermissionService.getById(8L)).thenReturn(new SysPermission());
        doReturn(0L).when(service).count(any());
        doReturn(true).when(service).save(any(RolePermission.class));

        RolePermission rp = new RolePermission();
        rp.setRoleId(2L);
        rp.setPermissionId(8L);
        assertDoesNotThrow(() -> service.addRolePermission(rp));
        verify(service).save(rp);
    }

    @Test
    void removeRolePermission_null_throws() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.removeRolePermission(null, 1L));
        assertEquals("角色ID和权限ID不能为空", ex.getMessage());
    }

    @Test
    void removeRolePermission_notLinked_throws() {
        when(rolePermissionMapper.deleteByRoleIdAndPermissionId(2L, 8L)).thenReturn(0);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.removeRolePermission(2L, 8L));
        assertEquals("角色权限关联不存在", ex.getMessage());
    }

    @Test
    void removeRolePermission_success() {
        when(rolePermissionMapper.deleteByRoleIdAndPermissionId(2L, 8L)).thenReturn(1);
        assertDoesNotThrow(() -> service.removeRolePermission(2L, 8L));
    }
}
