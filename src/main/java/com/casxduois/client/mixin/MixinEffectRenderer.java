package com.casxduois.client.mixin;

import com.casxduois.client.CasxDuoisClient;
import com.casxduois.client.modules.performance.LowGPUModule;
import com.casxduois.client.modules.performance.NoRenderModule;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MixinEffectRenderer: Hook ParticleManager để tắt các hiệu ứng hạt (particles) khi bật NoRender hoặc LowGPU
 */
@Mixin(ParticleManager.class)
public class MixinEffectRenderer {

    @Inject(method = {"renderParticles", "func_78874_a"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void onRenderParticles(Entity entityIn, float partialTicks, CallbackInfo ci) {
        if (shouldCancelParticles()) {
            ci.cancel();
        }
    }

    @Inject(method = {"renderLitParticles", "func_78872_b"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void onRenderLitParticles(Entity entityIn, float partialTick, CallbackInfo ci) {
        if (shouldCancelParticles()) {
            ci.cancel();
        }
    }

    private boolean shouldCancelParticles() {
        if (CasxDuoisClient.getInstance() == null || CasxDuoisClient.getInstance().getModuleManager() == null) {
            return false;
        }

        NoRenderModule noRender = CasxDuoisClient.getInstance().getModuleManager().getModule(NoRenderModule.class);
        if (noRender != null && noRender.isToggled() && noRender.noParticles.isEnabled()) {
            return true;
        }

        LowGPUModule lowGPU = CasxDuoisClient.getInstance().getModuleManager().getModule(LowGPUModule.class);
        if (lowGPU != null && lowGPU.isToggled() && lowGPU.disableParticles.isEnabled()) {
            return true;
        }

        return false;
    }
}
