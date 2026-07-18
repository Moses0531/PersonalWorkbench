package com.moses.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FormatUtilTest {

    @Test
    void isValidEmail() {
        assertFalse(FormatUtil.isValidEmail(null));
        assertFalse(FormatUtil.isValidEmail(" "));
        assertFalse(FormatUtil.isValidEmail("bad"));
        assertTrue(FormatUtil.isValidEmail("user@example.com"));
        assertTrue(FormatUtil.isValidEmail("  user@example.com  "));
    }

    @Test
    void isValidPhone() {
        assertFalse(FormatUtil.isValidPhone(null));
        assertFalse(FormatUtil.isValidPhone("12345"));
        assertFalse(FormatUtil.isValidPhone("12345678901"));
        assertTrue(FormatUtil.isValidPhone("13800138000"));
        assertTrue(FormatUtil.isValidPhone(" 13912345678 "));
    }
}
