package com.casxduois.client.config;

/**
 * StringSetting: Cấu hình kiểu chuỗi ký tự (URL Webhook, Bot Token, Channel ID, Mật khẩu)
 */
public class StringSetting extends Setting<String> {

    private final boolean isPassword;

    public StringSetting(String name, String vietnameseName, String description, String defaultValue) {
        this(name, vietnameseName, description, defaultValue, false);
    }

    public StringSetting(String name, String vietnameseName, String description, String defaultValue, boolean isPassword) {
        super(name, vietnameseName, description, defaultValue);
        this.isPassword = isPassword;
    }

    public boolean isPassword() {
        return isPassword;
    }

    public boolean isMasked() {
        return isPassword;
    }

    public String getMaskedValue() {
        if (!isPassword || value == null || value.isEmpty()) {
            return value;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            sb.append("•");
        }
        return sb.toString();
    }
}
