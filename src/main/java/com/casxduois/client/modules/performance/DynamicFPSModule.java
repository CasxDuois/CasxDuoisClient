package com.casxduois.client.modules.performance;

import com.casxduois.client.config.BooleanSetting;
import com.casxduois.client.config.NumberSetting;
import com.casxduois.client.modules.Category;
import com.casxduois.client.modules.Module;
import org.lwjgl.input.Keyboard;

public class DynamicFPSModule extends Module {

    public final NumberSetting unfocusedFps = new NumberSetting("Unfocused FPS", "FPS khi ẩn tab", "Số khung hình duy trì khi bạn chuyển sang cửa sổ khác", 3.0, 1.0, 30.0, 1.0);
    public final BooleanSetting idleThrottle = new BooleanSetting("Idle Throttle", "Hạ FPS khi không thao tác", "Tự giảm FPS nếu bạn không chạm chuột hoặc bàn phím", true);
    public final NumberSetting idleTimeout = new NumberSetting("Idle Timeout", "Thời gian chờ (giây)", "Số giây chờ trước khi tự động hạ FPS", 20.0, 5.0, 120.0, 5.0);
    public final NumberSetting idleFps = new NumberSetting("Idle FPS", "FPS khi treo máy", "Số khung hình duy trì khi đang treo máy", 15.0, 1.0, 60.0, 1.0);

    public DynamicFPSModule() {
        super("DynamicFPS", "Tự Giảm FPS", "Tự động hạ FPS khi bạn ẩn tab hoặc không chạm máy", Category.THROTTLE, Keyboard.KEY_NONE);
        addSetting(unfocusedFps);
        addSetting(idleThrottle);
        addSetting(idleTimeout);
        addSetting(idleFps);
        setToggled(true); // Bật mặc định
    }
}
