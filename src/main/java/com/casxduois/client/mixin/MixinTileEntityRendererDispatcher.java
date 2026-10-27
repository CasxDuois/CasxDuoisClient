package com.casxduois.client.mixin;

import com.casxduois.client.CasxDuoisClient;
import com.casxduois.client.modules.performance.TileEntityCullingModule;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MixinTileEntityRendererDispatcher: Culling TileEntity theo khoảng cách
 */
@Mixin(TileEntityRendererDispatcher.class)
public class MixinTileEntityRendererDispatcher {

    @Inject(method = {"render(Lnet/minecraft/tileentity/TileEntity;FI)V", "func_180546_a"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void onRenderTileEntity(TileEntity tileEntity, float partialTicks, int destroyStage, CallbackInfo ci) {
        if (CasxDuoisClient.getInstance() == null || CasxDuoisClient.getInstance().getModuleManager() == null) {
            return;
        }

        TileEntityCullingModule module = CasxDuoisClient.getInstance().getModuleManager().getModule(TileEntityCullingModule.class);
        if (module != null && module.isToggled() && !module.shouldRender(tileEntity)) {
            ci.cancel();
        }
    }
}
