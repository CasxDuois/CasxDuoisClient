package com.casxduois.client.gui.menu;

import com.casxduois.client.CasxDuoisClient;
import com.casxduois.client.gui.account.GuiInGameAccountSwitcher;
import com.casxduois.client.util.ColorUtils;
import com.casxduois.client.util.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiWorldSelection;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import com.casxduois.client.update.UpdateManager;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * CasxDuoisMainMenu: Giao diện Main Menu kính mờ Obsidian Glassmorphism cao cấp
 * - Tối ưu 3 nút bấm cốt lõi: Chơi đơn, Chơi mạng, Tài khoản
 * - Load DynamicTexture trực tiếp tránh lỗi thiếu texture (purple checkerboard)
 * - Lớp phủ gradient làm tối hài hòa với Dark Theme
 * - Bụi sáng bokeh chuyển động (Animated Ambient Particles)
 * - Tương thích hoàn hảo mọi GUI scale
 */
public class CasxDuoisMainMenu extends GuiScreen {

    private static ResourceLocation backgroundTexture = null;
    private static boolean textureLoaded = false;
    public static float bgWidth = 1024f;
    public static float bgHeight = 563f;

    public static ResourceLocation getBackgroundTexture() {
        if (backgroundTexture != null) {
            return backgroundTexture;
        }
        if (textureLoaded) {
            return null;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.getTextureManager() == null) {
            return null;
        }
        textureLoaded = true;
        try {
            InputStream is = CasxDuoisMainMenu.class.getResourceAsStream("/assets/casxduois/textures/gui/background.png");
            if (is == null) {
                is = CasxDuoisMainMenu.class.getClassLoader().getResourceAsStream("assets/casxduois/textures/gui/background.png");
            }
            if (is == null) {
                is = CasxDuoisMainMenu.class.getResourceAsStream("/assets/minecraft/textures/gui/background.png");
            }
            if (is != null) {
                BufferedImage image = ImageIO.read(is);
                is.close();
                if (image != null) {
                    bgWidth = image.getWidth();
                    bgHeight = image.getHeight();
                    DynamicTexture dynamicTexture = new DynamicTexture(image);
                    backgroundTexture = mc.getTextureManager().getDynamicTextureLocation("casxduois_main_bg", dynamicTexture);
                }
            }
        } catch (Throwable t) {
            CasxDuoisClient.LOGGER.error("Failed to load custom background texture", t);
        }
        return backgroundTexture;
    }

    private static class MenuButton {
        final int id;
        final String text;
        float x, y, width, height;
        float hoverProgress = 0.0f;

        MenuButton(int id, String text) {
            this.id = id;
            this.text = text;
        }
    }

    private static class MenuParticle {
        float x, y;
        float size;
        float speed;
        float baseAlpha;
        float phase;

        MenuParticle(float x, float y, float size, float speed, float baseAlpha) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.speed = speed;
            this.baseAlpha = baseAlpha;
            this.phase = (float) (Math.random() * Math.PI * 2);
        }
    }

    private final List<MenuButton> buttons = new ArrayList<>();
    private final List<MenuParticle> particles = new ArrayList<>();
    private long openTime = 0L;

    @Override
    public void initGui() {
        buttons.clear();
        particles.clear();
        openTime = System.currentTimeMillis();

        updateWindowTitle();

        // 1. Tạo danh sách hạt bokeh bay chuyển động
        Random rand = new Random();
        for (int i = 0; i < 40; i++) {
            float px = rand.nextFloat() * (width > 0 ? width : 800);
            float py = rand.nextFloat() * (height > 0 ? height : 600);
            float psize = 1.2f + rand.nextFloat() * 2.2f;
            float pspeed = 0.15f + rand.nextFloat() * 0.35f;
            float palpha = 0.15f + rand.nextFloat() * 0.35f;
            particles.add(new MenuParticle(px, py, psize, pspeed, palpha));
        }

        // 2. Chỉ 3 nút bấm thiết yếu theo yêu cầu
        buttons.add(new MenuButton(0, "Chơi đơn"));
        buttons.add(new MenuButton(1, "Chơi mạng"));
        buttons.add(new MenuButton(2, "Tài khoản"));
    }

    private void updateWindowTitle() {
        String accountName = getAccountName();
        if (Display.isActive() || Display.isCreated()) {
            Display.setTitle(accountName);
        }
    }

    private String getAccountName() {
        return (mc != null && mc.getSession() != null && mc.getSession().getUsername() != null)
                ? mc.getSession().getUsername()
                : "Player";
    }

    private String trimText(String text, float maxWidth) {
        if (text == null) return "";
        if (fontRenderer.getStringWidth(text) <= maxWidth) return text;
        return fontRenderer.trimStringToWidth(text, (int) Math.max(10, maxWidth - 8)) + "...";
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (UpdateManager.getInstance().hasUpdate() && !UpdateManager.getInstance().isModalDismissed()) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                UpdateManager.getInstance().setModalDismissed(true);
                return;
            }
        }

        if (keyCode == Keyboard.KEY_RSHIFT) {
            mc.displayGuiScreen(CasxDuoisClient.getInstance().getClickGui());
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (UpdateManager.getInstance().hasUpdate()) {
            // A. Khi Hộp thoại Modal đang mở
            if (!UpdateManager.getInstance().isModalDismissed()) {
                float modalW = Math.min(320f, width - 24f);
                float modalH = Math.min(210f, height - 24f);
                float modalX = (width - modalW) / 2f;
                float modalY = (height - modalH) / 2f;

                float closeW = 16f, closeH = 16f;
                float closeX = modalX + modalW - closeW - 8f;
                float closeY = modalY + 8f;

                float btnDlW = modalW - 110f;
                float btnDlH = 22f;
                float btnDlX = modalX + 12f;
                float btnDlY = modalY + modalH - 32f;

                float btnLaterW = 74f;
                float btnLaterH = 22f;
                float btnLaterX = modalX + modalW - btnLaterW - 12f;
                float btnLaterY = modalY + modalH - 32f;

                // Nút Đóng [x] hoặc [Để sau]
                if ((mouseX >= closeX && mouseX <= closeX + closeW && mouseY >= closeY && mouseY <= closeY + closeH) ||
                    (mouseX >= btnLaterX && mouseX <= btnLaterX + btnLaterW && mouseY >= btnLaterY && mouseY <= btnLaterY + btnLaterH)) {
                    UpdateManager.getInstance().setModalDismissed(true);
                    return;
                }

                // Nút [TẢI BẢN MỚI NGAY]
                if (mouseX >= btnDlX && mouseX <= btnDlX + btnDlW && mouseY >= btnDlY && mouseY <= btnDlY + btnDlH) {
                    UpdateManager.getInstance().openDownloadLink();
                    return;
                }

                // Nhấp ra ngoài modal -> đóng modal
                if (mouseX < modalX || mouseX > modalX + modalW || mouseY < modalY || mouseY > modalY + modalH) {
                    UpdateManager.getInstance().setModalDismissed(true);
                    return;
                }

                return; // Chặn các nút menu bên dưới khi modal đang hiển thị
            }

            // B. Khi Modal đã đóng: Xử lý click trên Floating Banner ở mép trên màn hình
            float banW = Math.min(360f, width - 20f);
            float banH = 24f;
            float banX = (width - banW) / 2f;
            float banY = 8f;

            float bDlW = 62f;
            float bDlX = banX + banW - bDlW - 6f;
            float bDlY = banY + 3f;

            float bInfoW = 78f;
            float bInfoX = bDlX - bInfoW - 5f;
            float bInfoY = banY + 3f;

            if (mouseX >= bDlX && mouseX <= bDlX + bDlW && mouseY >= bDlY && mouseY <= bDlY + 18f) {
                UpdateManager.getInstance().openDownloadLink();
                return;
            }

            if (mouseX >= bInfoX && mouseX <= bInfoX + bInfoW && mouseY >= bInfoY && mouseY <= bInfoY + 18f) {
                UpdateManager.getInstance().setModalDismissed(false);
                return;
            }

            // Click vào thông báo phiên bản ở footer
            if (height >= 210 && mouseX >= 10 && mouseX <= 320 && mouseY >= height - 16 && mouseY <= height) {
                UpdateManager.getInstance().openDownloadLink();
                return;
            }
        }

        for (MenuButton btn : buttons) {
            if (mouseX >= btn.x && mouseX <= btn.x + btn.width && mouseY >= btn.y && mouseY <= btn.y + btn.height) {
                switch (btn.id) {
                    case 0:
                        mc.displayGuiScreen(new GuiWorldSelection(this));
                        break;
                    case 1:
                        mc.displayGuiScreen(new GuiMultiplayer(this));
                        break;
                    case 2:
                        mc.displayGuiScreen(new GuiInGameAccountSwitcher(this));
                        break;
                }
                return;
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        long now = System.currentTimeMillis();

        // 1. VẼ ẢNH NỀN FANTASY COTTAGE (COVER SCALING TỶ LỆ CHUẨN, KHÔNG BỊ BIẾN DẠNG)
        ResourceLocation bg = getBackgroundTexture();
        if (bg != null) {
            float imgW = bgWidth;
            float imgH = bgHeight;
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

        // 2. LỚP PHỦ LÀM TỐI OBSIDIAN ĐỂ HỢP MÀU & TÔN VẺ ĐẸP CỦA GIAO DIỆN KÍNH
        RenderUtils.drawGradientRect(0, 0, width, height, new Color(7, 10, 18, 145).getRGB(), new Color(3, 4, 7, 215).getRGB());

        int centerX = width / 2;
        int centerY = height / 2;

        // 3. KÍCH THƯỚC CARD & HIỆU ỨNG TRƯỢT MƯỢT (ENTRANCE ANIMATION)
        float cardWidth = Math.min(210f, width - 24f);
        float cardHeight = 104f;

        // Animation trượt vào khi vừa mở menu (Ease-out cubic)
        float animProgress = Math.min(1.0f, (now - openTime) / 360.0f);
        float ease = 1.0f - (float) Math.pow(1.0 - animProgress, 3);
        float slideOffset = (1.0f - ease) * 14.0f;

        float cardX = centerX - cardWidth / 2f;
        float cardY = centerY - (cardHeight / 2f) + 12f + slideOffset;

        if (cardY + cardHeight > height - 16f) {
            cardY = height - cardHeight - 16f;
        }

        float titleY = cardY - 26f;
        if (titleY < 6f) {
            titleY = 6f;
            cardY = titleY + 24f;
        }

        // 4. VẦNG SÁNG AMBIENT BACKLIGHT PHÍA SAU CARD
        float glowCenterY = cardY + cardHeight / 2f;
        RenderUtils.drawCircle(centerX, glowCenterY, cardWidth * 0.70f, new Color(28, 44, 76, 26).getRGB());
        RenderUtils.drawCircle(centerX, glowCenterY, cardWidth * 0.46f, new Color(42, 66, 112, 36).getRGB());
        RenderUtils.drawCircle(centerX, glowCenterY, cardWidth * 0.26f, new Color(65, 96, 158, 42).getRGB());

        // 5. BỤI SÁNG CHUYỂN ĐỘNG (ANIMATED AMBIENT PARTICLES)
        for (MenuParticle p : particles) {
            p.y -= p.speed;
            p.x += (float) Math.sin((now / 1300.0) + p.phase) * 0.35f;

            if (p.y < -10f) {
                p.y = height + 10f;
                p.x = (float) (Math.random() * width);
            }

            float currentAlpha = p.baseAlpha * (float) (0.65 + 0.35 * Math.sin(now / 900.0 + p.phase));
            int pColor = new Color(210, 230, 255, Math.min(255, Math.max(0, (int) (currentAlpha * 255)))).getRGB();
            RenderUtils.drawCircle(p.x, p.y, p.size, pColor);
        }

        // 6. HERO SECTION: Title = Account Name + Status (Không lặp lại full-caps)
        String accountName = getAccountName();
        int titleW = fontRenderer.getStringWidth(accountName);
        fontRenderer.drawStringWithShadow(accountName, centerX - titleW / 2, (int) titleY, ColorUtils.PRIMARY_WHITE.getRGB());

        String statusLabel = "Sẵn sàng";
        int statusW = fontRenderer.getStringWidth(statusLabel) + 10;
        RenderUtils.drawCircle(centerX - statusW / 2f + 2f, titleY + 14.5f, 2.5f, ColorUtils.ACCENT_GREEN.getRGB());
        fontRenderer.drawString(statusLabel, centerX - statusW / 2 + 8, (int) titleY + 11, ColorUtils.TEXT_SILVER.getRGB());

        // 7. MAIN CARD: Glassmorphism với viền gradient accent mép trên
        RenderUtils.drawGlassPanel(cardX, cardY, cardWidth, cardHeight, 10f);

        // 8. CÁC NÚT BẤM (CHỈ 3 NÚT: CHƠI ĐƠN, CHƠI MẠNG, TÀI KHOẢN)
        float btnWidth = cardWidth - 18f;
        float btnHeight = 24f;
        float btnSpacing = 6f;
        float startBtnY = cardY + 10f;

        for (int i = 0; i < buttons.size(); i++) {
            MenuButton btn = buttons.get(i);
            btn.width = btnWidth;
            btn.height = btnHeight;
            btn.x = centerX - btn.width / 2f;
            btn.y = startBtnY + i * (btnHeight + btnSpacing);

            boolean isHover = mouseX >= btn.x && mouseX <= btn.x + btn.width && mouseY >= btn.y && mouseY <= btn.y + btn.height;

            // Nội suy chuyển động hover mượt mà
            float targetHover = isHover ? 1.0f : 0.0f;
            btn.hoverProgress += (targetHover - btn.hoverProgress) * 0.22f;

            float expand = btn.hoverProgress * 2.5f;

            // Nút Chơi mạng (id = 1) là Primary Action có điểm nhấn Gradient Accent
            if (btn.id == 1 && btn.hoverProgress > 0.05f) {
                RenderUtils.drawRoundedGradientRect(btn.x - expand / 2f, btn.y, btn.width + expand, btn.height, 4f,
                        ColorUtils.ACCENT_START.getRGB(), ColorUtils.ACCENT_END.getRGB());
                RenderUtils.drawRoundedOutline(btn.x - expand / 2f, btn.y, btn.width + expand, btn.height, 4f, 0.8f,
                        ColorUtils.PRIMARY_WHITE.getRGB());
            } else {
                Color idleBg = (btn.id == 1) ? ColorUtils.BG_CARD_HOVER : ColorUtils.BG_INPUT;
                Color hoverBg = ColorUtils.BG_CARD_HOVER;
                Color currentBg = ColorUtils.blend(idleBg, hoverBg, btn.hoverProgress);

                Color idleBorder = (btn.id == 1) ? ColorUtils.BORDER_ACTIVE : ColorUtils.BORDER_GLASS;
                Color hoverBorder = ColorUtils.BORDER_HOVER;
                Color currentBorder = ColorUtils.blend(idleBorder, hoverBorder, btn.hoverProgress);

                RenderUtils.drawRoundedRect(btn.x - expand / 2f, btn.y, btn.width + expand, btn.height, 4f, currentBg.getRGB());
                RenderUtils.drawRoundedOutline(btn.x - expand / 2f, btn.y, btn.width + expand, btn.height, 4f, 0.8f, currentBorder.getRGB());
            }

            String fullText = btn.text;
            Color currentTextColor = (btn.id == 1 && isHover) ? ColorUtils.PRIMARY_WHITE : ColorUtils.blend(ColorUtils.TEXT_SILVER, ColorUtils.PRIMARY_WHITE, btn.hoverProgress);
            float textShift = btn.hoverProgress * 1.0f;
            fontRenderer.drawStringWithShadow(fullText, btn.x + (btn.width - fontRenderer.getStringWidth(fullText)) / 2f + textShift, btn.y + (btn.height - 8f) / 2f, currentTextColor.getRGB());
        }

        // 9. FOOTER THÔNG TIN
        if (height >= 210) {
            String clientVer = "CasxDuois Client v" + CasxDuoisClient.CLIENT_VERSION;
            if (UpdateManager.getInstance().hasUpdate()) {
                clientVer += " · Có bản mới: v" + UpdateManager.getInstance().getLatestVersion();
            }
            fontRenderer.drawString(clientVer, 12, height - 12, ColorUtils.TEXT_MUTED.getRGB());
            String tip = "Bấm [R-SHIFT] để mở Cài đặt";
            fontRenderer.drawString(tip, width - fontRenderer.getStringWidth(tip) - 12, height - 12, ColorUtils.TEXT_MUTED.getRGB());
        }

        // 10. THÔNG BÁO CẬP NHẬT GITHUB RELEASES (MODAL HOẶC FLOATING BANNER)
        if (UpdateManager.getInstance().hasUpdate()) {
            if (!UpdateManager.getInstance().isModalDismissed()) {
                // HỘP THOẠI MODAL NỔI BẬT KHI VỪA MỞ CLIENT LÊN
                float modalW = Math.min(320f, width - 24f);
                float modalH = Math.min(210f, height - 24f);
                float modalX = (width - modalW) / 2f;
                float modalY = (height - modalH) / 2f;

                float closeW = 16f, closeH = 16f;
                float closeX = modalX + modalW - closeW - 8f;
                float closeY = modalY + 8f;

                float btnDlW = modalW - 110f;
                float btnDlH = 22f;
                float btnDlX = modalX + 12f;
                float btnDlY = modalY + modalH - 32f;

                float btnLaterW = 74f;
                float btnLaterH = 22f;
                float btnLaterX = modalX + modalW - btnLaterW - 12f;
                float btnLaterY = modalY + modalH - 32f;

                // Lớp mờ bóng tối phủ kín màn hình
                RenderUtils.drawGradientRect(0, 0, width, height, new Color(4, 6, 12, 175).getRGB(), new Color(2, 3, 6, 215).getRGB());

                // Modal Glassmorphism
                RenderUtils.drawGlassPanel(modalX, modalY, modalW, modalH, 10f);

                // Nút [x] đóng
                boolean hoverX = mouseX >= closeX && mouseX <= closeX + closeW && mouseY >= closeY && mouseY <= closeY + closeH;
                fontRenderer.drawString(hoverX ? "x" : "x", (int) closeX + 5, (int) closeY + 3, hoverX ? ColorUtils.PRIMARY_WHITE.getRGB() : ColorUtils.TEXT_MUTED.getRGB());

                // Tiêu đề & Accent Dot
                RenderUtils.drawAccentDot(modalX + 16f, modalY + 14f, 3f, true);
                String modalTitle = "Đã có bản cập nhật mới";
                fontRenderer.drawStringWithShadow(modalTitle, (int) (modalX + 24f), (int) (modalY + 10f), ColorUtils.PRIMARY_WHITE.getRGB());

                String verBadge = "Hiện tại: v" + CasxDuoisClient.CLIENT_VERSION + " ➔ Mới nhất: v" + UpdateManager.getInstance().getLatestVersion();
                fontRenderer.drawString(verBadge, (int) (modalX + 14f), (int) (modalY + 24f), ColorUtils.TEXT_SILVER.getRGB());

                // Khung tóm tắt changelog
                float boxX = modalX + 12f;
                float boxY = modalY + 38f;
                float boxW = modalW - 24f;
                float boxH = modalH - 80f;

                RenderUtils.drawRoundedRect(boxX, boxY, boxW, boxH, 6f, ColorUtils.BG_INPUT.getRGB());
                RenderUtils.drawRoundedOutline(boxX, boxY, boxW, boxH, 6f, 0.8f, ColorUtils.BORDER_GLASS.getRGB());

                String releaseName = UpdateManager.getInstance().getReleaseTitle();
                if (releaseName != null && !releaseName.isEmpty()) {
                    fontRenderer.drawString(trimText(releaseName, boxW - 14f), (int) (boxX + 8f), (int) (boxY + 6f), ColorUtils.PRIMARY_WHITE.getRGB());
                }

                String log = UpdateManager.getInstance().getChangelog();
                if (log == null || log.trim().isEmpty()) {
                    log = "• Bản cập nhật mới với nhiều tính năng và tối ưu hóa.\n• Nhấp vào nút Tải bản mới ngay để tải file cập nhật.";
                }

                String[] logLines = log.split("\\r?\\n");
                float lineY = boxY + 20f;
                for (String l : logLines) {
                    if (lineY > boxY + boxH - 12f) break;
                    String trimmedL = trimText(l, boxW - 16f);
                    fontRenderer.drawString(trimmedL, (int) (boxX + 8f), (int) lineY, ColorUtils.TEXT_MUTED.getRGB());
                    lineY += 10f;
                }

                // Nút [Tải bản mới ngay] với Gradient Accent
                boolean hoverDl = mouseX >= btnDlX && mouseX <= btnDlX + btnDlW && mouseY >= btnDlY && mouseY <= btnDlY + btnDlH;
                if (hoverDl) {
                    RenderUtils.drawRoundedGradientRect(btnDlX, btnDlY, btnDlW, btnDlH, 4f, ColorUtils.ACCENT_START.getRGB(), ColorUtils.ACCENT_END.getRGB());
                    RenderUtils.drawRoundedOutline(btnDlX, btnDlY, btnDlW, btnDlH, 4f, 0.8f, ColorUtils.PRIMARY_WHITE.getRGB());
                } else {
                    RenderUtils.drawRoundedRect(btnDlX, btnDlY, btnDlW, btnDlH, 4f, ColorUtils.BG_CARD_HOVER.getRGB());
                    RenderUtils.drawRoundedOutline(btnDlX, btnDlY, btnDlW, btnDlH, 4f, 0.8f, ColorUtils.BORDER_ACTIVE.getRGB());
                }
                String dlText = "Tải bản mới ngay";
                int dlCol = hoverDl ? ColorUtils.PRIMARY_WHITE.getRGB() : ColorUtils.PRIMARY_WHITE.getRGB();
                fontRenderer.drawString(dlText, (int) (btnDlX + (btnDlW - fontRenderer.getStringWidth(dlText)) / 2f), (int) (btnDlY + 7f), dlCol);

                // Nút [Để sau]
                boolean hoverLater = mouseX >= btnLaterX && mouseX <= btnLaterX + btnLaterW && mouseY >= btnLaterY && mouseY <= btnLaterY + btnLaterH;
                RenderUtils.drawRoundedRect(btnLaterX, btnLaterY, btnLaterW, btnLaterH, 4f, hoverLater ? ColorUtils.BG_CARD_HOVER.getRGB() : ColorUtils.BG_INPUT.getRGB());
                RenderUtils.drawRoundedOutline(btnLaterX, btnLaterY, btnLaterW, btnLaterH, 4f, 0.8f, hoverLater ? ColorUtils.BORDER_HOVER.getRGB() : ColorUtils.BORDER_GLASS.getRGB());
                String laterText = "Để sau";
                fontRenderer.drawString(laterText, (int) (btnLaterX + (btnLaterW - fontRenderer.getStringWidth(laterText)) / 2f), (int) (btnLaterY + 7f), ColorUtils.TEXT_MUTED.getRGB());

            } else {
                // BANNER TINH GỌN GHIM MÉP TRÊN (KHI ĐÃ ĐÓNG MODAL)
                float banW = Math.min(360f, width - 20f);
                float banH = 24f;
                float banX = (width - banW) / 2f;
                float banY = 8f;

                RenderUtils.drawGlassPanel(banX, banY, banW, banH, 5f);

                RenderUtils.drawCircle(banX + 12f, banY + 12f, 2.5f, ColorUtils.ACCENT_GREEN.getRGB());
                String banTitle = "Có bản mới: v" + UpdateManager.getInstance().getLatestVersion();
                fontRenderer.drawString(banTitle, (int) (banX + 18f), (int) (banY + 8f), ColorUtils.PRIMARY_WHITE.getRGB());

                float bDlW = 62f;
                float bDlX = banX + banW - bDlW - 6f;
                float bDlY = banY + 3f;
                boolean hDl = mouseX >= bDlX && mouseX <= bDlX + bDlW && mouseY >= bDlY && mouseY <= bDlY + 18f;
                if (hDl) {
                    RenderUtils.drawRoundedGradientRect(bDlX, bDlY, bDlW, 18f, 3f, ColorUtils.ACCENT_START.getRGB(), ColorUtils.ACCENT_END.getRGB());
                } else {
                    RenderUtils.drawRoundedRect(bDlX, bDlY, bDlW, 18f, 3f, ColorUtils.BG_INPUT.getRGB());
                    RenderUtils.drawRoundedOutline(bDlX, bDlY, bDlW, 18f, 3f, 0.8f, ColorUtils.BORDER_ACTIVE.getRGB());
                }
                int dlTxtCol = ColorUtils.PRIMARY_WHITE.getRGB();
                fontRenderer.drawString("Tải ngay", (int) (bDlX + (bDlW - fontRenderer.getStringWidth("Tải ngay")) / 2f), (int) (bDlY + 5f), dlTxtCol);

                float bInfoW = 78f;
                float bInfoX = bDlX - bInfoW - 5f;
                float bInfoY = banY + 3f;
                boolean hInfo = mouseX >= bInfoX && mouseX <= bInfoX + bInfoW && mouseY >= bInfoY && mouseY <= bInfoY + 18f;
                RenderUtils.drawRoundedRect(bInfoX, bInfoY, bInfoW, 18f, 3f, hInfo ? ColorUtils.BG_CARD_HOVER.getRGB() : ColorUtils.BG_INPUT.getRGB());
                RenderUtils.drawRoundedOutline(bInfoX, bInfoY, bInfoW, 18f, 3f, 0.8f, hInfo ? ColorUtils.BORDER_HOVER.getRGB() : ColorUtils.BORDER_GLASS.getRGB());
                fontRenderer.drawString("Xem chi tiết", (int) (bInfoX + (bInfoW - fontRenderer.getStringWidth("Xem chi tiết")) / 2f), (int) (bInfoY + 5f), ColorUtils.TEXT_SILVER.getRGB());
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
