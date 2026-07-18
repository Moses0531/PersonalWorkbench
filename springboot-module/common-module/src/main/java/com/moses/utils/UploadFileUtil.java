package com.moses.utils;

import com.moses.config.ResultConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.UUID;

@Component
public class UploadFileUtil {

    @Autowired
    private AliCloudUtil aliyunOSSOperator;

    /**
     * 上传文件 - 阿里云
     */
    public ResultConfig uploadToAliYun(MultipartFile file) throws Exception {
        if (!file.isEmpty()) {
            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extName = originalFilename.substring(originalFilename.lastIndexOf("."));
            String uniqueFileName = UUID.randomUUID().toString().replace("-", "") + extName;
            // 上传文件
            String url = aliyunOSSOperator.upload(file.getBytes(), uniqueFileName);
            return ResultConfig.success(url);
        }
        return ResultConfig.error("上传失败");
    }


    /**
     * 上传文件 - 参数名file
     */
    @PostMapping("/upload")
    public ResultConfig upload(MultipartFile file) throws Exception {
        if (!file.isEmpty()) {
            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extName = originalFilename.substring(originalFilename.lastIndexOf("."));
            String uniqueFileName = UUID.randomUUID().toString().replace("-", "") + extName;
            // 拼接完整的文件路径
            File targetFile = new File("D:/files/" + uniqueFileName);

            // 如果目标目录不存在，则创建它
            if (!targetFile.getParentFile().exists()) {
                targetFile.getParentFile().mkdirs();
            }
            // 保存文件
            file.transferTo(targetFile);
        }
        return ResultConfig.success();
    }
}
