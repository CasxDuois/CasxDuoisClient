package com.casxduois.client.gui.hud;

import com.casxduois.client.CasxDuoisClient;
import com.casxduois.client.core.MemoryOptimizer;
import com.casxduois.client.modules.Module;
import com.casxduois.client.modules.render.HUDModule;
import com.casxduois.client.util.ColorUtils;
import com.casxduois.client.util.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * CasxDuoisHUD: Giao diện HUD hiện đại Monochrome Dark Theme
 * Thiết kế các thẻ thông tin dạng khung viền kính mờ Obsidian cao cấp
 * Hỗ trợ kéo thả vị trí, tùy biến độ mờ đục và bật/tắt toàn bộ giao diện
 */
public class CasxDuoisHUD {

    private final Minecraft mc = Minecraft.getMinecraft();

    public void render(float partialTicks) {
        HUDModule hudModule = CasxDuoisClient.getInstance().getModuleManager().getModule(HUDModule.class);
        if (hudModule == null || !hudModule.isToggled() || mc.gameSettings.showDebugInfo) {
            return;
        }

        ScaledResolution sr = new ScaledResolution(mc);
        FontRenderer fr = mc.fontRenderer;

        float scale = hudModule.hudScale.getValueFloat() / 100.0f;
        boolean useScale = Math.abs(scale - 1.0f) > 0.01f;

        if (useScale) {
            GlStateManager.pushMatrix();
            GlStateManager.scale(scale, scale, 1.0f);
        }

        int opacityAlpha = (int) (hudModule.hudOpacity.getValueFloat() * 2.55f);
        int cardBgColor = ((opacityAlpha & 0xFF) << 24) | (ColorUtils.BG_CARD.getRGB() & 0x00FFFFFF);
        boolean drawBg = hudModule.cardBackground.isEnabled();
        boolean drawBorder = hudModule.borderAccent.isEnabled();

        // 1. Khung Watermark: Tên tài khoản, FPS, Máy chủ & Ping
        if (hudModule.showWatermark.isEnabled()) {
            renderWatermark(hudModule, sr, fr, drawBg, drawBorder, cardBgColor);
        }

        // 2. Khung Thông Số RAM & Bộ Nhớ Hệ Thống
        if (hudModule.showStats.isEnabled()) {
            renderStats(hudModule, sr, fr, drawBg, drawBorder, cardBgColor);
        }

        // 3. Khung Tọa Độ XYZ, Biome & Hướng Nhìn
        if (hudModule.showCoords.isEnabled() && mc.player != null) {
            renderCoords(hudModule, sr, fr, drawBg, drawBorder, cardBgColor);
        }

        // 4. Khung Danh Sách Tính Năng Đang Kích Hoạt (Module List)
        if (hudModule.showModules.isEnabled()) {
            renderModules(hudModule, sr, fr, drawBg, drawBorder, cardBgColor);
        }

        // 5. Khung Độ Bền Giáp & Vũ Khí Cầm Tay (Armor HUD)
        if (hudModule.showArmor.isEnabled() && mc.player != null) {
            renderArmor(hudModule, sr, fr, drawBg, drawBorder, cardBgColor);
        }

        // 6. Khung Tốc Độ Di Chuyển (Speed BPS)
        if (hudModule.showSpeed.isEnabled() && mc.player != null) {
            renderSpeed(hudModule, sr, fr, drawBg, drawBorder, cardBgColor);
        }

        if (useScale) {
            GlStateManager.popMatrix();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // CÁC THÀNH PHẦN KHUNG THẺ THÔNG TIN (WIDGETS)
    // ─────────────────────────────────────────────────────────────────

    private void renderWatermark(HUDModule mod, ScaledResolution sr, FontRenderer fr, boolean drawBg, boolean drawBorder, int bgCol) {
        float x = mod.watermarkX.getValueFloat();
        float y = mod.watermarkY.getValueFloat();

        String accountName = (mc.getSession() != null && mc.getSession().getUsername() != null) ? mc.getSession().getUsername() : "CasxDuois";
        int fps = Minecraft.getDebugFPS();
        String serverName = mc.getCurrentServerData() != null ? mc.getCurrentServerData().serverIP : "Chơi đơn";

        int ping = 0;
        try {
            if (mc.getConnection() != null && mc.player != null) {
                NetworkPlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUniqueID());
                if (info != null) ping = info.getResponseTime();
            }
        } catch (Throwable ignored) {}

        String subText = "FPS: " + fps + " · " + serverName + (ping > 0 ? " · " + ping + "ms" : "");

        float textW = Math.max(fr.getStringWidth(accountName) + 16, fr.getStringWidth(subText));
        float cardW = Math.max(130f, textW + 18f);
        float cardH = 30f;

        if (drawBg) {
            RenderUtils.drawRoundedRect(x, y, cardW, cardH, 5f, bgCol);
        }
        if (drawBorder) {
            RenderUtils.drawRoundedOutline(x, y, cardW, cardH, 5f, 0.8f, ColorUtils.BORDER_GLASS.getRGB());
        }

        // Điểm nhấn Vạch Gradient Accent
        RenderUtils.drawAccentIndicator(x + 7f, y + 6f, 2.5f, 9f, 1.25f);
        fr.drawStringWithShadow(accountName, (int) (x + 14f), (int) (y + 6f), ColorUtils.PRIMARY_WHITE.getRGB());
        fr.drawString(subText, (int) (x + 8f), (int) (y + 17f), ColorUtils.TEXT_MUTED.getRGB());
    }

    private void renderStats(HUDModule mod, ScaledResolution sr, FontRenderer fr, boolean drawBg, boolean drawBorder, int bgCol) {
        float x = mod.statsX.getValueFloat();
        float y = mod.statsY.getValueFloat();

        MemoryOptimizer mem = CasxDuoisClient.getInstance().getMemoryOptimizer();
        long usedMB = mem != null ? mem.getUsedMemoryMB() : (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024;
        long maxMB = mem != null ? mem.getMaxMemoryMB() : Runtime.getRuntime().maxMemory() / 1024 / 1024;
        int memPercent = mem != null ? mem.getMemoryPercent() : (int) ((usedMB * 100) / maxMB);

        float cardW = 135f;
        float cardH = 26f;

        if (drawBg) {
            RenderUtils.drawRoundedRect(x, y, cardW, cardH, 5f, bgCol);
        }
        if (drawBorder) {
            RenderUtils.drawRoundedOutline(x, y, cardW, cardH, 5f, 0.8f, ColorUtils.BORDER_GLASS.getRGB());
        }

        String ramText = "RAM: " + usedMB + "M / " + maxMB + "M (" + memPercent + "%)";
        fr.drawString(ramText, (int) (x + 8f), (int) (y + 5f), ColorUtils.TEXT_SILVER.getRGB());

        float barX = x + 8f;
        float barY = y + 17f;
        float barW = cardW - 16f;
        float barH = 3f;
        RenderUtils.drawRoundedRect(barX, barY, barW, barH, 1.5f, ColorUtils.BG_INPUT.getRGB());
        float progress = Math.min(1.0f, Math.max(0.0f, (float) memPercent / 100.0f));
        if (progress > 0.01f) {
            if (memPercent > 85) {
                RenderUtils.drawRoundedRect(barX, barY, barW * progress, barH, 1.5f, ColorUtils.ACCENT_RED.getRGB());
            } else {
                RenderUtils.drawRoundedGradientRect(barX, barY, barW * progress, barH, 1.5f, ColorUtils.ACCENT_START.getRGB(), ColorUtils.ACCENT_END.getRGB());
            }
        }
    }

    private void renderCoords(HUDModule mod, ScaledResolution sr, FontRenderer fr, boolean drawBg, boolean drawBorder, int bgCol) {
        float x = mod.coordsX.getValueFloat();
        float y = mod.coordsY.getValueFloat();

        if (x < 0) x = 8f;
        if (y < 0) y = sr.getScaledHeight() - 32f;

        int posX = (int) mc.player.posX;
        int posY = (int) mc.player.posY;
        int posZ = (int) mc.player.posZ;

        String coords = String.format("XYZ: %d / %d / %d", posX, posY, posZ);

        String biomeName = mc.world != null ? mc.world.getBiome(mc.player.getPosition()).getBiomeName() : "Unknown";
        EnumFacing facing = mc.player.getHorizontalFacing();
        String directionStr = facing.getName().substring(0, 1).toUpperCase();
        String subInfo = "Biome: " + biomeName + " (" + directionStr + ")";

        float boxW = Math.max(fr.getStringWidth(coords), fr.getStringWidth(subInfo)) + 16f;
        float boxH = 24f;

        if (drawBg) {
            RenderUtils.drawRoundedRect(x, y, boxW, boxH, 5f, bgCol);
        }
        if (drawBorder) {
            RenderUtils.drawRoundedOutline(x, y, boxW, boxH, 5f, 0.8f, ColorUtils.BORDER_GLASS.getRGB());
        }

        fr.drawStringWithShadow(coords, (int) (x + 8f), (int) (y + 4f), ColorUtils.TEXT_WHITE.getRGB());
        fr.drawString(subInfo, (int) (x + 8f), (int) (y + 14f), ColorUtils.TEXT_MUTED.getRGB());
    }

    private void renderModules(HUDModule mod, ScaledResolution sr, FontRenderer fr, boolean drawBg, boolean drawBorder, int bgCol) {
        List<Module> activeMods = new ArrayList<>();
        for (Module m : CasxDuoisClient.getInstance().getModuleManager().getModules()) {
            if (m.isToggled() && !m.getName().equalsIgnoreCase("HUD") && !m.getName().equalsIgnoreCase("ClickGUI")) {
                activeMods.add(m);
            }
        }

        if (activeMods.isEmpty()) return;

        float startX = mod.modulesX.getValueFloat();
        float curY = mod.modulesY.getValueFloat();
        if (curY < 0) curY = 8f;

        for (Module m : activeMods) {
            String name = m.getVietnameseName();
            int textW = fr.getStringWidth(name);
            float cardW = textW + 16f;
            float cardH = 16f;

            float x = (startX < 0) ? (sr.getScaledWidth() - cardW - 8f) : startX;

            if (drawBg) {
                RenderUtils.drawRoundedRect(x, curY, cardW, cardH, 4f, bgCol);
            }
            if (drawBorder) {
                RenderUtils.drawRoundedOutline(x, curY, cardW, cardH, 4f, 0.8f, ColorUtils.BORDER_GLASS.getRGB());
            }

            // Vạch Gradient Accent bên phải mỗi module
            RenderUtils.drawAccentIndicator(x + cardW - 3.5f, curY + 3f, 2f, 10f, 1f);
            fr.drawStringWithShadow(name, (int) (x + 6f), (int) (curY + 4f), ColorUtils.TEXT_WHITE.getRGB());

            curY += 19f;
        }
    }

    private void renderArmor(HUDModule mod, ScaledResolution sr, FontRenderer fr, boolean drawBg, boolean drawBorder, int bgCol) {
        float x = mod.armorX.getValueFloat();
        float y = mod.armorY.getValueFloat();

        if (x < 0) x = (sr.getScaledWidth() / 2f) + 95f;
        if (y < 0) y = sr.getScaledHeight() - 56f;

        List<ItemStack> items = new ArrayList<>();
        // 4 món giáp: Mũ (Slot 3), Áo (Slot 2), Quần (Slot 1), Giày (Slot 0)
        for (int slot = 3; slot >= 0; slot--) {
            ItemStack is = mc.player.inventory.armorItemInSlot(slot);
            if (is != null && !is.isEmpty()) {
                items.add(is);
            }
        }
        ItemStack held = mc.player.getHeldItemMainhand();
        if (held != null && !held.isEmpty()) {
            items.add(held);
        }

        if (items.isEmpty()) return;

        float cardW = (items.size() * 22f) + 8f;
        float cardH = 26f;

        if (drawBg) {
            RenderUtils.drawRoundedRect(x, y, cardW, cardH, 5f, bgCol);
        }
        if (drawBorder) {
            RenderUtils.drawRoundedOutline(x, y, cardW, cardH, 5f, 0.8f, ColorUtils.BORDER_GLASS.getRGB());
        }

        float curX = x + 4f;
        RenderHelper.enableGUIStandardItemLighting();
        for (ItemStack is : items) {
            mc.getRenderItem().renderItemAndEffectIntoGUI(is, (int) curX, (int) (y + 5f));
            mc.getRenderItem().renderItemOverlayIntoGUI(fr, is, (int) curX, (int) (y + 5f), null);
            curX += 22f;
        }
        RenderHelper.disableStandardItemLighting();
    }

    private void renderSpeed(HUDModule mod, ScaledResolution sr, FontRenderer fr, boolean drawBg, boolean drawBorder, int bgCol) {
        float x = mod.speedX.getValueFloat();
        float y = mod.speedY.getValueFloat();
        if (x < 0) x = 8f;
        if (y < 0) y = 74f;

        double dx = mc.player.posX - mc.player.prevPosX;
        double dz = mc.player.posZ - mc.player.prevPosZ;
        double bps = Math.sqrt(dx * dx + dz * dz) * 20.0;

        String speedText = String.format("Tốc độ: %.1f BPS", bps);
        float boxW = fr.getStringWidth(speedText) + 16f;
        float boxH = 18f;

        if (drawBg) {
            RenderUtils.drawRoundedRect(x, y, boxW, boxH, 4f, bgCol);
        }
        if (drawBorder) {
            RenderUtils.drawRoundedOutline(x, y, boxW, boxH, 4f, 0.8f, ColorUtils.BORDER_GLASS.getRGB());
        }

        fr.drawStringWithShadow(speedText, (int) (x + 8f), (int) (y + 5f), ColorUtils.TEXT_WHITE.getRGB());
    }

    // ─────────────────────────────────────────────────────────────────
    // PHỤC VỤ TRÌNH CHỈNH SỬA VỊ TRÍ HUD (GuiHudEditor)
    // ─────────────────────────────────────────────────────────────────

    public void renderWithEditorBounds(GuiHudEditor editor, int mouseX, int mouseY) {
        HUDModule hudModule = CasxDuoisClient.getInstance().getModuleManager().getModule(HUDModule.class);
        if (hudModule == null) return;

        ScaledResolution sr = new ScaledResolution(mc);
        FontRenderer fr = mc.fontRenderer;

        // Vẽ từng widget và kèm khung định vị
        drawEditorBox("watermark", "Khung Tài Khoản & Server", getWidgetBounds("watermark", sr, fr), mouseX, mouseY);
        drawEditorBox("stats", "Khung Thông Số RAM", getWidgetBounds("stats", sr, fr), mouseX, mouseY);
        drawEditorBox("coords", "Khung Tọa Độ & Biome", getWidgetBounds("coords", sr, fr), mouseX, mouseY);
        drawEditorBox("modules", "Khung Tính Năng (Modules)", getWidgetBounds("modules", sr, fr), mouseX, mouseY);
        drawEditorBox("armor", "Khung Độ Bền Giáp & Vũ Khí", getWidgetBounds("armor", sr, fr), mouseX, mouseY);
        if (hudModule.showSpeed.isEnabled()) {
            drawEditorBox("speed", "Khung Tốc Độ (BPS)", getWidgetBounds("speed", sr, fr), mouseX, mouseY);
        }
    }

    private void drawEditorBox(String id, String label, float[] b, int mouseX, int mouseY) {
        boolean hover = mouseX >= b[0] && mouseX <= b[0] + b[2] && mouseY >= b[1] && mouseY <= b[1] + b[3];
        int bgCol = hover ? ColorUtils.ACCENT_SOFT.getRGB() : ColorUtils.BG_GLASS.getRGB();
        int borderCol = hover ? ColorUtils.BORDER_ACTIVE.getRGB() : ColorUtils.BORDER_GLASS.getRGB();

        RenderUtils.drawRoundedRect(b[0], b[1], b[2], b[3], 5f, bgCol);
        RenderUtils.drawRoundedOutline(b[0], b[1], b[2], b[3], 5f, 1.0f, borderCol);

        String tag = (hover ? "§f✥ " : "§7✥ ") + label;
        mc.fontRenderer.drawStringWithShadow(tag, (int) (b[0] + 6f), (int) (b[1] + (b[3] - 8f) / 2f), ColorUtils.TEXT_WHITE.getRGB());
    }

    public float[] getWidgetBounds(String widgetId, ScaledResolution sr, FontRenderer fr) {
        HUDModule mod = CasxDuoisClient.getInstance().getModuleManager().getModule(HUDModule.class);
        if ("watermark".equalsIgnoreCase(widgetId)) {
            float x = mod.watermarkX.getValueFloat();
            float y = mod.watermarkY.getValueFloat();
            return new float[]{x, y, 140f, 30f};
        } else if ("stats".equalsIgnoreCase(widgetId)) {
            float x = mod.statsX.getValueFloat();
            float y = mod.statsY.getValueFloat();
            return new float[]{x, y, 135f, 26f};
        } else if ("coords".equalsIgnoreCase(widgetId)) {
            float x = mod.coordsX.getValueFloat();
            float y = mod.coordsY.getValueFloat();
            if (x < 0) x = 8f;
            if (y < 0) y = sr.getScaledHeight() - 32f;
            return new float[]{x, y, 150f, 24f};
        } else if ("modules".equalsIgnoreCase(widgetId)) {
            float x = mod.modulesX.getValueFloat();
            float y = mod.modulesY.getValueFloat();
            if (x < 0) x = sr.getScaledWidth() - 110f;
            if (y < 0) y = 8f;
            return new float[]{x, y, 105f, 54f};
        } else if ("armor".equalsIgnoreCase(widgetId)) {
            float x = mod.armorX.getValueFloat();
            float y = mod.armorY.getValueFloat();
            if (x < 0) x = (sr.getScaledWidth() / 2f) + 95f;
            if (y < 0) y = sr.getScaledHeight() - 56f;
            return new float[]{x, y, 118f, 26f};
        } else if ("speed".equalsIgnoreCase(widgetId)) {
            float x = mod.speedX.getValueFloat();
            float y = mod.speedY.getValueFloat();
            if (x < 0) x = 8f;
            if (y < 0) y = 74f;
            return new float[]{x, y, 110f, 18f};
        }
        return new float[]{8f, 8f, 100f, 24f};
    }

    public String findClickedWidget(int mouseX, int mouseY) {
        ScaledResolution sr = new ScaledResolution(mc);
        FontRenderer fr = mc.fontRenderer;
        String[] widgets = {"watermark", "stats", "coords", "modules", "armor", "speed"};
        for (String id : widgets) {
            float[] b = getWidgetBounds(id, sr, fr);
            if (mouseX >= b[0] && mouseX <= b[0] + b[2] && mouseY >= b[1] && mouseY <= b[1] + b[3]) {
                return id;
            }
        }
        return null;
    }

    public float[] getWidgetPosition(String widgetId) {
        ScaledResolution sr = new ScaledResolution(mc);
        FontRenderer fr = mc.fontRenderer;
        float[] b = getWidgetBounds(widgetId, sr, fr);
        return new float[]{b[0], b[1]};
    }
}
