package com.casxduois.client.gui.account;

import com.casxduois.client.CasxDuoisClient;
import com.casxduois.client.account.Account;
import com.casxduois.client.account.AccountManager;
import com.casxduois.client.gui.menu.CasxDuoisMainMenu;
import com.casxduois.client.util.ColorUtils;
import com.casxduois.client.util.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.io.IOException;
import java.util.List;

/**
 * GuiInGameAccountSwitcher: Giao diện đổi tài khoản in-game đồng bộ với Client Launcher
 */
public class GuiInGameAccountSwitcher extends GuiScreen {

    private static class GuiClientButton extends GuiButton {
        private final boolean isPrimary;

        public GuiClientButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText, boolean isPrimary) {
            super(buttonId, x, y, widthIn, heightIn, buttonText);
            this.isPrimary = isPrimary;
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (!this.visible) return;

            this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;

            if (isPrimary) {
                // Primary button: Gradient Accent nổi bật
                if (this.hovered) {
                    RenderUtils.drawRoundedGradientRect(this.x, this.y, this.width, this.height, 5f, ColorUtils.ACCENT_START.getRGB(), ColorUtils.ACCENT_END.getRGB());
                    RenderUtils.drawRoundedOutline(this.x, this.y, this.width, this.height, 5f, 0.8f, ColorUtils.PRIMARY_WHITE.getRGB());
                } else {
                    RenderUtils.drawRoundedRect(this.x, this.y, this.width, this.height, 5f, ColorUtils.BG_CARD_HOVER.getRGB());
                    RenderUtils.drawRoundedOutline(this.x, this.y, this.width, this.height, 5f, 0.8f, ColorUtils.BORDER_ACTIVE.getRGB());
                }
                this.drawCenteredString(mc.fontRenderer, this.displayString, this.x + this.width / 2, this.y + (this.height - 8) / 2, ColorUtils.PRIMARY_WHITE.getRGB());
            } else {
                // Secondary button: Glass button với viền tinh tế
                int bgColor = this.hovered ? ColorUtils.BG_CARD_HOVER.getRGB() : ColorUtils.BG_INPUT.getRGB();
                RenderUtils.drawRoundedRect(this.x, this.y, this.width, this.height, 5f, bgColor);
                RenderUtils.drawRoundedOutline(this.x, this.y, this.width, this.height, 5f, 0.8f,
                        this.hovered ? ColorUtils.BORDER_HOVER.getRGB() : ColorUtils.BORDER_GLASS.getRGB());
                int textColor = this.hovered ? ColorUtils.PRIMARY_WHITE.getRGB() : ColorUtils.TEXT_SILVER.getRGB();
                this.drawCenteredString(mc.fontRenderer, this.displayString, this.x + this.width / 2, this.y + (this.height - 8) / 2, textColor);
            }
        }
    }

    private final GuiScreen parentScreen;
    private GuiTextField usernameField;
    private String statusMessage = "§7Nhập tên tài khoản mới hoặc chọn từ danh sách";

    public GuiInGameAccountSwitcher(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();

        int centerX = width / 2;
        int centerY = height / 2;
        int shiftY = (height < 250) ? (height < 220 ? -16 : -8) : 0;

        // Ô nhập tên tài khoản + Nút Dán
        usernameField = new GuiTextField(0, fontRenderer, centerX - 120, centerY - 64 + shiftY, 175, 20);
        usernameField.setMaxStringLength(16);
        usernameField.setFocused(true);
        usernameField.setText(mc.getSession() != null ? mc.getSession().getUsername() : "");

        buttonList.add(new GuiClientButton(4, centerX + 62, centerY - 64 + shiftY, 58, 20, "Dán", false));

        // Nút thao tác
        buttonList.add(new GuiClientButton(1, centerX - 120, centerY - 36 + shiftY, 116, 22, "Đổi ngay", true));
        buttonList.add(new GuiClientButton(2, centerX + 4, centerY - 36 + shiftY, 116, 22, "Đổi & Vào lại", false));
        buttonList.add(new GuiClientButton(3, centerX - 120, centerY + 94 + shiftY, 240, 22, "Quay lại game", false));

        // Kích hoạt Shader Blur phần cứng khi mở
        if (mc.world != null && OpenGlHelper.shadersSupported) {
            try {
                mc.entityRenderer.loadShader(new ResourceLocation("shaders/post/blur.json"));
            } catch (Throwable ignored) {}
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        // Tắt Shader Blur khi đóng
        if (mc.entityRenderer != null) {
            try {
                mc.entityRenderer.stopUseShader();
            } catch (Throwable ignored) {}
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        AccountManager manager = CasxDuoisClient.getInstance().getAccountManager();
        String username = usernameField.getText().trim();

        if (button.id == 4) { // Dán từ clipboard
            String clip = GuiScreen.getClipboardString();
            if (clip != null && !clip.isEmpty()) {
                clip = clip.replaceAll("[\r\n\t]", "").trim();
                if (clip.length() > 16) clip = clip.substring(0, 16);
                usernameField.setText(clip);
                statusMessage = "§fĐã dán: " + clip;
            }
            return;
        }

        if (button.id == 1) { // Đổi ngay
            if (!username.isEmpty()) {
                if (manager.loginOffline(username)) {
                    statusMessage = "§fĐã chuyển sang tài khoản: " + username;
                    try { org.lwjgl.opengl.Display.setTitle(username); } catch (Throwable ignored) {}
                } else {
                    statusMessage = "§7Không thể đổi tài khoản.";
                }
            }
        } else if (button.id == 2) { // Đổi & Vào lại server
            if (!username.isEmpty()) {
                if (manager.loginOffline(username)) {
                    try { org.lwjgl.opengl.Display.setTitle(username); } catch (Throwable ignored) {}
                    ServerData currentServer = mc.getCurrentServerData();
                    if (currentServer != null) {
                        mc.world.sendQuittingDisconnectingPacket();
                        mc.loadWorld(null);
                        mc.displayGuiScreen(new GuiConnecting(parentScreen, mc, currentServer));
                        return;
                    } else {
                        statusMessage = "§fĐã đổi tài khoản (Chơi đơn).";
                    }
                }
            }
        } else if (button.id == 3) { // Quay lại
            mc.displayGuiScreen(parentScreen);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parentScreen);
            return;
        }
        if (keyCode == Keyboard.KEY_RETURN) {
            actionPerformed(buttonList.get(1)); // Button 1: Đổi ngay
            return;
        }
        if (isKeyComboCtrlV(keyCode) || typedChar == 22 || (isCtrlKeyDown() && (keyCode == Keyboard.KEY_V || typedChar == 'v' || typedChar == 'V'))) {
            String clip = GuiScreen.getClipboardString();
            if (clip != null && !clip.isEmpty()) {
                clip = clip.replaceAll("[\r\n\t]", "").trim();
                if (clip.length() > 16) clip = clip.substring(0, 16);
                usernameField.setText(clip);
                statusMessage = "§fĐã dán: " + clip;
                return;
            }
        }
        usernameField.textboxKeyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        // Chuột phải vào ô nhập username -> Tự động dán từ clipboard
        if (mouseButton == 1 && mouseX >= usernameField.x && mouseX <= usernameField.x + usernameField.width && mouseY >= usernameField.y && mouseY <= usernameField.y + usernameField.height) {
            String clip = GuiScreen.getClipboardString();
            if (clip != null && !clip.isEmpty()) {
                clip = clip.replaceAll("[\r\n\t]", "").trim();
                if (clip.length() > 16) clip = clip.substring(0, 16);
                usernameField.setText(clip);
                statusMessage = "§fĐã dán: " + clip;
                return;
            }
        }

        usernameField.mouseClicked(mouseX, mouseY, mouseButton);

        // Click chọn tài khoản trong danh sách
        int centerX = width / 2;
        int centerY = height / 2;
        int shiftY = (height < 250) ? (height < 220 ? -16 : -8) : 0;
        List<Account> accounts = CasxDuoisClient.getInstance().getAccountManager().getAccounts();
        int listY = centerY + 3 + shiftY;

        for (int i = 0; i < Math.min(accounts.size(), 3); i++) {
            Account acc = accounts.get(i);
            if (mouseX >= centerX - 120 && mouseX <= centerX + 120 && mouseY >= listY && mouseY <= listY + 20) {
                usernameField.setText(acc.getUsername());
                CasxDuoisClient.getInstance().getAccountManager().login(acc);
                try { org.lwjgl.opengl.Display.setTitle(acc.getUsername()); } catch (Throwable ignored) {}
                statusMessage = "§fĐã chọn: " + acc.getUsername();
                break;
            }
            listY += 24;
        }
    }

    @Override
    public void updateScreen() {
        usernameField.updateCursorCounter();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (mc.world != null) {
            drawDefaultBackground();
        } else {
            try {
                ResourceLocation bg = CasxDuoisMainMenu.getBackgroundTexture();
                if (bg != null) {
                    float imgW = CasxDuoisMainMenu.bgWidth;
                    float imgH = CasxDuoisMainMenu.bgHeight;
                    float imgAspect = imgW / imgH;
                    float screenAspect = (float) width / (float) height;
                    float drawW, drawH, drawX, drawY;

                    if (screenAspect > imgAspect) {
                        drawW = width;
                        drawH = width / imgAspect;
                        drawX = 0;
                        drawY = (height - drawH) / 2f;
                    } else {
                        drawH = height;
                        drawW = height * imgAspect;
                        drawX = (width - drawW) / 2f;
                        drawY = 0;
                    }

                    GlStateManager.enableTexture2D();
                    GlStateManager.enableBlend();
                    GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                    mc.getTextureManager().bindTexture(bg);
                    drawScaledCustomSizeModalRect((int) drawX, (int) drawY, 0, 0, (int) imgW, (int) imgH, (int) drawW, (int) drawH, imgW, imgH);
                } else {
                    RenderUtils.drawGradientRect(0, 0, width, height, new Color(11, 16, 28).getRGB(), new Color(4, 5, 8).getRGB());
                }
            } catch (Throwable ignored) {
                RenderUtils.drawGradientRect(0, 0, width, height, new Color(11, 16, 28).getRGB(), new Color(4, 5, 8).getRGB());
            }
            RenderUtils.drawGradientRect(0, 0, width, height, new Color(7, 10, 18, 170).getRGB(), new Color(3, 4, 7, 230).getRGB());
        }

        int centerX = width / 2;
        int centerY = height / 2;
        int shiftY = (height < 250) ? (height < 220 ? -16 : -8) : 0;
        float modalWidth = Math.min(280f, width - 20f);
        float modalHeight = Math.min(246f, height - 16f);

        float modalX = centerX - modalWidth / 2f;
        float modalY = centerY - modalHeight / 2f + shiftY;

        // Card nền chính Obsidian Glassmorphism
        RenderUtils.drawGlassPanel(modalX, modalY, modalWidth, modalHeight, 10f);

        // Header Modal: Accent Dot & Tiêu đề
        String title = "Đổi Tài Khoản";
        RenderUtils.drawAccentDot(centerX - fontRenderer.getStringWidth(title) / 2f - 7f, modalY + 16f, 3f, true);
        fontRenderer.drawStringWithShadow(title, centerX - fontRenderer.getStringWidth(title) / 2 + 4, (int) (modalY + 12), ColorUtils.PRIMARY_WHITE.getRGB());

        // Thông tin tài khoản hiện tại
        String currentNick = "Hiện tại: " + (mc.getSession() != null ? mc.getSession().getUsername() : "Player");
        float maxNickW = modalWidth - 100f;
        if (fontRenderer.getStringWidth(currentNick) > maxNickW) {
            currentNick = fontRenderer.trimStringToWidth(currentNick, (int) (maxNickW - 8f)) + "..";
        }
        fontRenderer.drawString(currentNick, centerX - 120, centerY - 82 + shiftY, ColorUtils.TEXT_MUTED.getRGB());

        RenderUtils.drawCircle(centerX + 64f, centerY - 79f + shiftY, 2.5f, ColorUtils.ACCENT_GREEN.getRGB());
        fontRenderer.drawString("Sẵn sàng", centerX + 70, centerY - 82 + shiftY, ColorUtils.TEXT_SILVER.getRGB());

        // Vẽ ô nhập liệu
        usernameField.drawTextBox();

        // Tiêu đề danh sách: "Tài khoản"
        int listY = centerY + 3 + shiftY;
        fontRenderer.drawString("Tài khoản đã lưu:", centerX - 120, centerY - 8 + shiftY, ColorUtils.TEXT_SILVER.getRGB());

        // Danh sách tài khoản đã lưu
        List<Account> accounts = CasxDuoisClient.getInstance().getAccountManager().getAccounts();
        String currentUsername = mc.getSession() != null ? mc.getSession().getUsername() : "";
        for (int i = 0; i < Math.min(accounts.size(), 3); i++) {
            Account acc = accounts.get(i);
            boolean isCurrent = acc.getUsername() != null && acc.getUsername().equalsIgnoreCase(currentUsername);
            boolean isHover = mouseX >= centerX - 120 && mouseX <= centerX + 120 && mouseY >= listY && mouseY <= listY + 20;
            int cardColor = isHover ? ColorUtils.BG_CARD_HOVER.getRGB() : ColorUtils.BG_CARD.getRGB();

            RenderUtils.drawRoundedRect(centerX - 120, listY, 240, 20, 5, cardColor);
            RenderUtils.drawRoundedOutline(centerX - 120, listY, 240, 20, 5, 0.8f,
                    isHover ? ColorUtils.BORDER_HOVER.getRGB() : (isCurrent ? ColorUtils.BORDER_ACTIVE.getRGB() : ColorUtils.BORDER_GLASS.getRGB()));

            if (isCurrent) {
                RenderUtils.drawAccentIndicator(centerX - 120 + 2f, listY + 4f, 2.5f, 12f, 1.25f);
            }

            fontRenderer.drawStringWithShadow(acc.getUsername(), centerX - 110, listY + 6,
                    isHover ? ColorUtils.PRIMARY_WHITE.getRGB() : ColorUtils.TEXT_SILVER.getRGB());
            fontRenderer.drawString("[Offline]", centerX + 72, listY + 6, ColorUtils.TEXT_MUTED.getRGB());

            listY += 24;
        }

        // Thông báo trạng thái ở chân modal
        String msg = statusMessage;
        if (fontRenderer.getStringWidth(msg) > modalWidth - 20f) {
            msg = fontRenderer.trimStringToWidth(msg, (int) (modalWidth - 26f)) + "..";
        }
        fontRenderer.drawString(msg, centerX - fontRenderer.getStringWidth(msg) / 2, centerY + 78 + shiftY, ColorUtils.TEXT_SILVER.getRGB());

        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
