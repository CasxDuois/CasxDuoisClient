package com.casxduois.client.modules.performance;

import com.casxduois.client.config.BooleanSetting;
import com.casxduois.client.modules.Category;
import com.casxduois.client.modules.Module;
import org.lwjgl.input.Keyboard;

public class FastRenderModule extends Module {

    public final BooleanSetting fastFont = new BooleanSetting("Fast Font", "Tối Ưu Font Chữ", "Tăng tốc độ vẽ font chữ của giao diện", true);
    public final BooleanSetting fastGui = new BooleanSetting("Fast GUI", "Tối Ưu GUI", "Tối ưu hóa các bảng hiển thị trong game", true);

    public FastRenderModule() {
        super("FastRender", "Tối Ưu Render", "Tối ưu render", null, Keyboard.KEY_NONE);
        addSetting(fastFont);
        addSetting(fastGui);
        setToggled(true);
    }
}
