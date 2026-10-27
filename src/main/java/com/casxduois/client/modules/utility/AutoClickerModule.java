package com.casxduois.client.modules.utility;

import com.casxduois.client.config.BooleanSetting;
import com.casxduois.client.config.ModeSetting;
import com.casxduois.client.config.NumberSetting;
import com.casxduois.client.event.EventBus;
import com.casxduois.client.event.events.EventTick;
import com.casxduois.client.modules.Category;
import com.casxduois.client.modules.Module;
import com.casxduois.client.util.TimerUtils;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

import java.util.Random;

public class AutoClickerModule extends Module {

    public final ModeSetting button = new ModeSetting("Button", "Nút Chuột", "Chọn chuột trái (đánh/đào) hoặc chuột phải (đặt block/câu cá)", "Trái", "Trái", "Phải");
    public final NumberSetting cps = new NumberSetting("CPS", "Tốc Độ Click (CPS)", "Số lần nhấp chuột mỗi giây", 10.0, 1.0, 20.0, 1.0);
    public final BooleanSetting randomJitter = new BooleanSetting("Randomize", "Ngẫu Nhiên Hóa", "Tạo độ trễ tự nhiên chống Anti-Cheat", true);

    private final TimerUtils timer = new TimerUtils();
    private final Random random = new Random();

    public AutoClickerModule() {
        super("AutoClicker", "Tự Động Click", "Tự động nhấp chuột trái hoặc phải liên tục theo tốc độ CPS tùy chỉnh", Category.AUTOMATION, Keyboard.KEY_NONE);
        addSetting(button);
        addSetting(cps);
        addSetting(randomJitter);
    }

    @EventBus.Subscribe
    public void onTick(EventTick event) {
        if (event.getPhase() != EventTick.Phase.START || mc.player == null || mc.currentScreen != null) return;

        double targetCPS = cps.getValue();
        if (randomJitter.isEnabled()) {
            targetCPS += (random.nextDouble() - 0.5) * 2.0;
        }
        long delay = (long) (1000.0 / Math.max(1.0, targetCPS));

        if (timer.hasTimeElapsed(delay, true)) {
            KeyBinding targetKey = button.is("Trái") ? mc.gameSettings.keyBindAttack : mc.gameSettings.keyBindUseItem;
            KeyBinding.onTick(targetKey.getKeyCode());
        }
    }
}
