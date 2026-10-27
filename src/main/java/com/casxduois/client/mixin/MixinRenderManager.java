package com.casxduois.client.mixin;

import com.casxduois.client.CasxDuoisClient;
import com.casxduois.client.modules.performance.EntityCullingModule;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * MixinRenderManager: Hook kiểm tra shouldRender của thực thể để thực hiện Frustum & Distance Culling
 */
@Mixin(RenderManager.class)
public class MixinRenderManager {

    @Inject(method = {"shouldRender", "func_178635_a"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void onShouldRender(Entity entity, ICamera camera, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> cir) {
        if (CasxDuoisClient.getInstance() == null || CasxDuoisClient.getInstance().getModuleManager() == null) {
            return;
        }

        EntityCullingModule module = CasxDuoisClient.getInstance().getModuleManager().getModule(EntityCullingModule.class);
        if (module != null && module.isToggled()) {
            module.setCamera(camera);
            if (!module.shouldRender(entity)) {
                cir.setReturnValue(false);
            }
        }
    }
}
