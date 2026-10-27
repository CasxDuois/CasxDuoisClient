package com.casxduois.client.mixin;

import com.casxduois.client.CasxDuoisClient;
import com.casxduois.client.modules.afk.AutoReconnectModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MixinGuiConnecting: Ghi nhận ServerData của server đang kết nối để Auto Reconnect có thông tin kết nối lại
 */
@Mixin(GuiConnecting.class)
public class MixinGuiConnecting extends GuiScreen {

    @Inject(method = "<init>(Lnet/minecraft/client/gui/GuiScreen;Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/multiplayer/ServerData;)V", at = @At("RETURN"), remap = false)
    private void onInit(GuiScreen parent, Minecraft mcIn, ServerData serverDataIn, CallbackInfo ci) {
        if (CasxDuoisClient.getInstance() != null && CasxDuoisClient.getInstance().getModuleManager() != null) {
            AutoReconnectModule autoReconnect = CasxDuoisClient.getInstance().getModuleManager().getModule(AutoReconnectModule.class);
            if (autoReconnect != null && serverDataIn != null) {
                autoReconnect.setLastServerData(serverDataIn);
            }
        }
    }

}
