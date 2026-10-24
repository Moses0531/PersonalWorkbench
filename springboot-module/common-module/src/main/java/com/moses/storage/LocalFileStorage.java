package com.moses.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.upload.storage", havingValue = "local")
public class LocalFileStorage implements FileStorage {

    @Value("${app.upload.local-dir:uploads/}")
    private String uploadDir;

    @Value("${app.upload.public-prefix:/uploads/}")
    private String publicPrefix;

    @Override
    public String upload(byte[] content, String originalFilename) throws Exception {
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("文件名不合法");
        }
        String ext = originalFilename.substring(originalFilename.lastIndexOf('.'));
        String uniqueName = UUID.randomUUID().toString().replace("-", "") + ext;

        String normalizedDir = uploadDir.replace('\\', '/');
        if (!normalizedDir.endsWith("/")) {
            normalizedDir += "/";
        }

        File baseDir = new File(System.getProperty("user.dir"), normalizedDir);
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            throw new IllegalStateException("创建上传目录失败: " + baseDir.getAbsolutePath());
        }

        File target = new File(baseDir, uniqueName);
        Files.write(target.toPath(), content);

        String prefix = publicPrefix.replace('\\', '/');
        if (!prefix.startsWith("/")) {
            prefix = "/" + prefix;
        }
        if (!prefix.endsWith("/")) {
            prefix += "/";
        }
        return prefix + uniqueName;
    }
}
