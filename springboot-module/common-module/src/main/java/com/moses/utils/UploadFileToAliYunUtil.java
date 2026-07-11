package com.moses.utils;

import com.moses.config.ResultConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;


@Component
public class UploadFileToAliYunUtil {

    @Autowired
    private AliCouldUtil aliyunOSSOperator;

    public ResultConfig upload(MultipartFile file) throws Exception {
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

}
