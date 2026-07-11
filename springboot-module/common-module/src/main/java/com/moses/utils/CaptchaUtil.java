package com.moses.utils;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import com.moses.config.ResultConfig;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Component
@RestController
@RequestMapping("/captchas")
public class CaptchaUtil {

    private DefaultKaptcha kaptcha;
    
    // 验证码加密密钥（实际项目应该从配置文件读取）
    private static final String CAPTCHA_SECRET = "CAPTCHA_SECRET_KEY_2026";

    @PostConstruct
    public void init() {
        Properties properties = new Properties();
        properties.setProperty("kaptcha.border", "no");
        properties.setProperty("kaptcha.image.width", "160");
        properties.setProperty("kaptcha.image.height", "60");
        properties.setProperty("kaptcha.textproducer.char.length", "4");
        properties.setProperty("kaptcha.textproducer.font.size", "45");
        properties.setProperty("kaptcha.textproducer.font.color", "black");
        properties.setProperty("kaptcha.textproducer.font.names", "Arial,Verdana");
        properties.setProperty("kaptcha.noise.impl", "com.google.code.kaptcha.impl.NoNoise");
        properties.setProperty("kaptcha.obscurificator.impl", "com.google.code.kaptcha.impl.ShadowGimpy");

        Config config = new Config(properties);
        kaptcha = new DefaultKaptcha();
        kaptcha.setConfig(config);
    }

    @GetMapping("/getCharacter")
    public void getCharacter(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws Exception {
        String code = kaptcha.createText();
        BufferedImage image = kaptcha.createImage(code);

        // 无状态设计：不再使用Session存储验证码
        // TODO: 如需启用，请配合无状态token验证机制

        servletResponse.setContentType("image/png");
        javax.imageio.ImageIO.write(image, "png", servletResponse.getOutputStream());
    }

    @GetMapping("/arithmetic")
    public void getArithmetic(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws Exception {
        int num1 = (int) (Math.random() * 10) + 1;
        int num2 = (int) (Math.random() * 10) + 1;
        int answer = num1 + num2;
        String code = num1 + "+" + num2 + "=?";

        BufferedImage image = kaptcha.createImage(code);

        // 无状态设计：不再使用Session存储验证码
        // TODO: 如需启用，请配合无状态token验证机制

        servletResponse.setContentType("image/png");
        javax.imageio.ImageIO.write(image, "png", servletResponse.getOutputStream());
    }

    @GetMapping("/getCaptchaBase64")
    public ResultConfig getCaptchaBase64(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        try {
            String code = kaptcha.createText();
            BufferedImage image = kaptcha.createImage(code);

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "png", os);
            String imgBase64 = Base64.getEncoder().encodeToString(os.toByteArray());

            // 生成验证码的加密token（使用时间戳+密钥进行MD5加密）
            long timestamp = System.currentTimeMillis();
            String token = generateCaptchaToken(code.toUpperCase(), timestamp);

            Map<String, Object> data = new HashMap<>();
            data.put("img", "data:image/png;base64," + imgBase64);
            data.put("token", token); // 返回加密token给前端
            data.put("timestamp", timestamp); // 返回时间戳

            return ResultConfig.success(data);
        } catch (Exception e) {
            e.printStackTrace();
            return ResultConfig.error("验证码生成失败: " + e.getMessage());
        }
    }

    public ResultConfig generateCaptcha() {
        try {
            String code = kaptcha.createText();
            BufferedImage image = kaptcha.createImage(code);

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "png", os);
            String imgBase64 = Base64.getEncoder().encodeToString(os.toByteArray());

            Map<String, String> data = new HashMap<>();
            data.put("img", imgBase64);

            return ResultConfig.success(data);

        } catch (Exception e) {
            e.printStackTrace();
            return ResultConfig.error("验证码生成失败: " + e.getMessage());
        }
    }
    
    /**
     * 生成验证码token（无状态设计）
     * 格式：MD5(验证码明文 + 时间戳 + 密钥)
     */
    private String generateCaptchaToken(String captchaCode, long timestamp) {
        try {
            String raw = captchaCode + timestamp + CAPTCHA_SECRET;
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new RuntimeException("生成验证码token失败", e);
        }
    }
    
    /**
     * 验证验证码（无状态设计）
     * @param captchaCode 用户输入的验证码明文
     * @param token 前端返回的token
     * @param timestamp 前端返回的时间戳
     * @return 是否验证通过
     */
    public boolean verifyCaptcha(String captchaCode, String token, Long timestamp) {
        if (captchaCode == null || token == null || timestamp == null) {
            return false;
        }
        
        // 检查时间戳是否过期（5分钟有效期）
        long currentTime = System.currentTimeMillis();
        if (currentTime - timestamp > 5 * 60 * 1000) {
            return false;
        }
        
        // 重新计算token并比对
        String expectedToken = generateCaptchaToken(captchaCode.toUpperCase(), timestamp);
        return expectedToken.equals(token);
    }
}
