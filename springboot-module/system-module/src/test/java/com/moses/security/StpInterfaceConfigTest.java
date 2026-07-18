package com.moses.security;

import com.moses.rabc.mapper.SysPermissionMapper;
import com.moses.rabc.mapper.SysRoleMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StpInterfaceConfigTest {

    @Mock
    private SysPermissionMapper sysPermissionMapper;

    @Mock
    private SysRoleMapper sysRoleMapper;

    @InjectMocks
    private StpInterfaceConfig stpInterfaceConfig;

    @Test
    void getPermissionList_null_returnsEmpty() {
        when(sysPermissionMapper.selectPermissionCodesByUserId(1L)).thenReturn(null);
        assertTrue(stpInterfaceConfig.getPermissionList(1L, "login").isEmpty());
    }

    @Test
    void getPermissionList_returnsCodes() {
        when(sysPermissionMapper.selectPermissionCodesByUserId(1L))
                .thenReturn(List.of("task:query", "project:add"));
        List<String> codes = stpInterfaceConfig.getPermissionList("1", "login");
        assertEquals(List.of("task:query", "project:add"), codes);
    }

    @Test
    void getRoleList_blank_returnsEmpty() {
        when(sysRoleMapper.selectRoleCodeByUserId(1L)).thenReturn("  ");
        assertTrue(stpInterfaceConfig.getRoleList(1L, "login").isEmpty());
    }

    @Test
    void getRoleList_returnsSingleRole() {
        when(sysRoleMapper.selectRoleCodeByUserId(1L)).thenReturn("user");
        assertEquals(List.of("user"), stpInterfaceConfig.getRoleList(1L, "login"));
    }
}
