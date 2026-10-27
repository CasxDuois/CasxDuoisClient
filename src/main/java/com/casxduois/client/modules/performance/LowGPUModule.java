package com.casxduois.client.modules.performance;

import com.casxduois.client.CasxDuoisClient;
import com.casxduois.client.config.BooleanSetting;
import com.casxduois.client.modules.Category;
import com.casxduois.client.modules.Module;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

public class LowGPUModule extends Module {

    public final BooleanSetting freezeAnimations = new BooleanSetting("Freeze Animations", "Dừng chuyển động nước và lửa", "Tạm dừng hoạt ảnh chất lỏng và ngọn lửa khi treo máy", true);
    public final BooleanSetting disableParticles = new BooleanSetting("Disable Particles", "Tắt hiệu ứng hạt", "Ẩn khói, lửa và các hạt bay trong không khí", true);
    public final BooleanSetting entityCulling = new BooleanSetting("Entity Culling", "Ẩn vật thể sau tường", "Không vẽ các sinh vật và rương bị che khuất sau tường", true);
    public final BooleanSetting disableShadows = new BooleanSetting("Disable Shadows", "Tắt bóng bề mặt", "Giảm xử lý bóng đổ trên bề mặt khối", true);
    public final BooleanSetting trimMemory = new BooleanSetting("Trim Memory", "Tự động dọn RAM", "Giải phóng bộ nhớ RAM định kỳ khi ẩn cửa sổ game", true);

    public LowGPUModule() {
        super("LowGPU", "Tiết Kiệm Phần Cứng", "Giảm tải GPU, CPU và dọn dẹp RAM khi treo máy", Category.THROTTLE, Keyboard.KEY_NONE);
        addSetting(freezeAnimations);
        addSetting(disableParticles);
        addSetting(entityCulling);
        addSetting(disableShadows);
        addSetting(trimMemory);
        setToggled(true);
    }

    private int tickCount = 0;

    @Override
    public void onTick() {
        if (!isToggled() || !trimMemory.isEnabled()) return;
        // Mỗi 10 giây nếu tab chạy ngầm thì tự động dọn dẹp RAM
        if (++tickCount % 200 == 0 && !Display.isActive()) {
            CasxDuoisClient.getInstance().getMemoryOptimizer().cleanMemoryAsync();
        }
    }
}
