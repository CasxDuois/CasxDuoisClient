package com.casxduois.client.modules.render;

import com.casxduois.client.config.ModeSetting;
import com.casxduois.client.modules.Category;
import com.casxduois.client.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;

/**
 * MotionBlurModule: Hiệu ứng làm mờ chuyển động mượt mà phong cách điện ảnh (144Hz - 240Hz)
 * Tận dụng pipeline shader hậu kỳ phần cứng để tạo độ mượt đỉnh cao khi lia chuột và combat.
 */
public class MotionBlurModule extends Module {

    public final ModeSetting mode = new ModeSetting("Mode", "Chế Độ", "Kiểu làm mờ chuyển động", "Motion Blur", "Motion Blur", "Soft Blur");

    private static final ResourceLocation SHADER_MOTION_BLUR = new ResourceLocation("shaders/post/phosphor.json");
    private static final ResourceLocation SHADER_SOFT_BLUR = new ResourceLocation("shaders/post/blur.json");

    public MotionBlurModule() {
        super("MotionBlur", "Làm Mờ", "Làm mờ chuyển động", null, Keyboard.KEY_NONE);
        addSetting(mode);
    }

    @Override
    public void onEnable() {
        applyShader();
    }

    @Override
    public void onDisable() {
        removeShader();
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world != null && mc.entityRenderer != null && OpenGlHelper.shadersSupported) {
            if (!mc.entityRenderer.isShaderActive()) {
                applyShader();
            }
        }
    }

    private void applyShader() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.entityRenderer == null || !OpenGlHelper.shadersSupported) return;
        try {
            ResourceLocation target = mode.is("Soft Blur") ? SHADER_SOFT_BLUR : SHADER_MOTION_BLUR;
            mc.entityRenderer.loadShader(target);
        } catch (Throwable ignored) {
        }
    }

    private void removeShader() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.entityRenderer != null) {
            try {
                mc.entityRenderer.stopUseShader();
            } catch (Throwable ignored) {
            }
        }
    }
}
