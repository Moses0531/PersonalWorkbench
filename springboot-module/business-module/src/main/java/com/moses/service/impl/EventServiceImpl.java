package com.moses.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moses.entity.Event;
import com.moses.service.EventService;
import com.moses.mapper.EventMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【event(工作台-日程表)】的数据库操作Service实现
* @createDate 2026-07-16 22:59:29
*/
@Service
public class EventServiceImpl extends ServiceImpl<EventMapper, Event>
    implements EventService{

}




