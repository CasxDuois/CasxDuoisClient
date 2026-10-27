package com.casxduois.client.modules.utility;

import com.casxduois.client.config.BooleanSetting;
import com.casxduois.client.event.EventBus;
import com.casxduois.client.event.events.EventTick;
import com.casxduois.client.modules.Category;
import com.casxduois.client.modules.Module;
import net.minecraft.init.MobEffects;
import org.lwjgl.input.Keyboard;

/**
 * AutoSprintModule: Tự động chạy nhanh (Sprint) khi di chuyển về phía trước hoặc mọi hướng
 * Tiết kiệm thao tác nhấp đúp phím W, di chuyển mượt mà và linh hoạt khi PvP/treo máy.
 */
public class AutoSprintModule extends Module {

    public final BooleanSetting omniSprint = new BooleanSetting("Omni Sprint", "Chạy Mọi Hướng", "Tự động chạy nhanh cả khi đi lùi hoặc sang ngang", false);

    public AutoSprintModule() {
        super("AutoSprint", "Tự Động Chạy Nhanh", "Tự động sprint khi di chuyển mà không cần nhấp đúp W", Category.AUTOMATION, Keyboard.KEY_NONE);
        addSetting(omniSprint);
        setToggled(true);
    }

    @EventBus.Subscribe
    public void onTick(EventTick event) {
        if (event.getPhase() != EventTick.Phase.START || mc.player == null || mc.player.isDead || mc.player.movementInput == null || mc.currentScreen != null) return;

        // Các điều kiện Minecraft không cho phép sprint
        boolean canSprint = !mc.player.isSneaking()
                && !mc.player.isRiding()
                && !mc.player.isOnLadder()
                && mc.player.getFoodStats().getFoodLevel() > 6
                && !mc.player.isPotionActive(MobEffects.BLINDNESS);

        if (!canSprint) return;

        boolean moving = omniSprint.isEnabled()
                ? (mc.player.movementInput.moveForward != 0.0F || mc.player.movementInput.moveStrafe != 0.0F)
                : (mc.player.movementInput.moveForward > 0.0F && !mc.player.collidedHorizontally);

        if (moving) {
            mc.player.setSprinting(true);
        }
    }

    @Override
    public void onDisable() {
        if (mc.player != null) {
            mc.player.setSprinting(false);
        }
    }
}
