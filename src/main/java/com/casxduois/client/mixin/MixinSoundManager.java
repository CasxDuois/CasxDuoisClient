package com.casxduois.client.mixin;

import com.casxduois.client.CasxDuoisClient;
import com.casxduois.client.modules.performance.DynamicFPSModule;
import net.minecraft.client.audio.SoundManager;
import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundManager.class)
public class MixinSoundManager {

    @Inject(method = {"updateAllSounds", "func_148605_d"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void onUpdateAllSounds(CallbackInfo ci) {
        DynamicFPSModule dynamicFPS = CasxDuoisClient.getInstance().getModuleManager().getModule(DynamicFPSModule.class);
        // Khi cửa sổ đang chạy ngầm / unfocused, tạm dừng cập nhật âm thanh liên tục để tiết kiệm xung CPU
        if (dynamicFPS != null && dynamicFPS.isToggled() && !Display.isActive()) {
            ci.cancel();
        }
    }
}
