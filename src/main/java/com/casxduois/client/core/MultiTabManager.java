package com.casxduois.client.core;

import com.casxduois.client.CasxDuoisClient;
import com.casxduois.client.modules.performance.DynamicFPSModule;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.Display;

/**
 * Quản lý tài nguyên đa tab: Điều tiết FPS và nhường CPU/GPU cho tab đang thao tác
 */
public class MultiTabManager {

    private final Minecraft mc = Minecraft.getMinecraft();
    private long lastUserInputTime = System.currentTimeMillis();
    private boolean isWindowActive = true;

    public void updateUserInput() {
        this.lastUserInputTime = System.currentTimeMillis();
    }

    public boolean isWindowActive() {
        return this.isWindowActive;
    }

    public static volatile boolean skipWorldRender = false;
    public static volatile boolean forceRenderFrameRequested = false;

    public boolean isUserIdle(long timeoutMs) {
        return (System.currentTimeMillis() - this.lastUserInputTime) > timeoutMs;
    }

    /**
     * Tính toán FPS mục tiêu cho tab hiện tại dựa trên trạng thái tương tác
     */
    public int getTargetFPS() {
        this.isWindowActive = Display.isActive();
        skipWorldRender = !this.isWindowActive;

        DynamicFPSModule dynamicFPS = CasxDuoisClient.getInstance().getModuleManager().getModule(DynamicFPSModule.class);
        if (dynamicFPS == null || !dynamicFPS.isToggled()) {
            return mc.gameSettings.limitFramerate;
        }

        // Nếu cửa sổ đang bị ẩn hoặc chuyển sang tab khác (Unfocused)
        if (!isWindowActive) {
            return (int) dynamicFPS.unfocusedFps.getValue().doubleValue();
        }

        // Nếu cửa sổ đang mở nhưng người chơi không thao tác chuột/phím (Idle)
        if (dynamicFPS.idleThrottle.isEnabled() && isUserIdle((long) (dynamicFPS.idleTimeout.getValue() * 1000))) {
            return (int) dynamicFPS.idleFps.getValue().doubleValue();
        }

        // Khi đang thao tác trực tiếp trên tab này
        return mc.gameSettings.limitFramerate > 0 ? mc.gameSettings.limitFramerate : 120;
    }

    /**
     * Ép vẽ 1 frame hoàn chỉnh (dùng khi Discord yêu cầu chụp ảnh !tra lúc tab đang chạy ngầm)
     */
    public static void forceRenderOneFrame() {
        forceRenderFrameRequested = true;
        skipWorldRender = false;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.entityRenderer != null && mc.world != null && mc.player != null) {
            try {
                mc.entityRenderer.updateCameraAndRender(0.0F, System.nanoTime());
            } catch (Throwable ignored) {
            }
        }
        forceRenderFrameRequested = false;
        if (!Display.isActive()) {
            skipWorldRender = true;
        }
    }

    /**
     * Giảm tải CPU bằng cách chèn sleep vi mô khi tab đang chạy ngầm
     */
    public void throttleBackgroundLoop() {
        DynamicFPSModule dynamicFPS = CasxDuoisClient.getInstance().getModuleManager().getModule(DynamicFPSModule.class);
        if (dynamicFPS != null && dynamicFPS.isToggled() && !Display.isActive()) {
            try {
                // Nhường xung nhịp CPU cho các tab khác
                Thread.sleep(15);
            } catch (InterruptedException ignored) {
            }
        }
    }
}
