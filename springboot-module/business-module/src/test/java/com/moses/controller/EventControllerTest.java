package com.moses.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moses.config.ResultConfig;
import com.moses.entity.Event;
import com.moses.service.EventService;
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
class EventControllerTest {

    @Mock
    private EventService eventService;

    @InjectMocks
    private EventController controller;

    @Test
    void addEvent_bindsCurrentUser() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(11L);
            when(eventService.save(any(Event.class))).thenReturn(true);

            Event body = new Event();
            body.setEventId(88L);
            body.setTitle("开会");
            ResultConfig result = controller.addEvent(body);

            assertEquals(1, result.getCode());
            assertNull(body.getEventId());
            assertEquals(11L, body.getUserId());
            verify(eventService).save(body);
        }
    }

    @Test
    void updateEvent_bindsUserId() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(11L);
            when(eventService.update(any(Event.class), any())).thenReturn(true);

            Event body = new Event();
            body.setEventId(2L);
            controller.updateEvent(body);
            assertEquals(11L, body.getUserId());
            verify(eventService).update(eq(body), any());
        }
    }

    @Test
    void deleteAndPage_useLoginUser() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(11L);
            when(eventService.remove(any())).thenReturn(true);
            when(eventService.page(any(Page.class), any())).thenReturn(new Page<>());

            assertEquals(1, controller.deleteBatchEvent(List.of(1L)).getCode());
            assertEquals(1, controller.getEventPage(1, 10).getCode());
            verify(eventService).remove(any());
            verify(eventService).page(any(Page.class), any());
        }
    }
}
