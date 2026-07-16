package com.moses.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moses.entity.WbEvent;
import com.moses.service.WbEventService;
import com.moses.mapper.WbEventMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【wb_event(工作台-日程表)】的数据库操作Service实现
* @createDate 2026-07-16 22:59:29
*/
@Service
public class WbEventServiceImpl extends ServiceImpl<WbEventMapper, WbEvent>
    implements WbEventService{

}




