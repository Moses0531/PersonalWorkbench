package com.moses.rabc.mapper;

import com.moses.rabc.entity.SysRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
* @author Administrator
* @description 针对表【sys_role(系统角色表)】的数据库操作Mapper
* @createDate 2026-07-12 12:04:43
* @Entity com.moses.rabc.entity.SysRole
*/
public interface SysRoleMapper extends BaseMapper<SysRole> {

    String selectRoleCodeByUserId(@Param("userId") Long userId);

}




