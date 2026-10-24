package com.moses.context;

/**
 * 一次请求内解析出的登录用户。
 *
 * @param userId 登录用户 id
 * @param banned 账号是否被封禁
 */
public record CurrentUser(long userId, boolean banned) {
}
