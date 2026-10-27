package com.casxduois.client.gui.hud;

import com.casxduois.client.CasxDuoisClient;
import com.casxduois.client.modules.render.HUDModule;
import com.casxduois.client.util.ColorUtils;
import com.casxduois.client.util.RenderUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.io.IOException;

/**
 * GuiHudEditor: Trình chỉnh sửa vị trí các khung HUD trực quan bằng chuột (Drag & Drop)
 * Cho phép kéo thả các thẻ Watermark, RAM, Tọa độ, Modules, Armor trực tiếp trên màn hình
 */
public class GuiHudEditor extends GuiScreen {

    private final GuiScreen parentScreen;
    private HUDModule hudModule;

    // Trạng thái kéo thả
    private String draggingId = null;
    private float dragOffsetX = 0;
    private float dragOffsetY = 0;

    public GuiHudEditor(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.hudModule = CasxDuoisClient.getInstance().getModuleManager().getModule(HUDModule.class);

        // Kích hoạt Shader Blur nhẹ khi vào trình chỉnh sửa
        if (mc.world != null && OpenGlHelper.shadersSupported) {
            try {
                mc.entityRenderer.loadShader(new ResourceLocation("shaders/post/blur.json"));
            } catch (Throwable ignored) {}
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        if (CasxDuoisClient.getInstance().getConfigManager() != null) {
            CasxDuoisClient.getInstance().getConfigManager().saveConfig();
        }
        if (mc.entityRenderer != null) {
            try {
                mc.entityRenderer.stopUseShader();
            } catch (Throwable ignored) {}
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // 1. Phủ lớp kính tối Obsidian mờ toàn màn hình để dễ căn chỉnh
        RenderUtils.drawGradientRect(0, 0, width, height, new Color(7, 10, 16, 160).getRGB(), new Color(3, 4, 7, 210).getRGB());

        ScaledResolution sr = new ScaledResolution(mc);
        if (hudModule == null) return;

        // 2. Vẽ các khung HUD kèm khung định vị (Bounding Box)
        CasxDuoisHUD hud = CasxDuoisClient.getInstance().getHud();
        if (hud != null) {
            hud.renderWithEditorBounds(this, mouseX, mouseY);
        }

        // 3. Thanh công cụ Toolbar trên cùng
        float barW = Math.min(360f, width - 20f);
        float barH = 36f;
        float barX = (width - barW) / 2f;
        float barY = 12f;

        RenderUtils.drawGlassPanel(barX, barY, barW, barH, 6f);

        RenderUtils.drawAccentDot(barX + 14f, barY + 10f, 2.5f, true);
        String title = "Chỉnh sửa vị trí HUD (Kéo thẻ để di chuyển)";
        fontRenderer.drawStringWithShadow(title, (int) (barX + (barW - fontRenderer.getStringWidth(title)) / 2f), (int) (barY + 6f), ColorUtils.PRIMARY_WHITE.getRGB());

        // Nút: Đặt lại vị trí
        float btnResetW = 86f;
        float btnResetX = barX + 8f;
        float btnY = barY + 18f;
        boolean hoverReset = mouseX >= btnResetX && mouseX <= btnResetX + btnResetW && mouseY >= btnY && mouseY <= btnY + 14f;
        RenderUtils.drawRoundedRect(btnResetX, btnY, btnResetW, 14f, 3, hoverReset ? ColorUtils.BG_CARD_HOVER.getRGB() : ColorUtils.BG_INPUT.getRGB());
        RenderUtils.drawRoundedOutline(btnResetX, btnY, btnResetW, 14f, 3, 0.8f, hoverReset ? ColorUtils.BORDER_HOVER.getRGB() : ColorUtils.BORDER_GLASS.getRGB());
        fontRenderer.drawString("Đặt lại vị trí", (int) (btnResetX + (btnResetW - fontRenderer.getStringWidth("Đặt lại vị trí")) / 2f), (int) (btnY + 3f), ColorUtils.TEXT_SILVER.getRGB());

        // Nút: Lưu & Đóng
        float btnSaveW = 76f;
        float btnSaveX = barX + barW - btnSaveW - 8f;
        boolean hoverSave = mouseX >= btnSaveX && mouseX <= btnSaveX + btnSaveW && mouseY >= btnY && mouseY <= btnY + 14f;
        if (hoverSave) {
            RenderUtils.drawRoundedGradientRect(btnSaveX, btnY, btnSaveW, 14f, 3f, ColorUtils.ACCENT_START.getRGB(), ColorUtils.ACCENT_END.getRGB());
            RenderUtils.drawRoundedOutline(btnSaveX, btnY, btnSaveW, 14f, 3f, 0.8f, ColorUtils.PRIMARY_WHITE.getRGB());
        } else {
            RenderUtils.drawRoundedRect(btnSaveX, btnY, btnSaveW, 14f, 3f, ColorUtils.BG_INPUT.getRGB());
            RenderUtils.drawRoundedOutline(btnSaveX, btnY, btnSaveW, 14f, 3f, 0.8f, ColorUtils.BORDER_ACTIVE.getRGB());
        }
        fontRenderer.drawString("Lưu & Đóng", (int) (btnSaveX + (btnSaveW - fontRenderer.getStringWidth("Lưu & Đóng")) / 2f), (int) (btnY + 3f), ColorUtils.PRIMARY_WHITE.getRGB());

        // Hướng dẫn nhanh ở đáy màn hình
        String guide = "Giữ chuột trái để kéo thả khung thẻ · Bấm ESC hoặc nút Lưu để hoàn tất";
        fontRenderer.drawStringWithShadow(guide, (width - fontRenderer.getStringWidth(guide)) / 2, height - 16, ColorUtils.TEXT_MUTED.getRGB());

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0) {
            // Kiểm tra click vào nút Toolbar
            float barW = Math.min(360f, width - 20f);
            float barX = (width - barW) / 2f;
            float barY = 12f;
            float btnResetW = 86f;
            float btnResetX = barX + 8f;
            float btnY = barY + 18f;

            if (mouseX >= btnResetX && mouseX <= btnResetX + btnResetW && mouseY >= btnY && mouseY <= btnY + 14f) {
                if (hudModule != null) {
                    hudModule.resetPositions();
                }
                return;
            }

            float btnSaveW = 76f;
            float btnSaveX = barX + barW - btnSaveW - 8f;
            if (mouseX >= btnSaveX && mouseX <= btnSaveX + btnSaveW && mouseY >= btnY && mouseY <= btnY + 14f) {
                mc.displayGuiScreen(parentScreen);
                return;
            }

            // Bắt đầu kéo thả khung được click
            CasxDuoisHUD hud = CasxDuoisClient.getInstance().getHud();
            if (hud != null) {
                String clickedWidget = hud.findClickedWidget(mouseX, mouseY);
                if (clickedWidget != null) {
                    this.draggingId = clickedWidget;
                    float[] pos = hud.getWidgetPosition(clickedWidget);
                    this.dragOffsetX = mouseX - pos[0];
                    this.dragOffsetY = mouseY - pos[1];
                    return;
                }
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (clickedMouseButton == 0 && draggingId != null && hudModule != null) {
            float newX = Math.max(2, Math.min(width - 50, mouseX - dragOffsetX));
            float newY = Math.max(2, Math.min(height - 25, mouseY - dragOffsetY));

            if ("watermark".equalsIgnoreCase(draggingId)) {
                hudModule.watermarkX.setValue((double) newX);
                hudModule.watermarkY.setValue((double) newY);
            } else if ("stats".equalsIgnoreCase(draggingId)) {
                hudModule.statsX.setValue((double) newX);
                hudModule.statsY.setValue((double) newY);
            } else if ("coords".equalsIgnoreCase(draggingId)) {
                hudModule.coordsX.setValue((double) newX);
                hudModule.coordsY.setValue((double) newY);
            } else if ("modules".equalsIgnoreCase(draggingId)) {
                hudModule.modulesX.setValue((double) newX);
                hudModule.modulesY.setValue((double) newY);
            } else if ("armor".equalsIgnoreCase(draggingId)) {
                hudModule.armorX.setValue((double) newX);
                hudModule.armorY.setValue((double) newY);
            } else if ("speed".equalsIgnoreCase(draggingId)) {
                hudModule.speedX.setValue((double) newX);
                hudModule.speedY.setValue((double) newY);
            }
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        this.draggingId = null;
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RETURN) {
            mc.displayGuiScreen(parentScreen);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }
}
