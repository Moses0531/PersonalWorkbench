package com.moses.rabc.mapper;

import com.moses.rabc.entity.SysPermission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author Administrator
* @description 针对表【sys_permission(权限表)】的数据库操作Mapper
* @createDate 2026-07-12 12:04:43
* @Entity com.moses.rabc.entity.SysPermission
*/
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    List<String> selectPermissionCodesByUserId(@Param("userId") Long userId);

}




