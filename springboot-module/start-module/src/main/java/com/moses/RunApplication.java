package com.moses;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;


@SpringBootApplication
@MapperScan("com.moses.**.mapper")
public class RunApplication {

    private static final String DEEPSEEK_AUTOCONFIG =
            "org.springframework.ai.model.deepseek.autoconfigure.DeepSeekChatAutoConfiguration";

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(RunApplication.class);
        // README: AI is optional — empty DEEPSEEK_KEY must not block startup
        if (!StringUtils.hasText(System.getenv("DEEPSEEK_KEY"))
                && !StringUtils.hasText(System.getProperty("DEEPSEEK_KEY"))) {
            Map<String, Object> defaults = new HashMap<>();
            defaults.put("spring.autoconfigure.exclude", DEEPSEEK_AUTOCONFIG);
            app.setDefaultProperties(defaults);
        }
        app.run(args);
    }
}
