package com.casxduois.client.mixin;

import com.casxduois.client.CasxDuoisClient;
import com.casxduois.client.event.events.EventKey;
import com.casxduois.client.event.events.EventTick;
import com.casxduois.client.gui.menu.CasxDuoisMainMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {

    @Inject(method = {"init", "func_71384_a"}, at = @At("RETURN"), remap = false)
    private void onInit(CallbackInfo ci) {
        CasxDuoisClient.getInstance().init();
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.getSession() != null && mc.getSession().getUsername() != null) {
                org.lwjgl.opengl.Display.setTitle(mc.getSession().getUsername());
            }
        } catch (Throwable ignored) {
        }
    }

    @Inject(method = {"shutdown", "func_71400_g"}, at = @At("HEAD"), remap = false)
    private void onShutdown(CallbackInfo ci) {
        CasxDuoisClient.getInstance().shutdown();
    }

    @Inject(method = {"runGameLoop", "func_71411_J"}, at = @At("HEAD"), remap = false)
    private void onRunGameLoopHead(CallbackInfo ci) {
        // Điều tiết sleep CPU khi tab chạy ngầm
        CasxDuoisClient.getInstance().getMultiTabManager().throttleBackgroundLoop();
    }

    @Inject(method = {"getLimitFramerate", "func_90020_K"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void onGetLimitFramerate(CallbackInfoReturnable<Integer> cir) {
        // Áp dụng Dynamic FPS thông minh
        cir.setReturnValue(CasxDuoisClient.getInstance().getMultiTabManager().getTargetFPS());
    }

    @Inject(method = {"runTick", "func_71407_l"}, at = @At("HEAD"), remap = false)
    private void onRunTickStart(CallbackInfo ci) {
        CasxDuoisClient.getInstance().getEventBus().post(new EventTick(EventTick.Phase.START));
    }

    @Inject(method = {"runTick", "func_71407_l"}, at = @At("RETURN"), remap = false)
    private void onRunTickEnd(CallbackInfo ci) {
        CasxDuoisClient.getInstance().getEventBus().post(new EventTick(EventTick.Phase.END));
    }

    @Inject(method = {"runTickKeyboard", "func_184118_az"}, at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Keyboard;getEventKey()I", shift = At.Shift.AFTER, ordinal = 0), remap = false)
    private void onRunTickKeyboard(CallbackInfo ci) {
        int key = Keyboard.getEventKey() == 0 ? Keyboard.getEventCharacter() + 256 : Keyboard.getEventKey();
        if (Keyboard.getEventKeyState()) {
            CasxDuoisClient.getInstance().getEventBus().post(new EventKey(key));
        }
    }

    @org.spongepowered.asm.mixin.injection.ModifyVariable(method = {"displayGuiScreen", "func_147108_a"}, at = @At("HEAD"), argsOnly = true, remap = false)
    private GuiScreen modifyGuiScreen(GuiScreen guiScreenIn) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.getSession() != null && mc.getSession().getUsername() != null) {
                org.lwjgl.opengl.Display.setTitle(mc.getSession().getUsername());
            }
        } catch (Throwable ignored) {
        }
        if (guiScreenIn instanceof GuiMainMenu) {
            return new CasxDuoisMainMenu();
        }
        return guiScreenIn;
    }

    @Inject(method = {"loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;)V", "func_71403_a"}, at = @At("RETURN"), remap = false)
    private void onLoadWorld(net.minecraft.client.multiplayer.WorldClient worldClientIn, CallbackInfo ci) {
        if (worldClientIn == null) {
            casxduoisPerformMemoryTrimming();
        }
    }

    private static void casxduoisPerformMemoryTrimming() {
        Thread cleanupThread = new Thread(() -> {
            try {
                Thread.sleep(150);
                long beforeMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
                System.gc();
                long afterMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
                CasxDuoisClient.LOGGER.info("[MemoryOptimizer] Đã dọn dẹp RAM sau khi thoát máy chủ: Giảm từ " + beforeMemory + "MB xuống " + afterMemory + "MB.");
            } catch (Exception ignored) {
            }
        }, "CasxDuois-MemoryTrimmer");
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }
}
