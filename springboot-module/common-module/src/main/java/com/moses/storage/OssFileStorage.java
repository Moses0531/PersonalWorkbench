package com.moses.storage;

import com.moses.utils.AliCloudUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.upload.storage", havingValue = "oss", matchIfMissing = true)
public class OssFileStorage implements FileStorage {

    private final AliCloudUtil aliCloudUtil;

    public OssFileStorage(AliCloudUtil aliCloudUtil) {
        this.aliCloudUtil = aliCloudUtil;
    }

    @Override
    public String upload(byte[] content, String originalFilename) throws Exception {
        return aliCloudUtil.upload(content, originalFilename);
    }
}
