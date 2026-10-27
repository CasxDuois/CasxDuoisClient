package com.casxduois.client.modules.afk;

import com.casxduois.client.config.BooleanSetting;
import com.casxduois.client.config.NumberSetting;
import com.casxduois.client.event.EventBus;
import com.casxduois.client.event.events.EventTick;
import com.casxduois.client.modules.Category;
import com.casxduois.client.modules.Module;
import com.casxduois.client.util.TimerUtils;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

import java.util.Random;

public class AntiAFKModule extends Module {

    public final BooleanSetting rotateHead = new BooleanSetting("Rotate Head", "Xoay Nhẹ Góc Nhìn", "Tự động đảo camera nhẹ để máy chủ ghi nhận hoạt động", true);
    public final BooleanSetting randomSneak = new BooleanSetting("Random Sneak", "Ngồi Xuống (Sneak)", "Thỉnh thoảng sneak để tránh bị phát hiện", true);
    public final BooleanSetting randomJump = new BooleanSetting("Random Jump", "Nhảy Nhẹ", "Nhảy tại chỗ theo chu kỳ", false);
    public final NumberSetting interval = new NumberSetting("Interval", "Chu Kỳ (Giây)", "Khoảng thời gian giữa các lần thực hiện hành động", 5.0, 1.0, 30.0, 1.0);

    private final TimerUtils timer = new TimerUtils();
    private final Random random = new Random();

    public AntiAFKModule() {
        super("AntiAFK", "Chống AFK Kick", "Tự động vận động chống bị đá khỏi server khi treo máy", Category.AUTOMATION, Keyboard.KEY_NONE);
        addSetting(rotateHead);
        addSetting(randomSneak);
        addSetting(randomJump);
        addSetting(interval);
    }

    @EventBus.Subscribe
    public void onTick(EventTick event) {
        if (event.getPhase() != EventTick.Phase.START || mc.player == null) return;

        if (timer.hasTimeElapsed((long) (interval.getValue() * 1000), true)) {
            if (rotateHead.isEnabled()) {
                float yawJitter = (random.nextFloat() - 0.5f) * 6.0f;
                float pitchJitter = (random.nextFloat() - 0.5f) * 4.0f;
                mc.player.rotationYaw += yawJitter;
                mc.player.rotationPitch = Math.max(-90.0f, Math.min(90.0f, mc.player.rotationPitch + pitchJitter));
            }

            if (randomSneak.isEnabled() && random.nextBoolean()) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), true);
                new Thread(() -> {
                    try {
                        Thread.sleep(200);
                        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
                    } catch (InterruptedException ignored) {
                    }
                }).start();
            }

            if (randomJump.isEnabled() && mc.player.onGround && random.nextBoolean()) {
                mc.player.jump();
            }
        }
    }
}
