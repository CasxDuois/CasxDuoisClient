package com.casxduois.client.mixin;

import com.casxduois.client.gui.proxy.GuiProxyManager;
import com.casxduois.client.proxy.ProxyManager;
import com.casxduois.client.util.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.init.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.Color;

/**
 * MixinGuiMultiplayer: Hiển thị Nút Proxy / AutoVPN ở góc trên bên phải khi vào màn hình Chơi Mạng
 * Thiết kế Obsidian Frosted Glass cao cấp với đèn trạng thái LED thời gian thực và Tooltip thông minh
 */
@Mixin(GuiMultiplayer.class)
public abstract class MixinGuiMultiplayer extends GuiScreen {

    private static String quickVnStatus = null;
    private static long quickVnTime = 0;

    @Inject(method = {"drawScreen", "func_73863_a"}, at = @At("TAIL"))
    private void onDrawScreen(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        float btnW = 142f;
        float btnH = 20f;
        float btnX = (float) this.width - btnW - 10f;
        float btnY = 8f;

        boolean isHover = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;

        ProxyManager pm = ProxyManager.getInstance();
        boolean active = pm.isProxyEnabled();
        boolean autoRotate = pm.isAutoRotate();

        int bg;
        int border;
        String statusDot;
        String label;

        if (active) {
            bg = isHover ? new Color(20, 36, 30, 240).getRGB() : new Color(14, 25, 20, 220).getRGB();
            border = isHover ? new Color(90, 255, 170, 255).getRGB() : new Color(50, 200, 130, 200).getRGB();
            statusDot = "§a● ";
            label = autoRotate ? "AutoVPN: BẬT" : (pm.getProxyType() + ": BẬT");
        } else {
            bg = isHover ? new Color(28, 32, 42, 240).getRGB() : new Color(14, 16, 22, 215).getRGB();
            border = isHover ? new Color(255, 255, 255, 220).getRGB() : new Color(50, 58, 76, 170).getRGB();
            statusDot = "§7○ ";
            label = "Proxy / VPN: TẮT";
        }

        // 1. Nền nút Bo tròn & Viền phát sáng
        RenderUtils.drawRoundedRect(btnX, btnY, btnW, btnH, 5, bg);
        RenderUtils.drawRoundedOutline(btnX, btnY, btnW, btnH, 5, 1.0f, border);

        // 2. Nội dung text căn giữa
        String fullText = statusDot + "§f" + label;
        float textW = fontRenderer.getStringWidth(fullText);
        float textX = btnX + (btnW - textW) / 2f;
        float textY = btnY + (btnH - fontRenderer.FONT_HEIGHT) / 2f;
        fontRenderer.drawStringWithShadow(fullText, textX, textY, 0xFFFFFF);

        // 3. Thông báo nhanh khi vừa quét Proxy VN bằng chuột phải
        if (quickVnStatus != null && System.currentTimeMillis() - quickVnTime < 4500) {
            float notifW = fontRenderer.getStringWidth(quickVnStatus) + 20f;
            float notifH = 18f;
            float notifX = ((float) this.width - notifW) / 2f;
            float notifY = 6f;
            RenderUtils.drawRoundedRect(notifX, notifY, notifW, notifH, 4, new Color(14, 22, 18, 245).getRGB());
            RenderUtils.drawRoundedOutline(notifX, notifY, notifW, notifH, 4, 1.0f, new Color(50, 220, 130, 220).getRGB());
            fontRenderer.drawStringWithShadow(quickVnStatus, notifX + 10f, notifY + 5f, 0xFFFFFF);
        }

        // 4. Tooltip nổi thông minh khi rê chuột
        if (isHover) {
            float tipW = 210f;
            float tipH = active ? 56f : 44f;
            float tipX = (float) this.width - tipW - 10f;
            float tipY = btnY + btnH + 6f;

            int cardBg = new Color(10, 12, 16, 245).getRGB();
            int cardBorder = new Color(60, 72, 95, 220).getRGB();

            RenderUtils.drawRoundedRect(tipX, tipY, tipW, tipH, 4, cardBg);
            RenderUtils.drawRoundedOutline(tipX, tipY, tipW, tipH, 4, 0.8f, cardBorder);

            fontRenderer.drawStringWithShadow("§f🛡 Quản lý Proxy & AutoVPN", tipX + 8f, tipY + 6f, 0xFFFFFF);
            if (active) {
                fontRenderer.drawStringWithShadow("§7IP: §a" + pm.getHost() + ":" + pm.getPort() + " §8(" + pm.getProxyType() + ")", tipX + 8f, tipY + 18f, 0xCCCCCC);
                fontRenderer.drawStringWithShadow("§b➡ Chuột trái: Mở Cài đặt / Tắt", tipX + 8f, tipY + 30f, 0x55FFFF);
                fontRenderer.drawStringWithShadow("§a➡ Chuột phải: 🇻🇳 Auto Tìm Proxy VN", tipX + 8f, tipY + 42f, 0x77FFAA);
            } else {
                fontRenderer.drawStringWithShadow("§b➡ Chuột trái: Mở Cài đặt cấu hình", tipX + 8f, tipY + 18f, 0xAAAAAA);
                fontRenderer.drawStringWithShadow("§a➡ Chuột phải: 🇻🇳 Auto Tìm & Bật Proxy VN", tipX + 8f, tipY + 30f, 0x77FFAA);
            }
        }
    }

    @Inject(method = {"mouseClicked", "func_73864_a"}, at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        float btnW = 142f;
        float btnH = 20f;
        float btnX = (float) this.width - btnW - 10f;
        float btnY = 8f;

        if (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
            if (mouseButton == 0) {
                // Chuột trái: Mở giao diện GuiProxyManager
                Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                Minecraft.getMinecraft().displayGuiScreen(new GuiProxyManager(this));
                ci.cancel();
            } else if (mouseButton == 1) {
                // Chuột phải: Kích hoạt Auto Quét & Kết nối Proxy Việt Nam ngay lập tức
                Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.2F));
                quickVnStatus = "⏳ Đang quét Proxy Việt Nam nhanh nhất...";
                quickVnTime = System.currentTimeMillis();
                ProxyManager.getInstance().autoSetupVietnamProxy(true, res -> {
                    if (res.success && res.bestProxy != null) {
                        quickVnStatus = "§a✔ Đã bật Proxy VN: " + res.bestProxy.ip + ":" + res.bestProxy.port + " (" + res.bestProxy.pingMs + "ms)";
                    } else {
                        quickVnStatus = "§c✘ Lỗi Proxy VN: " + res.message;
                    }
                    quickVnTime = System.currentTimeMillis();
                });
                ci.cancel();
            }
        }
    }
}
