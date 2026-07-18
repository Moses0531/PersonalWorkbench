package com.moses.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moses.config.ResultConfig;
import com.moses.entity.WbProject;
import com.moses.service.WbProjectService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WbProjectControllerTest {

    @Mock
    private WbProjectService wbProjectService;

    @InjectMocks
    private WbProjectController controller;

    @Test
    void addProject_bindsCurrentUser() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(7L);
            when(wbProjectService.save(any(WbProject.class))).thenReturn(true);

            WbProject body = new WbProject();
            body.setProjectId(999L);
            body.setName("P1");
            ResultConfig result = controller.addProject(body);

            assertEquals(1, result.getCode());
            assertNull(body.getProjectId());
            assertEquals(7L, body.getUserId());
            verify(wbProjectService).save(body);
        }
    }

    @Test
    void updateProject_scopesByUserId() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(7L);
            when(wbProjectService.update(any(WbProject.class), any())).thenReturn(true);

            WbProject body = new WbProject();
            body.setProjectId(3L);
            body.setName("P2");
            controller.updateProject(body);

            assertEquals(7L, body.getUserId());
            @SuppressWarnings("unchecked")
            ArgumentCaptor<LambdaQueryWrapper<WbProject>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
            verify(wbProjectService).update(eq(body), captor.capture());
            assertNotNull(captor.getValue());
        }
    }

    @Test
    void deleteBatch_scopesByUserId() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(7L);
            when(wbProjectService.remove(any())).thenReturn(true);
            controller.deleteBatchProject(List.of(1L, 2L));
            verify(wbProjectService).remove(any());
        }
    }

    @Test
    void getProjectPage_usesLoginUser() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(7L);
            when(wbProjectService.page(any(Page.class), any())).thenReturn(new Page<>());
            ResultConfig result = controller.getProjectPage(1, 20);
            assertEquals(1, result.getCode());
            verify(wbProjectService).page(any(Page.class), any());
        }
    }
}
