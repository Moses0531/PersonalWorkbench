package com.moses.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moses.entity.WbTask;
import com.moses.service.WbTaskService;
import com.moses.mapper.WbTaskMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【wb_task(工作台-任务表（独立任务或项目子任务）)】的数据库操作Service实现
* @createDate 2026-07-16 22:59:29
*/
@Service
public class WbTaskServiceImpl extends ServiceImpl<WbTaskMapper, WbTask>
    implements WbTaskService{

}




