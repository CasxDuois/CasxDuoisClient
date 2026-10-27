package com.casxduois.client.modules.performance;

import com.casxduois.client.config.BooleanSetting;
import com.casxduois.client.modules.Category;
import com.casxduois.client.modules.Module;
import org.lwjgl.input.Keyboard;

/**
 * NoRenderModule: Tắt các hiệu ứng đồ họa không cần thiết giúp giải phóng GPU và CPU
 */
public class NoRenderModule extends Module {

    public final BooleanSetting noRain = new BooleanSetting("No Rain", "Tắt Mưa/Tuyết", "Không hiển thị hiệu ứng thời tiết mưa và tuyết", true);
    public final BooleanSetting noFog = new BooleanSetting("No Fog", "Tắt Sương Mù", "Vô hiệu hóa sương mù che khuất tầm nhìn", true);
    public final BooleanSetting noParticles = new BooleanSetting("No Particles", "Tắt Hạt/Khói", "Tắt toàn bộ particle hạt nổ, khói, tim, crit", true);
    public final BooleanSetting noItems3D = new BooleanSetting("No 3D Items", "Tắt 3D Item Rơi", "Đơn giản hóa hoặc ẩn vật phẩm rơi trên đất", false);

    public NoRenderModule() {
        super("NoRender", "Tắt Render", "Tắt render", null, Keyboard.KEY_NONE);
        addSetting(noRain);
        addSetting(noFog);
        addSetting(noParticles);
        addSetting(noItems3D);
        setToggled(true);
    }
}
