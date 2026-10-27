package com.casxduois.client.modules;

/**
 * Category: Các nhóm tính năng trong R-Shift:
 * 1. WEBHOOK: Quản lý Webhook Discord
 * 2. RENDER_LIMIT: Giới hạn render thực thể, rương và Tăng Sáng (Fullbright)
 * 3. THROTTLE: Hạ FPS, GPU, CPU và giải phóng RAM khi treo
 * 4. MODS: Trình quản lý Mods (Mods Manager)
 */
public enum Category {
    WEBHOOK("Webhook", "Gửi nhận thông báo và điều khiển qua Discord"),
    HUD("Giao Diện HUD", "Tùy chỉnh, bật tắt và sắp xếp các khung thông tin trên màn hình"),
    RENDER_LIMIT("Giới Hạn Render", "Tối ưu tầm nhìn và tăng sáng ban đêm"),
    THROTTLE("Hạ Tài Nguyên", "Tiết kiệm CPU, GPU và giải phóng RAM khi treo"),
    AUTOMATION("Tự Động", "Tự động gõ lệnh, nhận /kit và hỗ trợ treo máy"),
    MODS("Quản Lý Mod", "Bật hoặc tắt các bản mod trong thư mục game");

    private final String displayName;
    private final String description;

    Category(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
