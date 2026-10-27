package com.casxduois.client.mixin;

import com.casxduois.client.util.ColorUtils;
import com.casxduois.client.util.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MixinGuiScreen: Tùy biến hình nền mặc định của tất cả các màn hình trong game
 * Khi ở ngoài sảnh chờ / menu (mc.world == null):
 * Thay thế hình nền đất đá Minecraft (dirt background) bằng nền Deep Obsidian #07080B
 * đồng bộ chuẩn xác 100% với phối màu của UI Client Launcher.
 */
@Mixin(GuiScreen.class)
public abstract class MixinGuiScreen {

    @Inject(method = {"drawDefaultBackground", "func_146276_q_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void onDrawDefaultBackground(CallbackInfo ci) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null) {
            ScaledResolution sr = new ScaledResolution(mc);
            RenderUtils.drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), ColorUtils.BG_DARK.getRGB());
            ci.cancel();
        }
    }
}
