package com.moses.utils;

import java.util.regex.Pattern;

public class FormatUtil {

    private FormatUtil() {
    }

    /**
     * 校验邮箱格式
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return Pattern.matches("^[\\w.-]+@[\\w.-]+\\.\\w+$", email.trim());
    }

    /**
     * 校验11位手机号格式（中国大陆）
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return false;
        }
        return Pattern.matches("^1[3-9]\\d{9}$", phone.trim());
    }
}
