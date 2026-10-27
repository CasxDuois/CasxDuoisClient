package com.casxduois.client.mixin;

import com.casxduois.client.CasxDuoisClient;
import com.casxduois.client.modules.afk.AutoReconnectModule;
import com.casxduois.client.util.ColorUtils;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiDisconnected.class)
public class MixinGuiDisconnected extends GuiScreen {

    private int reconnectTimer = -1;
    private long lastTimerTick = 0;

    @Inject(method = {"initGui", "func_73866_w_"}, at = @At("RETURN"), remap = false)
    private void onInitGui(CallbackInfo ci) {
        AutoReconnectModule autoReconnect = CasxDuoisClient.getInstance().getModuleManager().getModule(AutoReconnectModule.class);
        if (autoReconnect != null && autoReconnect.isToggled()) {
            reconnectTimer = (int) autoReconnect.delay.getValue().doubleValue();
            lastTimerTick = System.currentTimeMillis();
        }
    }

    @Inject(method = {"drawScreen", "func_73863_a"}, at = @At("RETURN"), remap = false)
    private void onDrawScreen(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {

        AutoReconnectModule autoReconnect = CasxDuoisClient.getInstance().getModuleManager().getModule(AutoReconnectModule.class);
        if (autoReconnect != null && autoReconnect.isToggled() && reconnectTimer >= 0) {
            if (System.currentTimeMillis() - lastTimerTick >= 1000) {
                reconnectTimer--;
                lastTimerTick = System.currentTimeMillis();
                if (reconnectTimer <= 0) {
                    ServerData lastServer = autoReconnect.getLastServerData();
                    if (lastServer != null) {
                        mc.displayGuiScreen(new GuiConnecting(this, mc, lastServer));
                        return;
                    }
                }
            }

            int cardW = 240;
            int cardH = 26;
            int cardX = this.width / 2 - cardW / 2;
            int cardY = this.height / 2 + 50;

            com.casxduois.client.util.RenderUtils.drawRoundedRect(cardX, cardY, cardW, cardH, 6, ColorUtils.BG_CARD.getRGB());
            com.casxduois.client.util.RenderUtils.drawRoundedOutline(cardX, cardY, cardW, cardH, 6, 1.0f, ColorUtils.BORDER_CARD.getRGB());

            String timerText = "§7Tự động kết nối lại sau: §a" + reconnectTimer + "s";
            this.drawCenteredString(this.fontRenderer, timerText, this.width / 2, cardY + 9, ColorUtils.TEXT_WHITE.getRGB());
        }
    }
}
