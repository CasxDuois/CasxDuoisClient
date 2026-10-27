package com.casxduois.client.gui.clickgui;

import com.casxduois.client.CasxDuoisClient;
import com.casxduois.client.modules.external.ExternalModManager;
import com.casxduois.client.util.ColorUtils;
import com.casxduois.client.util.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import java.awt.Color;
import java.util.List;

/**
 * ModManagerPanel: Giao diện tab "Quản Lý Mod" trong ClickGUI
 * Tự động thích ứng mọi GUI scale và chống tràn chữ
 */
public class ModManagerPanel {

    private static final Minecraft mc = Minecraft.getMinecraft();

    public static void draw(float contentX, float contentY, float cardWidth, float panelHeight, int scrollOffset, int mouseX, int mouseY) {
        FontRenderer font = mc.fontRenderer;
        ExternalModManager manager = CasxDuoisClient.getInstance().getExternalModManager();
        if (manager == null) return;

        // 1. Thanh công cụ Toolbar trên cùng
        float topBarY = contentY;
        RenderUtils.drawRoundedRect(contentX, topBarY, cardWidth, 26f, 5, ColorUtils.BG_CARD.getRGB());
        RenderUtils.drawRoundedOutline(contentX, topBarY, cardWidth, 26f, 5, 0.8f, ColorUtils.BORDER_GLASS.getRGB());

        // Nút Mở Thư Mục Mods
        float folderW = Math.min(106f, (cardWidth - 18f) * 0.60f);
        boolean hoverFolder = mouseX >= contentX + 5f && mouseX <= contentX + 5f + folderW && mouseY >= topBarY + 4f && mouseY <= topBarY + 22f;
        int folderBg = hoverFolder ? ColorUtils.BG_CARD_HOVER.getRGB() : ColorUtils.BG_INPUT.getRGB();
        RenderUtils.drawRoundedRect(contentX + 5f, topBarY + 4f, folderW, 18f, 4, folderBg);
        RenderUtils.drawRoundedOutline(contentX + 5f, topBarY + 4f, folderW, 18f, 4, 0.8f, hoverFolder ? ColorUtils.BORDER_HOVER.getRGB() : ColorUtils.BORDER_GLASS.getRGB());
        font.drawString("Mở thư mục mod", (int) (contentX + 10f), (int) (topBarY + 9f), ColorUtils.PRIMARY_WHITE.getRGB());

        // Nút Quét Lại
        float scanX = contentX + 5f + folderW + 6f;
        float scanW = Math.min(60f, cardWidth - folderW - 18f);
        boolean hoverScan = mouseX >= scanX && mouseX <= scanX + scanW && mouseY >= topBarY + 4f && mouseY <= topBarY + 22f;
        int scanBg = hoverScan ? ColorUtils.BG_CARD_HOVER.getRGB() : ColorUtils.BG_INPUT.getRGB();
        RenderUtils.drawRoundedRect(scanX, topBarY + 4f, scanW, 18f, 4, scanBg);
        RenderUtils.drawRoundedOutline(scanX, topBarY + 4f, scanW, 18f, 4, 0.8f, hoverScan ? ColorUtils.BORDER_HOVER.getRGB() : ColorUtils.BORDER_GLASS.getRGB());
        font.drawString("Quét lại", (int) (scanX + (scanW - font.getStringWidth("Quét lại")) / 2f), (int) (topBarY + 9f), ColorUtils.TEXT_SILVER.getRGB());

        // Cảnh báo ngắn gọn, chống tràn chữ
        float warnY = topBarY + 30f;
        RenderUtils.drawRoundedRect(contentX, warnY, cardWidth, 18f, 4, ColorUtils.BG_INPUT.getRGB());
        RenderUtils.drawRoundedOutline(contentX, warnY, cardWidth, 18f, 4, 0.8f, ColorUtils.BORDER_GLASS.getRGB());
        String warnText = "Khởi động lại game để áp dụng khi bật hoặc tắt mod.";
        float maxWarnW = cardWidth - 16f;
        if (font.getStringWidth(warnText) > maxWarnW) {
            warnText = font.trimStringToWidth(warnText, (int) (maxWarnW - 8f)) + "..";
        }
        font.drawString(warnText, (int) (contentX + 8f), (int) (warnY + 5f), ColorUtils.TEXT_MUTED.getRGB());

        // 2. Danh sách Mod (.jar)
        float itemY = warnY + 24f + scrollOffset;
        List<ExternalModManager.ModEntry> mods = manager.getMods();

        if (mods.isEmpty()) {
            RenderUtils.drawRoundedRect(contentX, itemY, cardWidth, 36f, 6, ColorUtils.BG_CARD.getRGB());
            RenderUtils.drawRoundedOutline(contentX, itemY, cardWidth, 36f, 6, 0.8f, ColorUtils.BORDER_GLASS.getRGB());
            font.drawString("Chưa có mod nào trong thư mục mods.", (int) (contentX + 10f), (int) (itemY + 14f), ColorUtils.TEXT_MUTED.getRGB());
            return;
        }

        for (ExternalModManager.ModEntry mod : mods) {
            float itemHeight = 36f;
            boolean isHover = mouseX >= contentX && mouseX <= contentX + cardWidth && mouseY >= itemY && mouseY <= itemY + itemHeight;
            int cardColor = isHover ? ColorUtils.BG_CARD_HOVER.getRGB() : ColorUtils.BG_CARD.getRGB();

            RenderUtils.drawRoundedRect(contentX, itemY, cardWidth, itemHeight, 6, cardColor);
            RenderUtils.drawRoundedOutline(contentX, itemY, cardWidth, itemHeight, 6, 0.8f,
                    mod.isEnabled() ? ColorUtils.BORDER_ACTIVE.getRGB() : (isHover ? ColorUtils.BORDER_HOVER.getRGB() : ColorUtils.BORDER_GLASS.getRGB()));

            if (mod.isEnabled()) {
                RenderUtils.drawAccentIndicator(contentX + 2.5f, itemY + 6f, 2.5f, itemHeight - 12f, 1.25f);
            }

            // Chống tràn chữ tên mod và tên file
            float maxModTextW = cardWidth - 44f;

            String title = mod.getName() + " v" + mod.getVersion();
            if (font.getStringWidth(title) > maxModTextW) {
                title = font.trimStringToWidth(title, (int) (maxModTextW - 8f)) + "...";
            }
            font.drawStringWithShadow(title, contentX + 10f, itemY + 7f, mod.isEnabled() ? ColorUtils.PRIMARY_WHITE.getRGB() : ColorUtils.TEXT_SILVER.getRGB());

            String fileInfo = mod.getFile().getName() + " - " + (mod.isEnabled() ? "Đang bật" : "Đã tắt");
            if (font.getStringWidth(fileInfo) > maxModTextW) {
                fileInfo = font.trimStringToWidth(fileInfo, (int) (maxModTextW - 8f)) + "...";
            }
            font.drawString(fileInfo, (int) (contentX + 10f), (int) (itemY + 20f), ColorUtils.TEXT_MUTED.getRGB());

            // Công tắc Toggle Switch Gradient Accent
            float toggleX = contentX + cardWidth - 32f;
            float toggleY = itemY + 11f;
            boolean hoverToggle = mouseX >= toggleX && mouseX <= toggleX + 28f && mouseY >= toggleY && mouseY <= toggleY + 16f;

            float w = 28f, h = 15f;
            if (mod.isEnabled()) {
                RenderUtils.drawRoundedGradientRect(toggleX, toggleY, w, h, 7.5f, ColorUtils.ACCENT_START.getRGB(), ColorUtils.ACCENT_END.getRGB());
                RenderUtils.drawRoundedOutline(toggleX, toggleY, w, h, 7.5f, 0.8f, ColorUtils.BORDER_ACTIVE.getRGB());
                float knobX = toggleX + w - 13f;
                RenderUtils.drawRoundedRect(knobX, toggleY + 2f, 11f, 11f, 5.5f, ColorUtils.PRIMARY_WHITE.getRGB());
            } else {
                int toggleBg = hoverToggle ? ColorUtils.BG_CARD_HOVER.getRGB() : ColorUtils.BG_INPUT.getRGB();
                RenderUtils.drawRoundedRect(toggleX, toggleY, w, h, 7.5f, toggleBg);
                RenderUtils.drawRoundedOutline(toggleX, toggleY, w, h, 7.5f, 0.8f, hoverToggle ? ColorUtils.BORDER_HOVER.getRGB() : ColorUtils.BORDER_GLASS.getRGB());
                float knobX = toggleX + 2f;
                RenderUtils.drawRoundedRect(knobX, toggleY + 2f, 11f, 11f, 5.5f, ColorUtils.TEXT_MUTED.getRGB());
            }

            itemY += itemHeight + 6f;
        }
    }

    public static boolean mouseClicked(float contentX, float contentY, float cardWidth, int scrollOffset, int mouseX, int mouseY) {
        ExternalModManager manager = CasxDuoisClient.getInstance().getExternalModManager();
        if (manager == null) return false;

        float topBarY = contentY;

        // Click Mở Thư Mục Mods
        float folderW = Math.min(106f, (cardWidth - 18f) * 0.60f);
        if (mouseX >= contentX + 5f && mouseX <= contentX + 5f + folderW && mouseY >= topBarY + 4f && mouseY <= topBarY + 22f) {
            manager.openModsFolder();
            return true;
        }

        // Click Quét Lại
        float scanX = contentX + 5f + folderW + 6f;
        float scanW = Math.min(60f, cardWidth - folderW - 18f);
        if (mouseX >= scanX && mouseX <= scanX + scanW && mouseY >= topBarY + 4f && mouseY <= topBarY + 22f) {
            manager.scanMods();
            return true;
        }

        // Click Toggle mod
        float warnY = topBarY + 30f;
        float itemY = warnY + 24f + scrollOffset;
        for (ExternalModManager.ModEntry mod : manager.getMods()) {
            float toggleX = contentX + cardWidth - 32f;
            float toggleY = itemY + 11f;
            if (mouseX >= toggleX - 4f && mouseX <= toggleX + 30f && mouseY >= toggleY - 4f && mouseY <= toggleY + 18f) {
                manager.toggleMod(mod);
                return true;
            }
            itemY += 36f + 6f;
        }

        return false;
    }
}
