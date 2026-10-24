package com.moses.controller;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.moses.config.ResultConfig;
import com.moses.context.UserContext;

import java.io.Serializable;
import java.util.Collection;

/**
 * 通用 CRUD 辅助基类。具体 Controller 自己声明路径和权限，再调用这里的方法。
 * <p>
 * 这里不做用户归属过滤。只能操作自己数据的资源，把条件写在
 * {@link com.moses.service.CurrentUserService}，不要在 Controller 里补 userId。
 */
public abstract class BasicController<T, S extends IService<T>> {

    /** 当前资源对应的 MyBatis-Plus Service */
    protected abstract S service();

    protected long currentUserId() {
        return UserContext.currentUserId();
    }

    protected ResultConfig getPage(Integer pageNum, Integer pageRows) {
        return ResultConfig.success(service().page(new Page<>(pageNum, pageRows)));
    }

    protected ResultConfig getPage(Integer pageNum, Integer pageRows, Wrapper<T> wrapper) {
        return ResultConfig.success(service().page(new Page<>(pageNum, pageRows), wrapper));
    }

    protected ResultConfig add(T entity) {
        service().save(entity);
        return ResultConfig.success();
    }

    protected ResultConfig update(T entity) {
        service().updateById(entity);
        return ResultConfig.success();
    }

    protected ResultConfig update(T entity, Wrapper<T> wrapper) {
        service().update(entity, wrapper);
        return ResultConfig.success();
    }

    protected ResultConfig deleteBatch(Collection<? extends Serializable> ids) {
        service().removeByIds(ids);
        return ResultConfig.success();
    }

    protected ResultConfig delete(Wrapper<T> wrapper) {
        service().remove(wrapper);
        return ResultConfig.success();
    }

    protected ResultConfig success() {
        return ResultConfig.success();
    }

    protected ResultConfig success(Object data) {
        return ResultConfig.success(data);
    }

    protected ResultConfig error(String msg) {
        return ResultConfig.error(msg);
    }
}
