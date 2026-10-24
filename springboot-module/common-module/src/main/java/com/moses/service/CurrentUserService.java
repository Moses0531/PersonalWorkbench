package com.moses.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moses.context.UserContext;
import com.moses.exception.BusinessException;
import com.moses.exception.ErrorCode;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * 按当前登录用户隔离数据的唯一入口。
 * <p>
 * 条件一律在这里用 {@link LambdaQueryWrapper} 生成，并带上 userId。
 * 不要改成普通 {@code QueryWrapper}，也不要在 Controller 再拼一套归属条件。
 * 系统角色、权限等全局数据继续用普通 {@link ServiceImpl}。
 */
public abstract class CurrentUserService<M extends BaseMapper<T>, T> extends ServiceImpl<M, T> {

    /** 主键字段，例如 {@code Note::getNoteId} */
    protected abstract SFunction<T, ?> idColumn();

    /** 所属用户字段，例如 {@code Note::getUserId} */
    protected abstract SFunction<T, ?> userIdColumn();

    protected long currentUserId() {
        return UserContext.currentUserId();
    }

    /** 已限定当前用户的查询条件，可继续链式追加筛选和排序 */
    protected LambdaQueryWrapper<T> currentUserQuery() {
        return new LambdaQueryWrapper<T>().eq(userIdColumn(), currentUserId());
    }

    /** 按主键查当前用户的一条；ID 为空或不存在则抛业务异常 */
    protected T currentUserGet(Serializable id, String emptyIdMsg, String notFoundMsg) {
        if (id == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, emptyIdMsg);
        }
        T entity = getOne(currentUserQuery().eq(idColumn(), id));
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, notFoundMsg);
        }
        return entity;
    }

    /** 批量删当前用户的数据；ids 为空则直接返回 */
    protected void currentUserRemove(Collection<? extends Serializable> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        remove(currentUserQuery().in(idColumn(), ids));
    }

    /** 分页查当前用户的数据；extra 里写筛选和排序 */
    protected IPage<T> currentUserPage(int pageNum, int pageRows,
                                       Consumer<LambdaQueryWrapper<T>> extra) {
        LambdaQueryWrapper<T> query = currentUserQuery();
        if (extra != null) {
            extra.accept(query);
        }
        return page(new Page<>(pageNum, pageRows), query);
    }

    /** 列表查当前用户的数据 */
    protected List<T> currentUserList(Consumer<LambdaQueryWrapper<T>> extra) {
        LambdaQueryWrapper<T> query = currentUserQuery();
        if (extra != null) {
            extra.accept(query);
        }
        return list(query);
    }

    /** 按主键更新当前用户的一条，条件带 userId */
    protected boolean currentUserUpdate(T entity, Serializable id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "ID不能为空");
        }
        return update(entity, currentUserQuery().eq(idColumn(), id));
    }
}
