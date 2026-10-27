package com.casxduois.client.gui.clickgui;

import com.casxduois.client.CasxDuoisClient;
import com.casxduois.client.config.BooleanSetting;
import com.casxduois.client.config.ModeSetting;
import com.casxduois.client.config.NumberSetting;
import com.casxduois.client.config.Setting;
import com.casxduois.client.config.StringSetting;
import com.casxduois.client.gui.hud.GuiHudEditor;
import com.casxduois.client.gui.menu.CasxDuoisMainMenu;
import com.casxduois.client.modules.Category;
import com.casxduois.client.modules.Module;
import com.casxduois.client.modules.afk.AutoReconnectModule;
import com.casxduois.client.modules.render.HUDModule;
import com.casxduois.client.modules.utility.DiscordRemoteModule;
import com.casxduois.client.util.ColorUtils;
import com.casxduois.client.util.RenderUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.Color;
import java.io.IOException;
import java.util.List;

/**
 * CasxDuoisClickGui: Giao diện Cài đặt Obsidian Monochrome cao cấp
 * Mở bằng phím RIGHT SHIFT
 * Tự động thích ứng mọi GUI scale (Small, Normal, Large, Auto)
 * Chống tràn chữ (Anti-Overflow) và hiển thị thông tin rõ ràng, tinh tế
 */
public class CasxDuoisClickGui extends GuiScreen {

    private Category selectedCategory = Category.WEBHOOK;
    private Module activeSettingModule = null;
    private Module listeningKeybindModule = null;
    private NumberSetting draggingSlider = null;
    private StringSetting editingStringSetting = null;
    private NumberSetting editingNumberSetting = null;
    private String editingNumberBuffer = "";
    private int scrollOffset = 0;
    private int maxScrollHeight = 250;
    private long openTime = 0L;

    // Layout kích thước thích ứng linh hoạt theo độ phân giải & GUI Scale
    private float panelWidth;
    private float panelHeight;
    private float panelX;
    private float panelY;
    private float sidebarWidth;
    private float contentX;
    private float cardWidth;

    private void updateLayout() {
        panelWidth = Math.min(540f, Math.max(340f, width - 24f));
        panelHeight = Math.min(340f, Math.max(210f, height - 24f));
        panelX = (width - panelWidth) / 2f;
        panelY = (height - panelHeight) / 2f;
        sidebarWidth = Math.max(105f, Math.min(125f, panelWidth * 0.25f));
        contentX = panelX + sidebarWidth + 10f;
        cardWidth = panelWidth - sidebarWidth - 18f;
    }

    private String trimText(String text, float maxWidth) {
        if (text == null) return "";
        if (fontRenderer.getStringWidth(text) <= maxWidth) return text;
        return fontRenderer.trimStringToWidth(text, (int) Math.max(10, maxWidth - 8)) + "...";
    }

    private void drawSwitch(float x, float y, boolean active, boolean hover) {
        float w = 28f, h = 15f;
        if (active) {
            RenderUtils.drawRoundedGradientRect(x, y, w, h, 7.5f, ColorUtils.ACCENT_START.getRGB(), ColorUtils.ACCENT_END.getRGB());
            RenderUtils.drawRoundedOutline(x, y, w, h, 7.5f, 0.8f, ColorUtils.BORDER_ACTIVE.getRGB());
            float kX = x + w - 13f;
            RenderUtils.drawRoundedRect(kX, y + 2f, 11f, 11f, 5.5f, ColorUtils.PRIMARY_WHITE.getRGB());
        } else {
            int bg = hover ? ColorUtils.BG_CARD_HOVER.getRGB() : ColorUtils.BG_INPUT.getRGB();
            int border = hover ? ColorUtils.BORDER_HOVER.getRGB() : ColorUtils.BORDER_CARD.getRGB();
            RenderUtils.drawRoundedRect(x, y, w, h, 7.5f, bg);
            RenderUtils.drawRoundedOutline(x, y, w, h, 7.5f, 0.8f, border);
            float kX = x + 2f;
            RenderUtils.drawRoundedRect(kX, y + 2f, 11f, 11f, 5.5f, ColorUtils.TEXT_MUTED.getRGB());
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        listeningKeybindModule = null;
        draggingSlider = null;
        editingStringSetting = null;
        editingNumberSetting = null;
        editingNumberBuffer = "";
        openTime = System.currentTimeMillis();
        updateLayout();

        // Kích hoạt Shader Blur phần cứng làm mờ thế giới 3D phía sau GUI
        if (mc.world != null && OpenGlHelper.shadersSupported) {
            try {
                mc.entityRenderer.loadShader(new ResourceLocation("shaders/post/blur.json"));
            } catch (Throwable ignored) {}
        }

        if (CasxDuoisClient.getInstance().getExternalModManager() != null) {
            CasxDuoisClient.getInstance().getExternalModManager().scanMods();
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        commitNumberInput();
        editingStringSetting = null;
        activeSettingModule = null;
        notifySettingChanged();

        // Tắt Shader Blur khi đóng GUI
        if (mc.entityRenderer != null) {
            try {
                mc.entityRenderer.stopUseShader();
            } catch (Throwable ignored) {}
        }
    }

    private void commitNumberInput() {
        if (editingNumberSetting != null) {
            try {
                String trimmed = editingNumberBuffer.trim();
                if (!trimmed.isEmpty() && !trimmed.equals("-") && !trimmed.equals(".")) {
                    double val = Double.parseDouble(trimmed);
                    editingNumberSetting.setTypedValue(val);
                    notifySettingChanged();
                }
            } catch (NumberFormatException ignored) {}
            editingNumberSetting = null;
            editingNumberBuffer = "";
        }
    }

    private void tryLiveUpdateSlider() {
        if (editingNumberSetting == null || editingNumberBuffer.isEmpty() || editingNumberBuffer.equals("-") || editingNumberBuffer.equals(".")) {
            return;
        }
        try {
            double val = Double.parseDouble(editingNumberBuffer);
            if (val >= editingNumberSetting.getMinimum() && val <= editingNumberSetting.getMaximum()) {
                editingNumberSetting.setTypedValue(val);
            }
        } catch (NumberFormatException ignored) {}
    }

    private void notifySettingChanged() {
        CasxDuoisClient.getInstance().getConfigManager().saveConfig();
        DiscordRemoteModule dMod = CasxDuoisClient.getInstance().getModuleManager().getModule(DiscordRemoteModule.class);
        if (dMod != null) {
            dMod.restartPoller();
        }
        if (activeSettingModule instanceof AutoReconnectModule) {
            ((AutoReconnectModule) activeSettingModule).onServerPasswordChanged();
        }
    }

    private void openSettingTab(Module module) {
        commitNumberInput();
        activeSettingModule = module;
        scrollOffset = 0;
        editingStringSetting = null;
        if (module instanceof AutoReconnectModule) {
            ((AutoReconnectModule) module).syncCurrentServerPassword();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (listeningKeybindModule != null) {
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_DELETE) {
                listeningKeybindModule.setKey(Keyboard.KEY_NONE);
            } else {
                listeningKeybindModule.setKey(keyCode);
            }
            listeningKeybindModule = null;
            return;
        }

        if (editingNumberSetting != null) {
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                commitNumberInput();
                return;
            }

            if (isKeyComboCtrlV(keyCode) || typedChar == 22 || (isCtrlKeyDown() && (keyCode == Keyboard.KEY_V || typedChar == 'v' || typedChar == 'V'))) {
                String clip = GuiScreen.getClipboardString();
                if (clip != null && !clip.isEmpty()) {
                    clip = clip.replaceAll("[^0-9.-]", "");
                    editingNumberBuffer += clip;
                    tryLiveUpdateSlider();
                }
                return;
            }

            if (isKeyComboCtrlC(keyCode) || typedChar == 3 || (isCtrlKeyDown() && (keyCode == Keyboard.KEY_C || typedChar == 'c' || typedChar == 'C'))) {
                if (!editingNumberBuffer.isEmpty()) {
                    GuiScreen.setClipboardString(editingNumberBuffer);
                }
                return;
            }

            if (typedChar == 24 || (isCtrlKeyDown() && (keyCode == Keyboard.KEY_X || typedChar == 'x' || typedChar == 'X'))) {
                if (!editingNumberBuffer.isEmpty()) {
                    GuiScreen.setClipboardString(editingNumberBuffer);
                    editingNumberBuffer = "";
                    tryLiveUpdateSlider();
                }
                return;
            }

            if (typedChar == 1 || (isCtrlKeyDown() && (keyCode == Keyboard.KEY_A || typedChar == 'a' || typedChar == 'A')) || keyCode == Keyboard.KEY_DELETE) {
                editingNumberBuffer = "";
                return;
            }

            if (keyCode == Keyboard.KEY_BACK || typedChar == 8) {
                if (!editingNumberBuffer.isEmpty()) {
                    if (isCtrlKeyDown()) {
                        editingNumberBuffer = "";
                    } else {
                        editingNumberBuffer = editingNumberBuffer.substring(0, editingNumberBuffer.length() - 1);
                    }
                    tryLiveUpdateSlider();
                }
                return;
            }

            if (typedChar >= '0' && typedChar <= '9') {
                if (editingNumberBuffer.length() < 12) {
                    editingNumberBuffer += typedChar;
                    tryLiveUpdateSlider();
                }
                return;
            }

            if (typedChar == '.') {
                if (!editingNumberBuffer.contains(".") && editingNumberBuffer.length() < 12) {
                    editingNumberBuffer += ".";
                }
                return;
            }

            if (typedChar == '-') {
                if (editingNumberSetting.getMinimum() < 0 && editingNumberBuffer.isEmpty()) {
                    editingNumberBuffer += "-";
                }
                return;
            }

            return;
        }

        if (editingStringSetting != null) {
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RETURN) {
                notifySettingChanged();
                editingStringSetting = null;
                return;
            }

            if (isKeyComboCtrlV(keyCode) || typedChar == 22 || (isCtrlKeyDown() && (keyCode == Keyboard.KEY_V || typedChar == 'v' || typedChar == 'V'))) {
                String clip = GuiScreen.getClipboardString();
                if (clip != null && !clip.isEmpty()) {
                    clip = clip.replaceAll("[\r\n\t]", "");
                    String val = editingStringSetting.getValue() != null ? editingStringSetting.getValue() : "";
                    editingStringSetting.setValue(val + clip);
                    notifySettingChanged();
                }
                return;
            }

            if (isKeyComboCtrlC(keyCode) || typedChar == 3 || (isCtrlKeyDown() && (keyCode == Keyboard.KEY_C || typedChar == 'c' || typedChar == 'C'))) {
                String val = editingStringSetting.getValue();
                if (val != null && !val.isEmpty()) {
                    GuiScreen.setClipboardString(val);
                }
                return;
            }

            if (typedChar == 24 || (isCtrlKeyDown() && (keyCode == Keyboard.KEY_X || typedChar == 'x' || typedChar == 'X'))) {
                String val = editingStringSetting.getValue();
                if (val != null && !val.isEmpty()) {
                    GuiScreen.setClipboardString(val);
                    editingStringSetting.setValue("");
                    notifySettingChanged();
                }
                return;
            }

            if (typedChar == 1 || (isCtrlKeyDown() && (keyCode == Keyboard.KEY_A || typedChar == 'a' || typedChar == 'A')) || keyCode == Keyboard.KEY_DELETE) {
                editingStringSetting.setValue("");
                notifySettingChanged();
                return;
            }

            if (keyCode == Keyboard.KEY_BACK || typedChar == 8) {
                String val = editingStringSetting.getValue();
                if (val != null && !val.isEmpty()) {
                    if (isCtrlKeyDown()) {
                        editingStringSetting.setValue("");
                    } else {
                        editingStringSetting.setValue(val.substring(0, val.length() - 1));
                    }
                    notifySettingChanged();
                }
                return;
            }

            if (typedChar >= 32 && typedChar != 127) {
                String val = editingStringSetting.getValue() != null ? editingStringSetting.getValue() : "";
                editingStringSetting.setValue(val + typedChar);
                notifySettingChanged();
                return;
            }
            return;
        }

        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (activeSettingModule != null) {
                activeSettingModule = null;
                scrollOffset = 0;
                return;
            }
            mc.displayGuiScreen(null);
            return;
        }

        if (keyCode == Keyboard.KEY_RSHIFT) {
            mc.displayGuiScreen(null);
            return;
        }

        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        updateLayout();

        // 1. Click chọn Danh mục Sidebar
        float catY = panelY + 40f;
        for (Category cat : Category.values()) {
            if (mouseX >= panelX + 6f && mouseX <= panelX + sidebarWidth - 6f && mouseY >= catY && mouseY <= catY + 24f) {
                selectedCategory = cat;
                activeSettingModule = null;
                scrollOffset = 0;
                editingStringSetting = null;
                return;
            }
            catY += 28f;
        }

        // 2. Click trong tab Quản lý Mods
        if (selectedCategory == Category.MODS) {
            float contentY = panelY + 40f;
            if (ModManagerPanel.mouseClicked(contentX, contentY, cardWidth, scrollOffset, mouseX, mouseY)) {
                return;
            }
            super.mouseClicked(mouseX, mouseY, mouseButton);
            return;
        }

        // 3. Đang trong Tab Cài Đặt Riêng của 1 Module
        if (activeSettingModule != null) {
            float navBarY = panelY + 40f;
            // Nút [← Quay lại]
            if (mouseX >= contentX && mouseX <= contentX + 72f && mouseY >= navBarY && mouseY <= navBarY + 18f) {
                activeSettingModule = null;
                scrollOffset = 0;
                editingStringSetting = null;
                return;
            }

            // Nút Toggle nhanh Module ở header
            float headerToggleX = contentX + cardWidth - 32f;
            float headerToggleY = navBarY + 2f;
            if (mouseX >= headerToggleX && mouseX <= headerToggleX + 28f && mouseY >= headerToggleY && mouseY <= headerToggleY + 16f) {
                activeSettingModule.toggle();
                return;
            }

            float settingContentY = panelY + 64f + scrollOffset;

            // Thẻ tổng quan & đổi phím tắt
            float keyBoxW = 68f;
            float keyBoxX = contentX + cardWidth - keyBoxW - 8f;
            if (mouseX >= keyBoxX && mouseX <= keyBoxX + keyBoxW && mouseY >= settingContentY + 14f && mouseY <= settingContentY + 30f) {
                listeningKeybindModule = activeSettingModule;
                return;
            }

            settingContentY += 46f;

            // Nút Mở Trình Kéo Thả HUD nếu đang trong HUDModule
            if (activeSettingModule instanceof HUDModule) {
                float edW = cardWidth;
                if (mouseX >= contentX && mouseX <= contentX + edW && mouseY >= settingContentY && mouseY <= settingContentY + 24f) {
                    mc.displayGuiScreen(new GuiHudEditor(this));
                    return;
                }
                settingContentY += 30f;
            }

            // Offset cho Khung Hướng dẫn Discord Bot
            if (activeSettingModule instanceof DiscordRemoteModule) {
                settingContentY += 56f;
            }

            // Danh sách Settings
            for (Setting<?> setting : activeSettingModule.getSettings()) {
                if (setting.getName().startsWith("Pos_")) {
                    continue; // Ẩn các tham số tọa độ thô, người dùng chỉnh trực quan bằng kéo thả
                }
                float cardH = (setting instanceof StringSetting) ? 36f : 32f;

                if (setting instanceof BooleanSetting) {
                    BooleanSetting bool = (BooleanSetting) setting;
                    if (mouseX >= contentX && mouseX <= contentX + cardWidth && mouseY >= settingContentY && mouseY <= settingContentY + cardH) {
                        commitNumberInput();
                        bool.toggle();
                        return;
                    }
                } else if (setting instanceof ModeSetting) {
                    ModeSetting mode = (ModeSetting) setting;
                    float mBtnW = Math.max(75f, Math.min(92f, cardWidth * 0.30f));
                    float mBtnX = contentX + cardWidth - mBtnW - 8f;
                    float mBtnY = settingContentY + 7f;
                    if (mouseX >= mBtnX && mouseX <= mBtnX + mBtnW && mouseY >= mBtnY && mouseY <= mBtnY + 18f) {
                        commitNumberInput();
                        mode.cycle();
                        return;
                    }
                } else if (setting instanceof NumberSetting) {
                    NumberSetting num = (NumberSetting) setting;
                    float inputBoxW = Math.max(46f, Math.min(56f, cardWidth * 0.16f));
                    float inputBoxH = 18f;
                    float inputBoxX = contentX + cardWidth - inputBoxW - 8f;
                    float inputBoxY = settingContentY + 7f;

                    float sliderW = Math.max(50f, Math.min(80f, cardWidth * 0.20f));
                    float sliderX = inputBoxX - sliderW - 8f;

                    // 1. Click vào ô nhập số trực tiếp bằng bàn phím
                    if (mouseX >= inputBoxX && mouseX <= inputBoxX + inputBoxW && mouseY >= inputBoxY && mouseY <= inputBoxY + inputBoxH) {
                        if (editingNumberSetting != num) {
                            commitNumberInput();
                            editingStringSetting = null;
                            editingNumberSetting = num;
                            if (num.getIncrement() >= 1.0 && num.getValue() == Math.floor(num.getValue())) {
                                editingNumberBuffer = String.valueOf(num.getValueInt());
                            } else {
                                editingNumberBuffer = String.format(java.util.Locale.US, "%.1f", num.getValue());
                            }
                        }
                        return;
                    }

                    // 2. Click hoặc kéo thanh slider
                    if (mouseX >= sliderX - 4f && mouseX <= sliderX + sliderW + 4f && mouseY >= settingContentY + 5f && mouseY <= settingContentY + cardH - 5f) {
                        commitNumberInput();
                        draggingSlider = num;
                        updateSlider(num, mouseX, sliderX, sliderW);
                        return;
                    }
                } else if (setting instanceof StringSetting) {
                    StringSetting str = (StringSetting) setting;
                    float inputAreaW = Math.max(140f, Math.min(210f, cardWidth * 0.52f));
                    float clearW = 16f, pasteW = 28f;
                    float inW = inputAreaW - clearW - pasteW - 6f;
                    float inX = contentX + cardWidth - inputAreaW - 8f;
                    float pasteX = inX + inW + 3f;
                    float clearX = pasteX + pasteW + 3f;
                    float inY = settingContentY + 8f;
                    float inH = 18f;

                    if (mouseButton == 0 && mouseX >= clearX && mouseX <= clearX + clearW && mouseY >= inY && mouseY <= inY + inH) {
                        str.setValue("");
                        editingStringSetting = str;
                        notifySettingChanged();
                        return;
                    }

                    if (mouseButton == 0 && mouseX >= pasteX && mouseX <= pasteX + pasteW && mouseY >= inY && mouseY <= inY + inH) {
                        String clip = GuiScreen.getClipboardString();
                        if (clip != null && !clip.isEmpty()) {
                            clip = clip.replaceAll("[\r\n\t]", "");
                            str.setValue(clip);
                            notifySettingChanged();
                        }
                        editingStringSetting = str;
                        return;
                    }

                    if (mouseX >= inX && mouseX <= inX + inW && mouseY >= inY && mouseY <= inY + inH) {
                        if (mouseButton == 1) {
                            String clip = GuiScreen.getClipboardString();
                            if (clip != null && !clip.isEmpty()) {
                                clip = clip.replaceAll("[\r\n\t]", "");
                                str.setValue(clip);
                                notifySettingChanged();
                            }
                        }
                        editingStringSetting = str;
                        return;
                    }
                }

                settingContentY += cardH + 6f;
            }

            if (activeSettingModule instanceof DiscordRemoteModule) {
                float testBtnY = settingContentY + 4f;
                if (mouseX >= contentX + 6f && mouseX <= contentX + 130f && mouseY >= testBtnY && mouseY <= testBtnY + 22f) {
                    ((DiscordRemoteModule) activeSettingModule).testConnection();
                    return;
                }
            }

            editingStringSetting = null;
            return;
        }

        // 4. Danh sách Module List
        float contentY = panelY + 42f + scrollOffset;

        if (selectedCategory == Category.HUD) {
            float editorBtnW = Math.min(180f, cardWidth * 0.65f);
            if (mouseButton == 0 && mouseX >= contentX + 6f && mouseX <= contentX + 6f + editorBtnW && mouseY >= contentY + 3f && mouseY <= contentY + 23f) {
                mc.displayGuiScreen(new GuiHudEditor(this));
                return;
            }
            contentY += 34f;
        }

        List<Module> modules = CasxDuoisClient.getInstance().getModuleManager().getModulesByCategory(selectedCategory);

        for (Module module : modules) {
            float cardHeight = 36f;

            if (mouseX >= contentX && mouseX <= contentX + cardWidth && mouseY >= contentY && mouseY <= contentY + cardHeight) {
                float keyW = 28f;
                float keyX = contentX + cardWidth - 116f;
                if (mouseButton == 0 && mouseX >= keyX && mouseX <= keyX + keyW && mouseY >= contentY + 10f && mouseY <= contentY + 26f) {
                    listeningKeybindModule = module;
                    return;
                }

                float optBtnW = 46f;
                float optBtnX = contentX + cardWidth - 84f;
                if (mouseX >= optBtnX && mouseX <= optBtnX + optBtnW && mouseY >= contentY + 10f && mouseY <= contentY + 26f) {
                    openSettingTab(module);
                    return;
                }

                float toggleW = 26f;
                float toggleX = contentX + cardWidth - 32f;
                float toggleY = contentY + 11f;
                if (mouseX >= toggleX && mouseX <= toggleX + toggleW && mouseY >= toggleY && mouseY <= toggleY + 14f) {
                    module.toggle();
                    return;
                }

                if (mouseButton == 1) {
                    openSettingTab(module);
                    return;
                }

                if (mouseButton == 0) {
                    module.toggle();
                    return;
                }
            }

            contentY += cardHeight + 6f;
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        draggingSlider = null;
        super.mouseReleased(mouseX, mouseY, state);
    }

    private void updateSlider(NumberSetting setting, int mouseX, float sliderX, float sliderWidth) {
        float diff = Math.min(sliderWidth, Math.max(0, mouseX - sliderX));
        double percent = diff / sliderWidth;
        double newValue = setting.getMinimum() + (setting.getMaximum() - setting.getMinimum()) * percent;
        double inc = setting.getIncrement();
        if (inc > 0) {
            newValue = Math.round(newValue / inc) * inc;
        }
        setting.setValue(newValue);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel != 0) {
            scrollOffset += (dWheel > 0 ? 25 : -25);
            scrollOffset = Math.min(0, Math.max(-maxScrollHeight, scrollOffset));
        }
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
        updateLayout();

        // Animation trượt vào khi mở GUI
        long now = System.currentTimeMillis();
        float animProgress = Math.min(1.0f, (now - openTime) / 320.0f);
        float ease = 1.0f - (float) Math.pow(1.0 - animProgress, 3);
        float slideOffset = (1.0f - ease) * 12.0f;
        panelY += slideOffset;

        // 1. Viền mờ & Nền Obsidian Deep Dark với Glassmorphism Panel
        RenderUtils.drawGlassPanel(panelX, panelY, panelWidth, panelHeight, 10f);

        // 2. Header Bar tinh gọn và thanh lịch
        float headerH = 34f;
        RenderUtils.drawRect(panelX + 10f, panelY + headerH, panelWidth - 20f, 1f, ColorUtils.BORDER_GLASS.getRGB());

        // Brand logo & title với Accent indicator dot
        RenderUtils.drawAccentDot(panelX + 16f, panelY + 17f, 3.5f, true);
        fontRenderer.drawStringWithShadow("CasxDuois", (int) (panelX + 26f), (int) (panelY + 13f), ColorUtils.PRIMARY_WHITE.getRGB());

        // Tên tài khoản tinh gọn
        String accountName = (mc.getSession() != null && mc.getSession().getUsername() != null) ? mc.getSession().getUsername() : "Player";
        fontRenderer.drawString("· " + accountName, (int) (panelX + 26f + fontRenderer.getStringWidth("CasxDuois") + 6f), (int) (panelY + 13f), ColorUtils.TEXT_MUTED.getRGB());

        // Phím tắt đóng ở góc phải
        String closeHint = "ESC / R-SHIFT";
        fontRenderer.drawString(closeHint, (int) (panelX + panelWidth - fontRenderer.getStringWidth(closeHint) - 16f), (int) (panelY + 13f), ColorUtils.TEXT_MUTED.getRGB());

        // 3. Sidebar Danh Mục bên trái
        float catY = panelY + 40f;
        for (Category cat : Category.values()) {
            boolean isSelected = (cat == selectedCategory);
            boolean isHover = mouseX >= panelX + 6f && mouseX <= panelX + sidebarWidth - 6f && mouseY >= catY && mouseY <= catY + 24f;

            if (isSelected) {
                RenderUtils.drawRoundedRect(panelX + 6f, catY, sidebarWidth - 12f, 24f, 5, ColorUtils.ACCENT_SOFT.getRGB());
                RenderUtils.drawRoundedOutline(panelX + 6f, catY, sidebarWidth - 12f, 24f, 5, 0.8f, ColorUtils.BORDER_ACTIVE.getRGB());
                RenderUtils.drawAccentIndicator(panelX + 6f, catY + 4f, 2.5f, 16f, 1.25f);
            } else if (isHover) {
                RenderUtils.drawRoundedRect(panelX + 6f, catY, sidebarWidth - 12f, 24f, 5, ColorUtils.BG_CARD_HOVER.getRGB());
                RenderUtils.drawRoundedOutline(panelX + 6f, catY, sidebarWidth - 12f, 24f, 5, 0.8f, ColorUtils.BORDER_HOVER.getRGB());
            }

            int textColor = isSelected ? ColorUtils.PRIMARY_WHITE.getRGB() : (isHover ? ColorUtils.TEXT_SILVER.getRGB() : ColorUtils.TEXT_MUTED.getRGB());
            String catName = cat.getDisplayName();
            float maxCatW = sidebarWidth - 22f;
            if (fontRenderer.getStringWidth(catName) > maxCatW) {
                catName = fontRenderer.trimStringToWidth(catName, (int) (maxCatW - 6)) + "..";
            }
            fontRenderer.drawStringWithShadow(catName, (int) (panelX + 16f), (int) (catY + 8f), textColor);
            catY += 28f;
        }

        // 4. Vùng Nội Dung chính bên phải
        RenderUtils.startScissor(contentX - 4f, panelY + 36f, cardWidth + 8f, panelHeight - 42f);

        if (selectedCategory == Category.MODS) {
            ModManagerPanel.draw(contentX, panelY + 40f, cardWidth, panelHeight, scrollOffset, mouseX, mouseY);
        } else if (activeSettingModule != null) {
            // ─────────────────────────────────────────────────────────────
            // TAB CÀI ĐẶT RIÊNG CHO MODULE
            // ─────────────────────────────────────────────────────────────
            float navBarY = panelY + 40f;
            boolean hoverBack = mouseX >= contentX && mouseX <= contentX + 72f && mouseY >= navBarY && mouseY <= navBarY + 18f;
            RenderUtils.drawRoundedRect(contentX, navBarY, 72f, 18f, 4, hoverBack ? ColorUtils.BG_CARD_HOVER.getRGB() : ColorUtils.BG_CARD.getRGB());
            RenderUtils.drawRoundedOutline(contentX, navBarY, 72f, 18f, 4, 0.8f, hoverBack ? ColorUtils.BORDER_ACTIVE.getRGB() : ColorUtils.BORDER_GLASS.getRGB());
            fontRenderer.drawString("← Quay lại", (int) (contentX + 8f), (int) (navBarY + 5f), hoverBack ? ColorUtils.PRIMARY_WHITE.getRGB() : ColorUtils.TEXT_SILVER.getRGB());

            String tabTitle = activeSettingModule.getVietnameseName();
            float titleMaxW = cardWidth - 120f;
            if (fontRenderer.getStringWidth(tabTitle) > titleMaxW) {
                tabTitle = fontRenderer.trimStringToWidth(tabTitle, (int) (titleMaxW - 8)) + "...";
            }
            fontRenderer.drawStringWithShadow(tabTitle, (int) (contentX + 80f), (int) (navBarY + 5f), ColorUtils.TEXT_WHITE.getRGB());

            float headerToggleX = contentX + cardWidth - 32f;
            float headerToggleY = navBarY + 2f;
            boolean hoverHToggle = mouseX >= headerToggleX && mouseX <= headerToggleX + 28f && mouseY >= headerToggleY && mouseY <= headerToggleY + 16f;
            drawSwitch(headerToggleX, headerToggleY, activeSettingModule.isToggled(), hoverHToggle);

            float curY = panelY + 64f + scrollOffset;

            // Thẻ 1: Thông tin & Phím tắt
            RenderUtils.drawRoundedRect(contentX, curY, cardWidth, 38f, 6, ColorUtils.BG_CARD.getRGB());
            RenderUtils.drawRoundedOutline(contentX, curY, cardWidth, 38f, 6, 0.8f, ColorUtils.BORDER_GLASS.getRGB());

            float descMaxW = cardWidth - 85f;
            String desc = trimText(activeSettingModule.getDescription(), descMaxW);
            fontRenderer.drawString(desc, (int) (contentX + 10f), (int) (curY + 7f), ColorUtils.TEXT_MUTED.getRGB());

            String keyStatus = listeningKeybindModule == activeSettingModule ? "[Nhấn phím...]" : (activeSettingModule.getKey() == Keyboard.KEY_NONE ? "[Chưa gán]" : "[" + Keyboard.getKeyName(activeSettingModule.getKey()) + "]");
            fontRenderer.drawString("Phím tắt:", (int) (contentX + 10f), (int) (curY + 21f), ColorUtils.TEXT_SILVER.getRGB());

            float keyBoxW = 68f;
            float keyBoxX = contentX + cardWidth - keyBoxW - 8f;
            boolean hoverKey = mouseX >= keyBoxX && mouseX <= keyBoxX + keyBoxW && mouseY >= curY + 18f && mouseY <= curY + 32f;
            RenderUtils.drawRoundedRect(keyBoxX, curY + 18f, keyBoxW, 14f, 4, hoverKey ? ColorUtils.BG_CARD_HOVER.getRGB() : ColorUtils.BG_INPUT.getRGB());
            RenderUtils.drawRoundedOutline(keyBoxX, curY + 18f, keyBoxW, 14f, 4, 0.8f, hoverKey ? ColorUtils.BORDER_ACTIVE.getRGB() : ColorUtils.BORDER_INPUT.getRGB());
            fontRenderer.drawString(keyStatus, (int) (keyBoxX + (keyBoxW - fontRenderer.getStringWidth(keyStatus)) / 2f), (int) (curY + 21f), ColorUtils.TEXT_SILVER.getRGB());

            curY += 44f;

            // Nút Mở Trình Kéo Thả Vị Trí HUD nếu là HUDModule
            if (activeSettingModule instanceof HUDModule) {
                float edW = cardWidth;
                boolean hoverEd = mouseX >= contentX && mouseX <= contentX + edW && mouseY >= curY && mouseY <= curY + 24f;
                if (hoverEd) {
                    RenderUtils.drawRoundedGradientRect(contentX, curY, edW, 24f, 5f, ColorUtils.ACCENT_START.getRGB(), ColorUtils.ACCENT_END.getRGB());
                } else {
                    RenderUtils.drawRoundedRect(contentX, curY, edW, 24f, 5f, ColorUtils.BG_CARD.getRGB());
                    RenderUtils.drawRoundedOutline(contentX, curY, edW, 24f, 5f, 0.8f, ColorUtils.BORDER_ACTIVE.getRGB());
                }
                int col = hoverEd ? ColorUtils.BG_DARK.getRGB() : ColorUtils.PRIMARY_WHITE.getRGB();
                String txt = "Kéo thả vị trí HUD (Drag & Drop Editor)";
                fontRenderer.drawString(txt, (int) (contentX + (edW - fontRenderer.getStringWidth(txt)) / 2f), (int) (curY + 8f), col);
                curY += 30f;
            }

            // Khung Hướng dẫn Lệnh Discord Bot nếu là DiscordRemoteModule
            if (activeSettingModule instanceof DiscordRemoteModule) {
                float infoBoxH = 50f;
                RenderUtils.drawRoundedRect(contentX, curY, cardWidth, infoBoxH, 6f, ColorUtils.BG_CARD.getRGB());
                RenderUtils.drawRoundedOutline(contentX, curY, cardWidth, infoBoxH, 6f, 0.8f, ColorUtils.BORDER_GLASS.getRGB());
                fontRenderer.drawString("Lệnh điều khiển Discord Bot (!tra)", (int) (contentX + 10f), (int) (curY + 6f), ColorUtils.PRIMARY_WHITE.getRGB());
                fontRenderer.drawString("• !tra : Gửi thông tin trạng thái tài khoản đang treo", (int) (contentX + 10f), (int) (curY + 18f), ColorUtils.TEXT_SILVER.getRGB());
                fontRenderer.drawString("• !tra <tên> hoặc !tra all : Chụp ảnh màn hình acc gửi qua Discord", (int) (contentX + 10f), (int) (curY + 28f), ColorUtils.TEXT_SILVER.getRGB());
                String botStatus = (CasxDuoisClient.getInstance().getDiscordPoller() != null && CasxDuoisClient.getInstance().getDiscordPoller().isRunning()) ? "Bot đang lắng nghe" : "Bot chưa kết nối";
                fontRenderer.drawString("Trạng thái: " + botStatus, (int) (contentX + 10f), (int) (curY + 39f), ColorUtils.TEXT_MUTED.getRGB());
                curY += infoBoxH + 6f;
            }

            // Danh sách Settings
            List<Setting<?>> settings = activeSettingModule.getSettings();
            if (settings.isEmpty()) {
                RenderUtils.drawRoundedRect(contentX, curY, cardWidth, 32f, 6, ColorUtils.BG_CARD.getRGB());
                RenderUtils.drawRoundedOutline(contentX, curY, cardWidth, 32f, 6, 0.8f, ColorUtils.BORDER_GLASS.getRGB());
                fontRenderer.drawString("Tính năng này hoạt động tự động, không có thêm cài đặt.", (int) (contentX + 10f), (int) (curY + 12f), ColorUtils.TEXT_MUTED.getRGB());
                curY += 38f;
            } else {
                for (Setting<?> setting : settings) {
                    if (setting.getName().startsWith("Pos_")) {
                        continue;
                    }
                    float cardH = (setting instanceof StringSetting) ? 36f : 32f;
                    RenderUtils.drawRoundedRect(contentX, curY, cardWidth, cardH, 6, ColorUtils.BG_CARD.getRGB());
                    RenderUtils.drawRoundedOutline(contentX, curY, cardWidth, cardH, 6, 0.8f, ColorUtils.BORDER_GLASS.getRGB());

                    if (setting instanceof BooleanSetting) {
                        BooleanSetting bool = (BooleanSetting) setting;
                        float swX = contentX + cardWidth - 32f;
                        float swY = curY + 8f;
                        float maxTextW = swX - (contentX + 10f) - 8f;

                        String sName = trimText(bool.getVietnameseName(), maxTextW);
                        String sDesc = trimText(bool.getDescription(), maxTextW);
                        fontRenderer.drawStringWithShadow(sName, (int) (contentX + 10f), (int) (curY + 6f), ColorUtils.TEXT_WHITE.getRGB());
                        fontRenderer.drawString(sDesc, (int) (contentX + 10f), (int) (curY + 18f), ColorUtils.TEXT_MUTED.getRGB());

                        boolean hoverSw = mouseX >= swX && mouseX <= swX + 28f && mouseY >= swY && mouseY <= swY + 16f;
                        drawSwitch(swX, swY, bool.isEnabled(), hoverSw);

                    } else if (setting instanceof ModeSetting) {
                        ModeSetting mode = (ModeSetting) setting;
                        float mBtnW = Math.max(75f, Math.min(92f, cardWidth * 0.30f));
                        float mBtnX = contentX + cardWidth - mBtnW - 8f;
                        float mBtnY = curY + 7f;
                        float maxTextW = mBtnX - (contentX + 10f) - 8f;

                        String sName = trimText(mode.getVietnameseName(), maxTextW);
                        String sDesc = trimText(mode.getDescription(), maxTextW);
                        fontRenderer.drawStringWithShadow(sName, (int) (contentX + 10f), (int) (curY + 6f), ColorUtils.TEXT_WHITE.getRGB());
                        fontRenderer.drawString(sDesc, (int) (contentX + 10f), (int) (curY + 18f), ColorUtils.TEXT_MUTED.getRGB());

                        boolean hoverMode = mouseX >= mBtnX && mouseX <= mBtnX + mBtnW && mouseY >= mBtnY && mouseY <= mBtnY + 18f;
                        RenderUtils.drawRoundedRect(mBtnX, mBtnY, mBtnW, 18f, 4, hoverMode ? ColorUtils.BG_CARD_HOVER.getRGB() : ColorUtils.BG_INPUT.getRGB());
                        RenderUtils.drawRoundedOutline(mBtnX, mBtnY, mBtnW, 18f, 4, 0.8f, hoverMode ? ColorUtils.BORDER_HOVER.getRGB() : ColorUtils.BORDER_INPUT.getRGB());
                        String modeTxt = "‹ " + mode.getValue() + " ›";
                        fontRenderer.drawString(modeTxt, (int) (mBtnX + (mBtnW - fontRenderer.getStringWidth(modeTxt)) / 2f), (int) (mBtnY + 5f), hoverMode ? ColorUtils.PRIMARY_WHITE.getRGB() : ColorUtils.TEXT_SILVER.getRGB());

                    } else if (setting instanceof NumberSetting) {
                        NumberSetting num = (NumberSetting) setting;
                        float inputBoxW = Math.max(46f, Math.min(56f, cardWidth * 0.16f));
                        float inputBoxH = 18f;
                        float inputBoxX = contentX + cardWidth - inputBoxW - 8f;
                        float inputBoxY = curY + 7f;

                        float sliderW = Math.max(50f, Math.min(80f, cardWidth * 0.20f));
                        float sliderX = inputBoxX - sliderW - 8f;
                        float maxTextW = sliderX - (contentX + 10f) - 6f;

                        if (draggingSlider == num) {
                            updateSlider(num, mouseX, sliderX, sliderW);
                        }

                        String sName = trimText(num.getVietnameseName(), maxTextW);
                        String sDesc = trimText(num.getDescription(), maxTextW);
                        fontRenderer.drawStringWithShadow(sName, (int) (contentX + 10f), (int) (curY + 6f), ColorUtils.TEXT_WHITE.getRGB());
                        fontRenderer.drawString(sDesc, (int) (contentX + 10f), (int) (curY + 18f), ColorUtils.TEXT_MUTED.getRGB());

                        // 1. Thanh trượt Slider với Gradient Accent
                        float sliderBarY = curY + 14f;
                        RenderUtils.drawRoundedRect(sliderX, sliderBarY, sliderW, 4f, 2, ColorUtils.BG_INPUT.getRGB());
                        RenderUtils.drawRoundedOutline(sliderX, sliderBarY, sliderW, 4f, 2, 0.8f, ColorUtils.BORDER_INPUT.getRGB());
                        double pct = Math.min(1.0, Math.max(0.0, (num.getValue() - num.getMinimum()) / (num.getMaximum() - num.getMinimum())));
                        if (pct > 0.01) {
                            RenderUtils.drawRoundedGradientRect(sliderX, sliderBarY, (float) (sliderW * pct), 4f, 2f, ColorUtils.ACCENT_START.getRGB(), ColorUtils.ACCENT_END.getRGB());
                        }
                        // Nút thumb tròn trên thanh slider
                        float thumbX = sliderX + (float) (sliderW * pct);
                        RenderUtils.drawCircle(thumbX, sliderBarY + 2f, 4.5f, ColorUtils.PRIMARY_WHITE.getRGB());

                        // 2. Ô nhập số trực tiếp thay vì chỉ kéo (Direct Numeric Input Box)
                        boolean isEditNum = (editingNumberSetting == num);
                        boolean hoverInput = mouseX >= inputBoxX && mouseX <= inputBoxX + inputBoxW && mouseY >= inputBoxY && mouseY <= inputBoxY + inputBoxH;

                        int boxBg = isEditNum ? ColorUtils.BG_INPUT.getRGB() : (hoverInput ? ColorUtils.BG_CARD_HOVER.getRGB() : ColorUtils.BG_INPUT.getRGB());
                        int boxBorder = isEditNum ? ColorUtils.BORDER_ACTIVE.getRGB() : (hoverInput ? ColorUtils.BORDER_HOVER.getRGB() : ColorUtils.BORDER_INPUT.getRGB());

                        RenderUtils.drawRoundedRect(inputBoxX, inputBoxY, inputBoxW, inputBoxH, 3, boxBg);
                        RenderUtils.drawRoundedOutline(inputBoxX, inputBoxY, inputBoxW, inputBoxH, 3, 0.8f, boxBorder);

                        String displayVal;
                        if (isEditNum) {
                            displayVal = editingNumberBuffer;
                            if (System.currentTimeMillis() % 1000 > 500) {
                                displayVal += "|";
                            }
                        } else {
                            if (num.getIncrement() >= 1.0 && num.getValue() == Math.floor(num.getValue())) {
                                displayVal = String.valueOf(num.getValueInt());
                            } else {
                                displayVal = String.format(java.util.Locale.US, "%.1f", num.getValue());
                            }
                        }

                        String trimmedDisplay = trimText(displayVal, inputBoxW - 6f);
                        int textX = (int) (inputBoxX + Math.max(3f, (inputBoxW - fontRenderer.getStringWidth(trimmedDisplay)) / 2f));
                        int textY = (int) (inputBoxY + 5f);
                        int numTextColor = isEditNum ? ColorUtils.PRIMARY_WHITE.getRGB() : (hoverInput ? ColorUtils.TEXT_WHITE.getRGB() : ColorUtils.TEXT_SILVER.getRGB());
                        fontRenderer.drawString(trimmedDisplay, textX, textY, numTextColor);

                    } else if (setting instanceof StringSetting) {
                        StringSetting str = (StringSetting) setting;
                        float inputAreaW = Math.max(140f, Math.min(210f, cardWidth * 0.52f));
                        float clearW = 16f, pasteW = 28f;
                        float inW = inputAreaW - clearW - pasteW - 6f;
                        float inX = contentX + cardWidth - inputAreaW - 8f;
                        float pasteX = inX + inW + 3f;
                        float clearX = pasteX + pasteW + 3f;
                        float inY = curY + 9f;
                        float inH = 18f;

                        float maxTextW = inX - (contentX + 10f) - 8f;
                        String sName = trimText(str.getVietnameseName(), maxTextW);
                        String sDesc = trimText(str.getDescription(), maxTextW);
                        fontRenderer.drawStringWithShadow(sName, (int) (contentX + 10f), (int) (curY + 6f), ColorUtils.TEXT_WHITE.getRGB());
                        fontRenderer.drawString(sDesc, (int) (contentX + 10f), (int) (curY + 19f), ColorUtils.TEXT_MUTED.getRGB());

                        boolean isEdit = (editingStringSetting == str);
                        boolean hoverIn = mouseX >= inX && mouseX <= inX + inW && mouseY >= inY && mouseY <= inY + inH;
                        boolean hoverPaste = mouseX >= pasteX && mouseX <= pasteX + pasteW && mouseY >= inY && mouseY <= inY + inH;
                        boolean hoverClear = mouseX >= clearX && mouseX <= clearX + clearW && mouseY >= inY && mouseY <= inY + inH;

                        // Ô nhập text
                        RenderUtils.drawRoundedRect(inX, inY, inW, inH, 4, isEdit ? ColorUtils.BG_INPUT.getRGB() : (hoverIn ? ColorUtils.BG_CARD_HOVER.getRGB() : ColorUtils.BG_INPUT.getRGB()));
                        RenderUtils.drawRoundedOutline(inX, inY, inW, inH, 4, 0.8f, isEdit ? ColorUtils.BORDER_ACTIVE.getRGB() : (hoverIn ? ColorUtils.BORDER_HOVER.getRGB() : ColorUtils.BORDER_INPUT.getRGB()));

                        String val = str.getValue();
                        String display = (val == null || val.isEmpty()) ? (isEdit ? "" : "(Bấm/Dán)") : (isEdit ? val : str.getMaskedValue());
                        if (isEdit && (System.currentTimeMillis() % 1000 > 500)) display += "|";
                        String trimmed = trimText(display, inW - 8f);
                        fontRenderer.drawString(trimmed, (int) (inX + 5f), (int) (inY + 5f), isEdit ? ColorUtils.PRIMARY_WHITE.getRGB() : ColorUtils.TEXT_WHITE.getRGB());

                        // Nút Dán
                        RenderUtils.drawRoundedRect(pasteX, inY, pasteW, inH, 3, hoverPaste ? ColorUtils.BG_CARD_HOVER.getRGB() : ColorUtils.BG_INPUT.getRGB());
                        RenderUtils.drawRoundedOutline(pasteX, inY, pasteW, inH, 3, 0.8f, hoverPaste ? ColorUtils.BORDER_HOVER.getRGB() : ColorUtils.BORDER_INPUT.getRGB());
                        fontRenderer.drawString("Dán", (int) (pasteX + (pasteW - fontRenderer.getStringWidth("Dán")) / 2f), (int) (inY + 5f), hoverPaste ? ColorUtils.PRIMARY_WHITE.getRGB() : ColorUtils.TEXT_SILVER.getRGB());

                        // Nút Xóa
                        RenderUtils.drawRoundedRect(clearX, inY, clearW, inH, 3, hoverClear ? ColorUtils.ACCENT_RED.getRGB() : ColorUtils.BG_INPUT.getRGB());
                        RenderUtils.drawRoundedOutline(clearX, inY, clearW, inH, 3, 0.8f, hoverClear ? ColorUtils.TEXT_WHITE.getRGB() : ColorUtils.BORDER_INPUT.getRGB());
                        fontRenderer.drawString("x", (int) (clearX + (clearW - fontRenderer.getStringWidth("x")) / 2f), (int) (inY + 5f), hoverClear ? ColorUtils.TEXT_WHITE.getRGB() : ColorUtils.TEXT_MUTED.getRGB());
                    }

                    curY += cardH + 6f;
                }
            }

            if (activeSettingModule instanceof DiscordRemoteModule) {
                float testBtnY = curY + 4f;
                float testBtnW = 100f;
                boolean hoverTest = mouseX >= contentX + 6f && mouseX <= contentX + 6f + testBtnW && mouseY >= testBtnY && mouseY <= testBtnY + 20f;
                RenderUtils.drawRoundedRect(contentX + 6f, testBtnY, testBtnW, 20f, 4, hoverTest ? ColorUtils.BG_CARD_HOVER.getRGB() : ColorUtils.BG_INPUT.getRGB());
                RenderUtils.drawRoundedOutline(contentX + 6f, testBtnY, testBtnW, 20f, 4, 0.8f, hoverTest ? ColorUtils.BORDER_HOVER.getRGB() : ColorUtils.BORDER_INPUT.getRGB());
                fontRenderer.drawString("Gửi thử Webhook", (int) (contentX + 12f), (int) (testBtnY + 6f), hoverTest ? ColorUtils.PRIMARY_WHITE.getRGB() : ColorUtils.TEXT_SILVER.getRGB());
                curY += 26f;
            }

            float totalH = curY - (panelY + 64f + scrollOffset);
            maxScrollHeight = Math.max(0, (int) (totalH - (panelHeight - 80f)));

        } else {
            // ─────────────────────────────────────────────────────────────
            // DANH SÁCH MODULE LIST GỌN GÀNG
            // ─────────────────────────────────────────────────────────────
            float contentY = panelY + 42f + scrollOffset;

            if (selectedCategory == Category.HUD) {
                float bannerH = 26f;
                RenderUtils.drawRoundedRect(contentX, contentY, cardWidth, bannerH, 5f, ColorUtils.BG_CARD.getRGB());
                RenderUtils.drawRoundedOutline(contentX, contentY, cardWidth, bannerH, 5f, 0.8f, ColorUtils.BORDER_GLASS.getRGB());

                float editorBtnW = Math.min(180f, cardWidth * 0.65f);
                boolean hoverEditor = mouseX >= contentX + 6f && mouseX <= contentX + 6f + editorBtnW && mouseY >= contentY + 3f && mouseY <= contentY + 23f;
                if (hoverEditor) {
                    RenderUtils.drawRoundedGradientRect(contentX + 6f, contentY + 3f, editorBtnW, 20f, 4f, ColorUtils.ACCENT_START.getRGB(), ColorUtils.ACCENT_END.getRGB());
                } else {
                    RenderUtils.drawRoundedRect(contentX + 6f, contentY + 3f, editorBtnW, 20f, 4f, ColorUtils.BG_INPUT.getRGB());
                    RenderUtils.drawRoundedOutline(contentX + 6f, contentY + 3f, editorBtnW, 20f, 4f, 0.8f, ColorUtils.BORDER_ACTIVE.getRGB());
                }
                int editorTextColor = hoverEditor ? ColorUtils.BG_DARK.getRGB() : ColorUtils.PRIMARY_WHITE.getRGB();
                fontRenderer.drawString("Kéo thả vị trí HUD", (int) (contentX + 16f), (int) (contentY + 9f), editorTextColor);

                String hintF7 = "[ Phím tắt ẩn HUD: F7 ]";
                fontRenderer.drawString(hintF7, (int) (contentX + cardWidth - fontRenderer.getStringWidth(hintF7) - 8f), (int) (contentY + 9f), ColorUtils.TEXT_MUTED.getRGB());

                contentY += 34f;
            }

            List<Module> modules = CasxDuoisClient.getInstance().getModuleManager().getModulesByCategory(selectedCategory);

            for (Module module : modules) {
                float cardHeight = 36f;
                boolean isHover = mouseX >= contentX && mouseX <= contentX + cardWidth && mouseY >= contentY && mouseY <= contentY + cardHeight;

                int bgColor = isHover ? ColorUtils.BG_CARD_HOVER.getRGB() : ColorUtils.BG_CARD.getRGB();
                RenderUtils.drawRoundedRect(contentX, contentY, cardWidth, cardHeight, 6, bgColor);

                if (module.isToggled()) {
                    RenderUtils.drawRoundedOutline(contentX, contentY, cardWidth, cardHeight, 6, 1.0f, ColorUtils.BORDER_ACTIVE.getRGB());
                    // Vạch Gradient Accent bên trái biểu thị trạng thái Bật
                    RenderUtils.drawAccentIndicator(contentX + 2.5f, contentY + 6f, 2.5f, cardHeight - 12f, 1.25f);
                } else {
                    RenderUtils.drawRoundedOutline(contentX, contentY, cardWidth, cardHeight, 6, 0.8f, isHover ? ColorUtils.BORDER_HOVER.getRGB() : ColorUtils.BORDER_GLASS.getRGB());
                }

                // Chống tràn chữ tên và mô tả
                float rightControlsW = 120f;
                float maxTextW = cardWidth - rightControlsW - 18f;

                int titleColor = module.isToggled() ? ColorUtils.PRIMARY_WHITE.getRGB() : ColorUtils.TEXT_SILVER.getRGB();
                String vName = trimText(module.getVietnameseName(), maxTextW);
                String desc = trimText(module.getDescription(), maxTextW);

                fontRenderer.drawStringWithShadow(vName, (int) (contentX + 10f), (int) (contentY + 7f), titleColor);
                fontRenderer.drawString(desc, (int) (contentX + 10f), (int) (contentY + 20f), ColorUtils.TEXT_MUTED.getRGB());

                // Phím tắt Keybind
                String keyName = listeningKeybindModule == module ? "..." : (module.getKey() == Keyboard.KEY_NONE ? "[-]" : "[" + Keyboard.getKeyName(module.getKey()) + "]");
                float keyW = 28f;
                float keyX = contentX + cardWidth - 116f;
                RenderUtils.drawRoundedRect(keyX, contentY + 10f, keyW, 16f, 3, ColorUtils.BG_INPUT.getRGB());
                RenderUtils.drawRoundedOutline(keyX, contentY + 10f, keyW, 16f, 3, 0.8f, ColorUtils.BORDER_INPUT.getRGB());
                fontRenderer.drawString(keyName, (int) (keyX + (keyW - fontRenderer.getStringWidth(keyName)) / 2f), (int) (contentY + 14f), ColorUtils.TEXT_MUTED.getRGB());

                // Nút Cài Đặt
                float optBtnW = 46f;
                float optBtnX = contentX + cardWidth - 84f;
                boolean hoverOpt = mouseX >= optBtnX && mouseX <= optBtnX + optBtnW && mouseY >= contentY + 10f && mouseY <= contentY + 26f;
                RenderUtils.drawRoundedRect(optBtnX, contentY + 10f, optBtnW, 16f, 3, hoverOpt ? ColorUtils.BG_CARD_HOVER.getRGB() : ColorUtils.BG_INPUT.getRGB());
                RenderUtils.drawRoundedOutline(optBtnX, contentY + 10f, optBtnW, 16f, 3, 0.8f, hoverOpt ? ColorUtils.BORDER_HOVER.getRGB() : ColorUtils.BORDER_INPUT.getRGB());
                fontRenderer.drawString("Cài đặt", (int) (optBtnX + (optBtnW - fontRenderer.getStringWidth("Cài đặt")) / 2f), (int) (contentY + 14f), hoverOpt ? ColorUtils.PRIMARY_WHITE.getRGB() : ColorUtils.TEXT_SILVER.getRGB());

                // Công tắc Toggle Switch
                float toggleX = contentX + cardWidth - 32f;
                float toggleY = contentY + 11f;
                boolean hoverToggle = mouseX >= toggleX && mouseX <= toggleX + 28f && mouseY >= toggleY && mouseY <= toggleY + 16f;
                drawSwitch(toggleX, toggleY, module.isToggled(), hoverToggle);

                contentY += cardHeight + 6f;
            }

            float totalH = contentY - (panelY + 42f + scrollOffset);
            maxScrollHeight = Math.max(0, (int) (totalH - (panelHeight - 60f)));
        }

        RenderUtils.endScissor();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
