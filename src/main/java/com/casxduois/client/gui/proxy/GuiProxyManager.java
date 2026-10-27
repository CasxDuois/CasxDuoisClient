package com.casxduois.client.gui.proxy;

import com.casxduois.client.proxy.ProxyManager;
import com.casxduois.client.util.ColorUtils;
import com.casxduois.client.util.RenderUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.io.IOException;

/**
 * GuiProxyManager: Giao diện quản lý Proxy và AutoVPN cao cấp
 * Đồng bộ với phong cách Obsidian Frosted Glass của CasxDuois Client
 */
public class GuiProxyManager extends GuiScreen {

    private final GuiScreen parentScreen;
    private final ProxyManager proxyManager = ProxyManager.getInstance();

    private GuiTextField hostField;
    private GuiTextField portField;
    private GuiTextField userField;
    private GuiTextField passField;

    private float panelW;
    private float panelH;
    private float panelX;
    private float panelY;

    private String testStatus = null;
    private boolean isSearchingVN = false;
    private String vnStatusMessage = null;

    public GuiProxyManager(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private void updateLayout() {
        panelW = Math.min(380f, Math.max(300f, width - 24f));
        panelH = Math.min(315f, Math.max(240f, height - 24f));
        panelX = (width - panelW) / 2f;
        panelY = (height - panelH) / 2f;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        updateLayout();

        // Kích hoạt Shader Blur
        if (mc.world != null && OpenGlHelper.shadersSupported) {
            try {
                mc.entityRenderer.loadShader(new ResourceLocation("shaders/post/blur.json"));
            } catch (Throwable ignored) {}
        }

        int fieldX = (int) (panelX + 75f);
        int fieldW = (int) (panelW - 85f);

        // Host field
        hostField = new GuiTextField(0, fontRenderer, fieldX, (int) (panelY + 68f), fieldW - 55, 16);
        hostField.setMaxStringLength(128);
        hostField.setText(proxyManager.getHost());

        // Port field
        portField = new GuiTextField(1, fontRenderer, fieldX, (int) (panelY + 92f), 65, 16);
        portField.setMaxStringLength(5);
        portField.setText(String.valueOf(proxyManager.getPort()));

        // Username field
        userField = new GuiTextField(2, fontRenderer, fieldX, (int) (panelY + 116f), (fieldW / 2) - 10, 16);
        userField.setMaxStringLength(64);
        userField.setText(proxyManager.getUsername());

        // Password field
        int passX = fieldX + (fieldW / 2) + 15;
        passField = new GuiTextField(3, fontRenderer, passX, (int) (panelY + 116f), (fieldW / 2) - 25, 16);
        passField.setMaxStringLength(64);
        passField.setText(proxyManager.getPassword());
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        saveInputs();
        if (mc.entityRenderer != null) {
            try {
                mc.entityRenderer.stopUseShader();
            } catch (Throwable ignored) {}
        }
    }

    private void saveInputs() {
        if (hostField != null) proxyManager.setHost(hostField.getText().trim());
        if (portField != null) {
            try {
                proxyManager.setPort(Integer.parseInt(portField.getText().trim()));
            } catch (Exception ignored) {}
        }
        if (userField != null) proxyManager.setUsername(userField.getText().trim());
        if (passField != null) proxyManager.setPassword(passField.getText().trim());
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        updateLayout();

        // Nền tối Obsidian làm nổi bật
        RenderUtils.drawGradientRect(0, 0, width, height, new Color(7, 10, 16, 175).getRGB(), new Color(3, 4, 7, 220).getRGB());

        // 1. Khung chính Glassmorphism
        RenderUtils.drawShadow(panelX, panelY, panelW, panelH, 10f, 6, 75);
        RenderUtils.drawRoundedRect(panelX, panelY, panelW, panelH, 10f, new Color(11, 14, 22, 245).getRGB());
        RenderUtils.drawRoundedOutline(panelX, panelY, panelW, panelH, 10f, 1.0f, new Color(255, 255, 255, 35).getRGB());

        // Vạch sáng mép trên
        RenderUtils.drawHorizontalGradient(panelX + 25f, panelY, panelW - 50f, 1.2f,
                new Color(255, 255, 255, 65).getRGB(), new Color(255, 255, 255, 0).getRGB());

        // 2. Header Bar
        float headerH = 32f;
        RenderUtils.drawRoundedRect(panelX, panelY, panelW, headerH, 10f, ColorUtils.BG_CARD.getRGB());
        RenderUtils.drawRect(panelX, panelY + headerH, panelW, 1f, ColorUtils.BORDER_CARD.getRGB());

        // Nút Quay Lại ở góc trái
        float backW = 55f, backH = 18f;
        float backX = panelX + 8f, backY = panelY + 7f;
        boolean hoverBack = mouseX >= backX && mouseX <= backX + backW && mouseY >= backY && mouseY <= backY + backH;
        RenderUtils.drawRoundedRect(backX, backY, backW, backH, 4f, hoverBack ? ColorUtils.BG_CARD_HOVER.getRGB() : ColorUtils.BG_INPUT.getRGB());
        RenderUtils.drawRoundedOutline(backX, backY, backW, backH, 4f, 0.8f, hoverBack ? ColorUtils.BORDER_ACTIVE.getRGB() : ColorUtils.BORDER_CARD.getRGB());
        fontRenderer.drawString("← Trở lại", (int) (backX + 6f), (int) (backY + 5f), hoverBack ? ColorUtils.PRIMARY_WHITE.getRGB() : ColorUtils.TEXT_SILVER.getRGB());

        // Tiêu đề
        String title = "✦ QUẢN LÝ PROXY & AUTO-VPN";
        fontRenderer.drawStringWithShadow(title, (int) (panelX + 70f), (int) (panelY + 11f), ColorUtils.TEXT_WHITE.getRGB());

        // 3. Khung 1: Master Switch & Loại Proxy
        float curY = panelY + 40f;
        float cardH1 = 24f;
        RenderUtils.drawRoundedRect(panelX + 8f, curY, panelW - 16f, cardH1, 5f, ColorUtils.BG_CARD.getRGB());
        RenderUtils.drawRoundedOutline(panelX + 8f, curY, panelW - 16f, cardH1, 5f, 0.8f, ColorUtils.BORDER_CARD.getRGB());

        fontRenderer.drawString("Kích hoạt Proxy:", (int) (panelX + 16f), (int) (curY + 8f), ColorUtils.TEXT_SILVER.getRGB());

        // Nút Bật/Tắt Switch
        float swX = panelX + 105f;
        float swY = curY + 5f;
        boolean hoverSw = mouseX >= swX && mouseX <= swX + 26f && mouseY >= swY && mouseY <= swY + 14f;
        drawSwitch(swX, swY, proxyManager.isProxyEnabled(), hoverSw);

        // Nút Đổi Loại (SOCKS5 / HTTP/HTTPS / SOCKS4)
        float typeW = 84f, typeH = 16f;
        float typeX = panelX + panelW - typeW - 14f;
        float typeY = curY + 4f;
        boolean hoverType = mouseX >= typeX && mouseX <= typeX + typeW && mouseY >= typeY && mouseY <= typeY + typeH;
        RenderUtils.drawRoundedRect(typeX, typeY, typeW, typeH, 4f, hoverType ? ColorUtils.BG_CARD_HOVER.getRGB() : ColorUtils.BG_INPUT.getRGB());
        RenderUtils.drawRoundedOutline(typeX, typeY, typeW, typeH, 4f, 0.8f, hoverType ? ColorUtils.BORDER_ACTIVE.getRGB() : ColorUtils.BORDER_CARD.getRGB());
        String dispType = proxyManager.getProxyType();
        if ("HTTP".equalsIgnoreCase(dispType) || "HTTPS".equalsIgnoreCase(dispType)) dispType = "HTTP/HTTPS";
        String typeTxt = "◄ " + dispType + " ►";
        fontRenderer.drawString(typeTxt, (int) (typeX + (typeW - fontRenderer.getStringWidth(typeTxt)) / 2f), (int) (typeY + 4f), ColorUtils.TEXT_SILVER.getRGB());

        // 4. Khung 2: Các ô nhập IP, Cổng, User, Pass
        curY += 28f;
        float cardH2 = 78f;
        RenderUtils.drawRoundedRect(panelX + 8f, curY, panelW - 16f, cardH2, 5f, ColorUtils.BG_CARD.getRGB());
        RenderUtils.drawRoundedOutline(panelX + 8f, curY, panelW - 16f, cardH2, 5f, 0.8f, ColorUtils.BORDER_CARD.getRGB());

        fontRenderer.drawString("Địa chỉ IP:", (int) (panelX + 16f), (int) (curY + 4f), ColorUtils.TEXT_MUTED.getRGB());
        fontRenderer.drawString("Cổng (Port):", (int) (panelX + 16f), (int) (curY + 28f), ColorUtils.TEXT_MUTED.getRGB());
        fontRenderer.drawString("Tài khoản:", (int) (panelX + 16f), (int) (curY + 52f), ColorUtils.TEXT_MUTED.getRGB());
        fontRenderer.drawString("Pass:", (int) (panelX + (panelW / 2) + 20f), (int) (curY + 52f), ColorUtils.TEXT_MUTED.getRGB());

        // Nút Dán nhanh IP:Port
        float pasteW = 48f, pasteH = 16f;
        float pasteX = panelX + panelW - pasteW - 14f;
        float pasteY = curY + 2f;
        boolean hoverPaste = mouseX >= pasteX && mouseX <= pasteX + pasteW && mouseY >= pasteY && mouseY <= pasteY + pasteH;
        RenderUtils.drawRoundedRect(pasteX, pasteY, pasteW, pasteH, 3f, hoverPaste ? ColorUtils.BG_CARD_HOVER.getRGB() : ColorUtils.BG_INPUT.getRGB());
        RenderUtils.drawRoundedOutline(pasteX, pasteY, pasteW, pasteH, 3f, 0.8f, hoverPaste ? ColorUtils.BORDER_ACTIVE.getRGB() : ColorUtils.BORDER_CARD.getRGB());
        fontRenderer.drawString("Dán IP", (int) (pasteX + (pasteW - fontRenderer.getStringWidth("Dán IP")) / 2f), (int) (pasteY + 4f), hoverPaste ? ColorUtils.PRIMARY_WHITE.getRGB() : ColorUtils.TEXT_SILVER.getRGB());

        // Render các TextFields
        if (hostField != null) hostField.drawTextBox();
        if (portField != null) portField.drawTextBox();
        if (userField != null) userField.drawTextBox();
        if (passField != null) passField.drawTextBox();

        // 5. Khung 3: Chế độ AutoVPN & Quay vòng Proxy Pool
        curY += 82f;
        float cardH3 = 48f;
        RenderUtils.drawRoundedRect(panelX + 8f, curY, panelW - 16f, cardH3, 5f, ColorUtils.BG_CARD.getRGB());
        RenderUtils.drawRoundedOutline(panelX + 8f, curY, panelW - 16f, cardH3, 5f, 0.8f, ColorUtils.BORDER_CARD.getRGB());

        // Checkbox AutoRotate
        float chkX = panelX + 16f;
        float chkY = curY + 6f;
        boolean hoverChk = mouseX >= chkX && mouseX <= chkX + 12f && mouseY >= chkY && mouseY <= chkY + 12f;
        RenderUtils.drawRoundedRect(chkX, chkY, 12f, 12f, 3f, proxyManager.isAutoRotate() ? ColorUtils.PRIMARY_WHITE.getRGB() : ColorUtils.BG_INPUT.getRGB());
        RenderUtils.drawRoundedOutline(chkX, chkY, 12f, 12f, 3f, 0.8f, ColorUtils.BORDER_ACTIVE.getRGB());
        if (proxyManager.isAutoRotate()) {
            fontRenderer.drawString("✔", (int) (chkX + 2f), (int) (chkY + 2f), ColorUtils.BG_DARK.getRGB());
        }
        fontRenderer.drawString("Tự động quay vòng Proxy khi mất kết nối", (int) (chkX + 18f), (int) (chkY + 2f), ColorUtils.TEXT_WHITE.getRGB());

        // Nút Đổi ngẫu nhiên từ Pool
        float rollW = 105f, rollH = 16f;
        float rollX = panelX + 16f, rollY = curY + 24f;
        boolean hoverRoll = mouseX >= rollX && mouseX <= rollX + rollW && mouseY >= rollY && mouseY <= rollY + rollH;
        RenderUtils.drawRoundedRect(rollX, rollY, rollW, rollH, 3f, hoverRoll ? ColorUtils.BG_CARD_HOVER.getRGB() : ColorUtils.BG_INPUT.getRGB());
        RenderUtils.drawRoundedOutline(rollX, rollY, rollW, rollH, 3f, 0.8f, hoverRoll ? ColorUtils.BORDER_ACTIVE.getRGB() : ColorUtils.BORDER_CARD.getRGB());
        fontRenderer.drawString("🎲 Đổi Proxy ngẫu nhiên", (int) (rollX + 6f), (int) (rollY + 4f), ColorUtils.TEXT_SILVER.getRGB());

        // Nút Thêm Proxy Pool
        float addPoolW = 85f, addPoolH = 16f;
        float addPoolX = rollX + rollW + 6f;
        boolean hoverAddPool = mouseX >= addPoolX && mouseX <= addPoolX + addPoolW && mouseY >= rollY && mouseY <= rollY + addPoolH;
        RenderUtils.drawRoundedRect(addPoolX, rollY, addPoolW, addPoolH, 3f, hoverAddPool ? ColorUtils.BG_CARD_HOVER.getRGB() : ColorUtils.BG_INPUT.getRGB());
        RenderUtils.drawRoundedOutline(addPoolX, rollY, addPoolW, addPoolH, 3f, 0.8f, hoverAddPool ? ColorUtils.BORDER_ACTIVE.getRGB() : ColorUtils.BORDER_CARD.getRGB());
        int poolCount = proxyManager.getProxyPool().size();
        fontRenderer.drawString("+ Dán Pool (" + poolCount + ")", (int) (addPoolX + 6f), (int) (rollY + 4f), ColorUtils.TEXT_MUTED.getRGB());

        // Nút Auto Tìm & Kết Nối Proxy VN Miễn Phí
        float vnBtnW = panelW - 32f, vnBtnH = 18f;
        float vnBtnX = panelX + 16f, vnBtnY = curY + 46f;
        boolean hoverVn = mouseX >= vnBtnX && mouseX <= vnBtnX + vnBtnW && mouseY >= vnBtnY && mouseY <= vnBtnY + vnBtnH;
        int vnBg = isSearchingVN ? new Color(30, 45, 38, 240).getRGB() : (hoverVn ? new Color(24, 52, 38, 240).getRGB() : new Color(14, 28, 20, 220).getRGB());
        int vnBorder = isSearchingVN ? new Color(255, 200, 80, 240).getRGB() : (hoverVn ? new Color(90, 255, 170, 255).getRGB() : new Color(45, 190, 120, 190).getRGB());
        RenderUtils.drawRoundedRect(vnBtnX, vnBtnY, vnBtnW, vnBtnH, 4f, vnBg);
        RenderUtils.drawRoundedOutline(vnBtnX, vnBtnY, vnBtnW, vnBtnH, 4f, 0.9f, vnBorder);

        String vnTxt = isSearchingVN ? "⏳ Đang quét Proxy VN & đo Ping..." : (vnStatusMessage != null ? vnStatusMessage : "🇻🇳 Auto Tìm & Bật Proxy VN Miễn Phí (6ms - 15ms)");
        int vnTxtCol = isSearchingVN ? 0xFFEE88 : (hoverVn ? 0xFFFFFF : 0x77FFAA);
        fontRenderer.drawString(vnTxt, (int) (vnBtnX + (vnBtnW - fontRenderer.getStringWidth(vnTxt)) / 2f), (int) (vnBtnY + 5f), vnTxtCol);

        // 6. Khung 4: Kiểm tra kết nối & Ping
        curY += 74f;
        float cardH4 = 28f;
        RenderUtils.drawRoundedRect(panelX + 8f, curY, panelW - 16f, cardH4, 5f, ColorUtils.BG_CARD.getRGB());
        RenderUtils.drawRoundedOutline(panelX + 8f, curY, panelW - 16f, cardH4, 5f, 0.8f, ColorUtils.BORDER_CARD.getRGB());

        // Nút Kiểm tra
        float testW = 82f, testH = 18f;
        float testX = panelX + 14f, testY = curY + 5f;
        boolean hoverTest = mouseX >= testX && mouseX <= testX + testW && mouseY >= testY && mouseY <= testY + testH;
        RenderUtils.drawRoundedRect(testX, testY, testW, testH, 4f, hoverTest ? ColorUtils.PRIMARY_WHITE.getRGB() : ColorUtils.BG_INPUT.getRGB());
        RenderUtils.drawRoundedOutline(testX, testY, testW, testH, 4f, 0.8f, hoverTest ? ColorUtils.BORDER_ACTIVE.getRGB() : ColorUtils.BORDER_CARD.getRGB());
        int testCol = hoverTest ? ColorUtils.BG_DARK.getRGB() : ColorUtils.PRIMARY_WHITE.getRGB();
        fontRenderer.drawString(proxyManager.isTesting() ? "Đang thử..." : "⚡ Kiểm tra IP", (int) (testX + 6f), (int) (testY + 5f), testCol);

        // Kết quả test
        String res = (proxyManager.getLastTestedIp() != null) ? "IP: " + proxyManager.getLastTestedIp() : "Chưa kiểm tra";
        if (proxyManager.getLastPingMs() > 0) {
            res += " (" + proxyManager.getLastPingMs() + "ms)";
        }
        fontRenderer.drawString("§7" + res, (int) (testX + testW + 8f), (int) (testY + 5f), ColorUtils.TEXT_MUTED.getRGB());

        // 7. Footer: Nút Áp Dụng
        curY += 32f;
        float applyW = panelW - 16f;
        float applyH = 20f;
        float applyX = panelX + 8f;
        boolean hoverApply = mouseX >= applyX && mouseX <= applyX + applyW && mouseY >= curY && mouseY <= curY + applyH;
        RenderUtils.drawRoundedRect(applyX, curY, applyW, applyH, 5f, hoverApply ? new Color(245, 248, 252).getRGB() : ColorUtils.PRIMARY_WHITE.getRGB());
        if (hoverApply) {
            RenderUtils.drawRoundedOutline(applyX - 1, curY - 1, applyW + 2, applyH + 2, 6f, 1.2f, new Color(255, 255, 255, 120).getRGB());
        }
        String applyText = "✔ Lưu & Áp Dụng Proxy";
        fontRenderer.drawString(applyText, (int) (applyX + (applyW - fontRenderer.getStringWidth(applyText)) / 2f), (int) (curY + 6f), new Color(8, 9, 13).getRGB());

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawSwitch(float x, float y, boolean active, boolean hover) {
        float w = 26f, h = 14f;
        int bg = active ? ColorUtils.PRIMARY_WHITE.getRGB() : (hover ? ColorUtils.BG_CARD_HOVER.getRGB() : new Color(18, 21, 30).getRGB());
        RenderUtils.drawRoundedRect(x, y, w, h, 7, bg);
        int border = active ? ColorUtils.PRIMARY_WHITE.getRGB() : (hover ? ColorUtils.BORDER_ACTIVE.getRGB() : ColorUtils.BORDER_CARD.getRGB());
        RenderUtils.drawRoundedOutline(x, y, w, h, 7, 0.8f, border);
        float kX = active ? x + 14f : x + 2f;
        int kColor = active ? new Color(10, 11, 14).getRGB() : ColorUtils.TEXT_MUTED.getRGB();
        RenderUtils.drawRoundedRect(kX, y + 2f, 10f, 10f, 5, kColor);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        updateLayout();

        // 1. Click Nút Trở lại
        float backW = 55f, backH = 18f;
        float backX = panelX + 8f, backY = panelY + 7f;
        if (mouseX >= backX && mouseX <= backX + backW && mouseY >= backY && mouseY <= backY + backH) {
            saveInputs();
            mc.displayGuiScreen(parentScreen);
            return;
        }

        // 2. Click Master Switch
        float curY = panelY + 40f;
        float swX = panelX + 105f, swY = curY + 5f;
        if (mouseX >= swX && mouseX <= swX + 26f && mouseY >= swY && mouseY <= swY + 14f) {
            saveInputs();
            proxyManager.setProxyEnabled(!proxyManager.isProxyEnabled());
            return;
        }

        // 3. Click Đổi Loại Proxy (SOCKS5 -> HTTP/HTTPS -> SOCKS4)
        float typeW = 84f, typeH = 16f;
        float typeX = panelX + panelW - typeW - 14f;
        float typeY = curY + 4f;
        if (mouseX >= typeX && mouseX <= typeX + typeW && mouseY >= typeY && mouseY <= typeY + typeH) {
            String curr = proxyManager.getProxyType();
            if ("SOCKS5".equalsIgnoreCase(curr)) proxyManager.setProxyType("HTTP/HTTPS");
            else if ("HTTP".equalsIgnoreCase(curr) || "HTTPS".equalsIgnoreCase(curr) || "HTTP/HTTPS".equalsIgnoreCase(curr)) proxyManager.setProxyType("SOCKS4");
            else proxyManager.setProxyType("SOCKS5");
            return;
        }

        // 4. Click Nút Dán IP:Port
        curY += 28f;
        float pasteW = 48f, pasteH = 16f;
        float pasteX = panelX + panelW - pasteW - 14f;
        float pasteY = curY + 2f;
        if (mouseX >= pasteX && mouseX <= pasteX + pasteW && mouseY >= pasteY && mouseY <= pasteY + pasteH) {
            String clip = GuiScreen.getClipboardString();
            if (clip != null && !clip.isEmpty()) {
                proxyManager.parseAndSetProxy(clip.replaceAll("[\r\n\t]", ""));
                if (hostField != null) hostField.setText(proxyManager.getHost());
                if (portField != null) portField.setText(String.valueOf(proxyManager.getPort()));
                if (userField != null) userField.setText(proxyManager.getUsername());
                if (passField != null) passField.setText(proxyManager.getPassword());
            }
            return;
        }

        // 5. Click Checkbox AutoRotate
        curY += 82f;
        float chkX = panelX + 16f, chkY = curY + 6f;
        if (mouseX >= chkX && mouseX <= chkX + 220f && mouseY >= chkY && mouseY <= chkY + 14f) {
            proxyManager.setAutoRotate(!proxyManager.isAutoRotate());
            return;
        }

        // 6. Click Đổi Proxy ngẫu nhiên
        float rollW = 105f, rollH = 16f;
        float rollX = panelX + 16f, rollY = curY + 24f;
        if (mouseX >= rollX && mouseX <= rollX + rollW && mouseY >= rollY && mouseY <= rollY + rollH) {
            if (proxyManager.rotateNextProxy()) {
                if (hostField != null) hostField.setText(proxyManager.getHost());
                if (portField != null) portField.setText(String.valueOf(proxyManager.getPort()));
                if (userField != null) userField.setText(proxyManager.getUsername());
                if (passField != null) passField.setText(proxyManager.getPassword());
            }
            return;
        }

        // 7. Click Dán Pool (thêm từ clipboard)
        float addPoolW = 85f, addPoolH = 16f;
        float addPoolX = rollX + rollW + 6f;
        if (mouseX >= addPoolX && mouseX <= addPoolX + addPoolW && mouseY >= rollY && mouseY <= rollY + addPoolH) {
            String clip = GuiScreen.getClipboardString();
            if (clip != null && !clip.isEmpty()) {
                String[] lines = clip.split("[\r\n]+");
                for (String l : lines) {
                    if (l.contains(":") && !proxyManager.getProxyPool().contains(l.trim())) {
                        proxyManager.getProxyPool().add(l.trim());
                    }
                }
                proxyManager.saveConfig();
            }
            return;
        }

        // 8. Click Nút Auto Tìm & Bật Proxy VN Miễn Phí
        float vnBtnW = panelW - 32f, vnBtnH = 18f;
        float vnBtnX = panelX + 16f, vnBtnY = curY + 46f;
        if (mouseX >= vnBtnX && mouseX <= vnBtnX + vnBtnW && mouseY >= vnBtnY && mouseY <= vnBtnY + vnBtnH) {
            if (!isSearchingVN) {
                isSearchingVN = true;
                vnStatusMessage = "⏳ Đang quét máy chủ VN...";
                proxyManager.autoSetupVietnamProxy(true, result -> {
                    isSearchingVN = false;
                    if (result.success && result.bestProxy != null) {
                        vnStatusMessage = "§a✔ " + result.bestProxy.ip + ":" + result.bestProxy.port + " (" + result.bestProxy.pingMs + "ms)";
                        if (hostField != null) hostField.setText(result.bestProxy.ip);
                        if (portField != null) portField.setText(String.valueOf(result.bestProxy.port));
                        if (userField != null) userField.setText("");
                        if (passField != null) passField.setText("");
                    } else {
                        vnStatusMessage = "§c✘ " + result.message;
                    }
                });
            }
            return;
        }

        // 9. Click Nút Test
        curY += 74f;
        float testW = 82f, testH = 18f;
        float testX = panelX + 14f, testY = curY + 5f;
        if (mouseX >= testX && mouseX <= testX + testW && mouseY >= testY && mouseY <= testY + testH) {
            saveInputs();
            proxyManager.testProxyAsync(null);
            return;
        }

        // 9. Click Nút Lưu & Áp Dụng
        curY += 32f;
        float applyW = panelW - 16f, applyH = 20f;
        float applyX = panelX + 8f;
        if (mouseX >= applyX && mouseX <= applyX + applyW && mouseY >= curY && mouseY <= curY + applyH) {
            saveInputs();
            mc.displayGuiScreen(parentScreen);
            return;
        }

        // Xử lý Focus Textfields
        if (hostField != null) hostField.mouseClicked(mouseX, mouseY, mouseButton);
        if (portField != null) portField.mouseClicked(mouseX, mouseY, mouseButton);
        if (userField != null) userField.mouseClicked(mouseX, mouseY, mouseButton);
        if (passField != null) passField.mouseClicked(mouseX, mouseY, mouseButton);

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            saveInputs();
            mc.displayGuiScreen(parentScreen);
            return;
        }

        if (hostField != null && hostField.textboxKeyTyped(typedChar, keyCode)) return;
        if (portField != null && portField.textboxKeyTyped(typedChar, keyCode)) return;
        if (userField != null && userField.textboxKeyTyped(typedChar, keyCode)) return;
        if (passField != null && passField.textboxKeyTyped(typedChar, keyCode)) return;

        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void updateScreen() {
        if (hostField != null) hostField.updateCursorCounter();
        if (portField != null) portField.updateCursorCounter();
        if (userField != null) userField.updateCursorCounter();
        if (passField != null) passField.updateCursorCounter();
        super.updateScreen();
    }
}
