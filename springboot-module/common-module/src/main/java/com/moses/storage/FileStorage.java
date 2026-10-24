package com.moses.storage;

/**
 * 文件存储策略。业务侧只依赖本接口，具体实现由配置切换（OSS / 本地）。
 */
public interface FileStorage {

    /**
     * 上传字节内容，返回可对外访问的 URL 或路径。
     */
    String upload(byte[] content, String originalFilename) throws Exception;
}
