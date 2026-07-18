package com.moses.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moses.config.ResultConfig;
import com.moses.entity.WbEvent;
import com.moses.service.WbEventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WbEventControllerTest {

    @Mock
    private WbEventService wbEventService;

    @InjectMocks
    private WbEventController controller;

    @Test
    void addEvent_bindsCurrentUser() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(11L);
            when(wbEventService.save(any(WbEvent.class))).thenReturn(true);

            WbEvent body = new WbEvent();
            body.setEventId(88L);
            body.setTitle("开会");
            ResultConfig result = controller.addEvent(body);

            assertEquals(1, result.getCode());
            assertNull(body.getEventId());
            assertEquals(11L, body.getUserId());
            verify(wbEventService).save(body);
        }
    }

    @Test
    void updateEvent_bindsUserId() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(11L);
            when(wbEventService.update(any(WbEvent.class), any())).thenReturn(true);

            WbEvent body = new WbEvent();
            body.setEventId(2L);
            controller.updateEvent(body);
            assertEquals(11L, body.getUserId());
            verify(wbEventService).update(eq(body), any());
        }
    }

    @Test
    void deleteAndPage_useLoginUser() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(11L);
            when(wbEventService.remove(any())).thenReturn(true);
            when(wbEventService.page(any(Page.class), any())).thenReturn(new Page<>());

            assertEquals(1, controller.deleteBatchEvent(List.of(1L)).getCode());
            assertEquals(1, controller.getEventPage(1, 10).getCode());
            verify(wbEventService).remove(any());
            verify(wbEventService).page(any(Page.class), any());
        }
    }
}
