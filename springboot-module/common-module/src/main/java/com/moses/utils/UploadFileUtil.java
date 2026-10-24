package com.moses.utils;

import com.moses.config.ResultConfig;
import com.moses.exception.ErrorCode;
import com.moses.storage.FileStorage;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 上传入口：委托 {@link FileStorage}，由 {@code app.upload.storage} 切换 OSS / 本地。
 */
@Component
public class UploadFileUtil {

    private final FileStorage fileStorage;

    public UploadFileUtil(FileStorage fileStorage) {
        this.fileStorage = fileStorage;
    }

    public ResultConfig upload(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            return ResultConfig.error(ErrorCode.BAD_REQUEST, "上传文件不能为空");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            return ResultConfig.error(ErrorCode.BAD_REQUEST, "文件名不合法");
        }
        String url = fileStorage.upload(file.getBytes(), originalFilename);
        return ResultConfig.success(url);
    }

    /** @deprecated 使用 {@link #upload(MultipartFile)}，存储策略由配置决定 */
    @Deprecated
    public ResultConfig uploadToAliYun(MultipartFile file) throws Exception {
        return upload(file);
    }
}
