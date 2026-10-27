package com.casxduois.client.modules.render;

import com.casxduois.client.config.BooleanSetting;
import com.casxduois.client.config.NumberSetting;
import com.casxduois.client.modules.Category;
import com.casxduois.client.modules.Module;
import org.lwjgl.input.Keyboard;

/**
 * HUDModule: Quản lý toàn bộ giao diện thông tin HUD in-game
 * Hỗ trợ ẩn toàn bộ HUD, tùy chỉnh bật/tắt từng khung thẻ và kéo thả vị trí
 */
public class HUDModule extends Module {

    // 1. Nhóm Bật/Tắt các Khung Thẻ Thông Tin
    public final BooleanSetting showWatermark = new BooleanSetting("Watermark", "Khung Tài Khoản & Server", "Hiển thị thẻ tên tài khoản, FPS, máy chủ và ping", true);
    public final BooleanSetting showStats = new BooleanSetting("Stats", "Khung Thông Số RAM", "Hiển thị thẻ tài nguyên bộ nhớ RAM và thanh tiến trình", true);
    public final BooleanSetting showCoords = new BooleanSetting("Coords", "Khung Tọa Độ & Biome", "Hiển thị thẻ tọa độ XYZ, biome và hướng nhìn", true);
    public final BooleanSetting showModules = new BooleanSetting("Module List", "Khung Tính Năng Đang Bật", "Hiển thị danh sách các tính năng đang kích hoạt", true);
    public final BooleanSetting showArmor = new BooleanSetting("Armor HUD", "Khung Độ Bền Giáp & Vũ Khí", "Hiển thị trang bị giáp trên người và độ bền", true);
    public final BooleanSetting showSpeed = new BooleanSetting("Speed BPS", "Khung Tốc Độ Di Chuyển", "Hiển thị tốc độ di chuyển thực tế (Blocks/giây)", false);

    // 2. Nhóm Tùy Chỉnh Thẩm Mỹ Khung
    public final BooleanSetting cardBackground = new BooleanSetting("Card Background", "Nền Khung Kính Mờ", "Bật nền thẻ kính mờ Obsidian chống mỏi mắt", true);
    public final BooleanSetting borderAccent = new BooleanSetting("Border Glow", "Viền Sáng Tinh Tế", "Hiển thị viền mờ tinh tế quanh các khung thông tin", true);
    public final NumberSetting hudOpacity = new NumberSetting("HUD Opacity", "Độ Đậm Khung (%)", "Độ mờ đục của nền thẻ kính mờ", 85.0, 20.0, 100.0, 5.0);
    public final NumberSetting hudScale = new NumberSetting("HUD Scale", "Kích Thước HUD (%)", "Tỉ lệ hiển thị của các khung thông tin", 100.0, 75.0, 125.0, 5.0);

    // 3. Tọa độ lưu vị trí kéo thả từng khung
    public final NumberSetting watermarkX = new NumberSetting("Pos_WM_X", "WM_X", "Tọa độ X Watermark", 8.0, 0.0, 3840.0, 1.0);
    public final NumberSetting watermarkY = new NumberSetting("Pos_WM_Y", "WM_Y", "Tọa độ Y Watermark", 8.0, 0.0, 2160.0, 1.0);

    public final NumberSetting statsX = new NumberSetting("Pos_ST_X", "ST_X", "Tọa độ X Stats", 8.0, 0.0, 3840.0, 1.0);
    public final NumberSetting statsY = new NumberSetting("Pos_ST_Y", "ST_Y", "Tọa độ Y Stats", 42.0, 0.0, 2160.0, 1.0);

    public final NumberSetting coordsX = new NumberSetting("Pos_CD_X", "CD_X", "Tọa độ X Coords", 8.0, -100.0, 3840.0, 1.0);
    public final NumberSetting coordsY = new NumberSetting("Pos_CD_Y", "CD_Y", "Tọa độ Y Coords", -1.0, -100.0, 2160.0, 1.0);

    public final NumberSetting modulesX = new NumberSetting("Pos_MD_X", "MD_X", "Tọa độ X Modules", -1.0, -100.0, 3840.0, 1.0);
    public final NumberSetting modulesY = new NumberSetting("Pos_MD_Y", "MD_Y", "Tọa độ Y Modules", 8.0, -100.0, 2160.0, 1.0);

    public final NumberSetting armorX = new NumberSetting("Pos_AR_X", "AR_X", "Tọa độ X Armor", -1.0, -100.0, 3840.0, 1.0);
    public final NumberSetting armorY = new NumberSetting("Pos_AR_Y", "AR_Y", "Tọa độ Y Armor", -1.0, -100.0, 2160.0, 1.0);

    public final NumberSetting speedX = new NumberSetting("Pos_SP_X", "SP_X", "Tọa độ X Speed", 8.0, -100.0, 3840.0, 1.0);
    public final NumberSetting speedY = new NumberSetting("Pos_SP_Y", "SP_Y", "Tọa độ Y Speed", 72.0, -100.0, 2160.0, 1.0);

    public HUDModule() {
        super("HUD", "Giao Diện HUD", "Tùy chỉnh và ẩn/hiện các khung thông số in-game", Category.HUD, Keyboard.KEY_F7);

        // Đăng ký cài đặt hiển thị
        addSetting(showWatermark);
        addSetting(showStats);
        addSetting(showCoords);
        addSetting(showModules);
        addSetting(showArmor);
        addSetting(showSpeed);

        addSetting(cardBackground);
        addSetting(borderAccent);
        addSetting(hudOpacity);
        addSetting(hudScale);

        // Đăng ký tọa độ để lưu vào config
        addSetting(watermarkX);
        addSetting(watermarkY);
        addSetting(statsX);
        addSetting(statsY);
        addSetting(coordsX);
        addSetting(coordsY);
        addSetting(modulesX);
        addSetting(modulesY);
        addSetting(armorX);
        addSetting(armorY);
        addSetting(speedX);
        addSetting(speedY);

        setToggled(true);
    }

    public void resetPositions() {
        watermarkX.setValue(8.0);
        watermarkY.setValue(8.0);
        statsX.setValue(8.0);
        statsY.setValue(42.0);
        coordsX.setValue(8.0);
        coordsY.setValue(-1.0);
        modulesX.setValue(-1.0);
        modulesY.setValue(8.0);
        armorX.setValue(-1.0);
        armorY.setValue(-1.0);
        speedX.setValue(8.0);
        speedY.setValue(72.0);
    }
}
