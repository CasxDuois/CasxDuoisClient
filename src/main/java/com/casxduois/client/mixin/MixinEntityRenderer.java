package com.casxduois.client.mixin;

import com.casxduois.client.CasxDuoisClient;
import com.casxduois.client.core.MultiTabManager;
import com.casxduois.client.event.events.EventRender2D;
import com.casxduois.client.event.events.EventRender3D;
import com.casxduois.client.modules.performance.NoRenderModule;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {

    @Inject(method = "updateCameraAndRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiIngame;renderGameOverlay(F)V", shift = At.Shift.AFTER))
    private void onRenderGameOverlay(float partialTicks, long nanoTime, CallbackInfo ci) {
        if (CasxDuoisClient.getInstance() != null && CasxDuoisClient.getInstance().getEventBus() != null) {
            CasxDuoisClient.getInstance().getEventBus().post(new EventRender2D(partialTicks));
        }
    }

    /**
     * Hạ GPU tối đa khi tab đang chạy ngầm bằng cách bỏ qua toàn bộ vòng lặp vẽ thế giới 3D
     */
    @Inject(method = "renderWorld", at = @At("HEAD"), cancellable = true)
    private void onRenderWorld(float partialTicks, long finishTimeNano, CallbackInfo ci) {
        if (MultiTabManager.skipWorldRender && !MultiTabManager.forceRenderFrameRequested) {
            ci.cancel();
        }
    }

    /**
     * Hook phát sự kiện EventRender3D để vẽ ESP Box 3D, Tracers sau khi dựng hình thế giới xong
     */
    @Inject(method = "renderWorldPass", at = @At("RETURN"))
    private void onRenderWorldPass(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        if (CasxDuoisClient.getInstance() != null && CasxDuoisClient.getInstance().getEventBus() != null) {
            CasxDuoisClient.getInstance().getEventBus().post(new EventRender3D(partialTicks));
        }
    }

    /**
     * Tắt render hiệu ứng thời tiết mưa và tuyết nếu bật NoRender
     */
    @Inject(method = "renderRainSnow", at = @At("HEAD"), cancellable = true)
    private void onRenderRainSnow(float partialTicks, CallbackInfo ci) {
        if (CasxDuoisClient.getInstance() == null || CasxDuoisClient.getInstance().getModuleManager() == null) return;
        NoRenderModule noRender = CasxDuoisClient.getInstance().getModuleManager().getModule(NoRenderModule.class);
        if (noRender != null && noRender.isToggled() && noRender.noRain.isEnabled()) {
            ci.cancel();
        }
    }

    /**
     * Tắt sương mù nếu bật NoRender
     */
    @Inject(method = "setupFog", at = @At("HEAD"), cancellable = true)
    private void onSetupFog(int startCoords, float partialTicks, CallbackInfo ci) {
        if (CasxDuoisClient.getInstance() == null || CasxDuoisClient.getInstance().getModuleManager() == null) return;
        NoRenderModule noRender = CasxDuoisClient.getInstance().getModuleManager().getModule(NoRenderModule.class);
        if (noRender != null && noRender.isToggled() && noRender.noFog.isEnabled()) {
            org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_FOG);
            ci.cancel();
        }
    }
}
