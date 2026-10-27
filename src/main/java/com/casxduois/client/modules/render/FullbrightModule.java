package com.casxduois.client.modules.render;

import com.casxduois.client.modules.Category;
import com.casxduois.client.modules.Module;
import org.lwjgl.input.Keyboard;

public class FullbrightModule extends Module {

    private float previousGamma = 1.0f;

    public FullbrightModule() {
        super("Fullbright", "Tăng Sáng", "Nhìn rõ trong bóng tối và hang sâu không cần đuốc", Category.RENDER_LIMIT, Keyboard.KEY_B);
        setToggled(true); // Bật sẵn theo nhu cầu người chơi
    }

    @Override
    public void onEnable() {
        if (mc.gameSettings != null) {
            if (mc.gameSettings.gammaSetting < 10.0f) {
                this.previousGamma = mc.gameSettings.gammaSetting;
            }
            mc.gameSettings.gammaSetting = 1000.0f;
        }
    }

    @Override
    public void onDisable() {
        if (mc.gameSettings != null) {
            mc.gameSettings.gammaSetting = Math.min(1.0f, Math.max(0.0f, this.previousGamma));
        }
    }

    @Override
    public void onTick() {
        if (mc.gameSettings != null && mc.gameSettings.gammaSetting < 100.0f) {
            this.previousGamma = Math.min(1.0f, Math.max(0.0f, mc.gameSettings.gammaSetting));
            mc.gameSettings.gammaSetting = 1000.0f;
        }
    }
}
