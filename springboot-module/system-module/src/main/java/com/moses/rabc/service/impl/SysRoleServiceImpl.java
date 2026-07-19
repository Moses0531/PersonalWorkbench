package com.moses.rabc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moses.rabc.entity.SysRole;
import com.moses.rabc.mapper.SysRoleMapper;
import com.moses.rabc.service.SysRoleService;
import org.springframework.stereotype.Service;

import java.io.Serializable;

/**
 * @author Administrator
 * @description 针对表【sys_role(系统角色表)】的数据库操作Service实现
 * @createDate 2026-07-12 12:04:43
 */
@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole>
        implements SysRoleService {

    /**
     * 主键为 0 时部分 MyBatis-Plus 路径不可靠，统一按条件查询。
     */
    @Override
    public SysRole getById(Serializable id) {
        if (id == null) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleId, id), false);
    }
}
