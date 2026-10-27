package com.casxduois.client.util;

import java.awt.Color;

/**
 * ColorUtils: Bảng màu Glassmorphism Nhẹ + Gradient Accent (Indigo #6366F1 -> Violet #8B5CF6)
 * Tinh tế, hiện đại, chiều sâu obsidian tối sâu kết hợp viền mờ và điểm nhấn gradient
 */
public class ColorUtils {

    // Nền tối Obsidian Deep Dark & Glassmorphism
    public static final Color BG_DEEP = new Color(6, 8, 14, 255);             // #06080E Nền tối sâu nhất
    public static final Color BG_DARK = BG_DEEP;                              // Alias tương thích
    public static final Color BG_SURFACE = new Color(12, 16, 26, 235);        // #0C101A Nền bề mặt
    public static final Color BG_GLASS = new Color(14, 18, 28, 208);          // Glassmorphism nền (~82% alpha)
    public static final Color BG_CARD = new Color(18, 22, 34, 195);           // Card glassmorphism (~76% alpha)
    public static final Color BG_CARD_HOVER = new Color(26, 32, 48, 220);     // Card khi hover
    public static final Color BG_INPUT = new Color(10, 12, 20, 240);          // Nền ô nhập dữ liệu

    // Gradient Accent (Indigo -> Violet) làm điểm nhấn visual chính
    public static final Color ACCENT_START = new Color(99, 102, 241, 255);    // #6366F1 Indigo-500
    public static final Color ACCENT_END = new Color(139, 92, 246, 255);      // #8B5CF6 Violet-500
    public static final Color ACCENT_GLOW = new Color(99, 102, 241, 40);      // Glow halo cho accent
    public static final Color ACCENT_SOFT = new Color(99, 102, 241, 25);      // Nền active nhẹ

    // Viền kính mờ & viền Accent
    public static final Color BORDER_GLASS = new Color(255, 255, 255, 18);     // Viền kính rất mảnh (~7% alpha)
    public static final Color BORDER_CARD = new Color(255, 255, 255, 24);      // Viền card chuẩn
    public static final Color BORDER_CARD_HOVER = new Color(99, 102, 241, 90); // Viền card hover ánh Indigo
    public static final Color BORDER_HOVER = new Color(99, 102, 241, 90);      // Alias
    public static final Color BORDER_ACTIVE = new Color(139, 92, 246, 140);    // Viền active/selected ánh Violet
    public static final Color BORDER_INPUT = new Color(255, 255, 255, 22);     // Viền ô nhập

    // Điểm nhấn trạng thái & Semantic colors
    public static final Color PRIMARY_WHITE = new Color(248, 250, 252);        // #F8FAFC Trắng ngà cao cấp
    public static final Color PRIMARY_CYAN = ACCENT_START;                     // Điểm nhấn Indigo
    public static final Color ACCENT_CYAN = new Color(129, 140, 248);          // Indigo sáng
    public static final Color ACCENT_CYAN_GLOW = new Color(99, 102, 241, 120); // Glow
    public static final Color ACCENT_PURPLE = ACCENT_END;                      // Violet
    public static final Color ACCENT_VIOLET = new Color(167, 139, 250);        // Violet sáng
    public static final Color ACCENT_GOLD = new Color(245, 158, 11);           // Hổ phách
    public static final Color ACCENT_GREEN = new Color(34, 197, 94);           // #22C55E Xanh lá thành công
    public static final Color ACCENT_RED = new Color(239, 68, 68);             // #EF4444 Đỏ cảnh báo/lỗi
    public static final Color ACCENT_ORANGE = new Color(245, 158, 11);         // #F59E0B Cam cảnh báo

    // Màu hiển thị ESP
    public static final Color ESP_PLAYER = PRIMARY_WHITE;
    public static final Color ESP_CHEST = ACCENT_ORANGE;
    public static final Color ESP_ENDER_CHEST = ACCENT_PURPLE;

    // Typography chuẩn sắc nét
    public static final Color TEXT_WHITE = new Color(248, 250, 252);           // #F8FAFC Trắng chính
    public static final Color TEXT_SILVER = new Color(148, 163, 184);          // #94A3B8 Bạc phụ
    public static final Color TEXT_MUTED = new Color(100, 116, 139);           // #64748B Xám ghi mờ
    public static final Color TEXT_DARK = new Color(71, 85, 105);              // #475569 Xám tối

    public static int toRGBA(int r, int g, int b, int a) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    public static Color blend(Color c1, Color c2, float ratio) {
        float rClamped = Math.max(0.0f, Math.min(1.0f, ratio));
        float iRatio = 1.0f - rClamped;
        int r = (int) (c1.getRed() * iRatio + c2.getRed() * rClamped);
        int g = (int) (c1.getGreen() * iRatio + c2.getGreen() * rClamped);
        int b = (int) (c1.getBlue() * iRatio + c2.getBlue() * rClamped);
        int a = (int) (c1.getAlpha() * iRatio + c2.getAlpha() * rClamped);
        return new Color(r, g, b, a);
    }

    /**
     * Lấy màu gradient giữa ACCENT_START và ACCENT_END theo tiến trình (0.0 -> 1.0)
     */
    public static Color getAccentGradient(float progress) {
        return blend(ACCENT_START, ACCENT_END, progress);
    }

    /**
     * Hiệu ứng accent biến thiên nhẹ nhàng theo thời gian
     */
    public static Color getAccentAnimated(long offset, float speed) {
        float wave = (float) ((Math.sin((System.currentTimeMillis() + offset) * 0.002 * speed) + 1.0) / 2.0);
        return blend(ACCENT_START, ACCENT_END, wave);
    }

    public static Color getRainbow(float seconds, float saturation, float brightness, long index) {
        float hue = ((System.currentTimeMillis() + index) % (int) (seconds * 1000)) / (seconds * 1000f);
        return Color.getHSBColor(hue, saturation, brightness);
    }
}
