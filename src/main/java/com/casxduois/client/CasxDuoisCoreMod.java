package com.casxduois.client;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * CasxDuoisCoreMod: Cầu nối IFMLLoadingPlugin cho Forge 1.12.2 và MixinBooter
 * Chịu trách nhiệm đăng ký cấu hình Mixin (mixins.casxduois.json) khi Forge khởi động.
 */
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.Name("CasxDuoisCoreMod")
@IFMLLoadingPlugin.SortingIndex(1001)
public class CasxDuoisCoreMod implements IFMLLoadingPlugin {

    public CasxDuoisCoreMod() {
        // Khởi tạo và đăng ký Mixin config
        try {
            // Thử đăng ký qua MixinBooter API nếu có
            Class<?> mixinBooterClass = Class.forName("zone.rong.mixinbooter.MixinBooter");
            Method registerMethod = mixinBooterClass.getMethod("registerMixinConfiguration", String.class);
            registerMethod.invoke(null, "mixins.casxduois.json");
        } catch (Throwable t) {
            // Fallback sang SpongePowered Mixin trực tiếp
            try {
                MixinBootstrap.init();
                Mixins.addConfiguration("mixins.casxduois.json");
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
