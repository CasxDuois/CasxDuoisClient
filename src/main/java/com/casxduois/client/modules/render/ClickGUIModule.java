package com.casxduois.client.modules.render;

import com.casxduois.client.CasxDuoisClient;
import com.casxduois.client.modules.Category;
import com.casxduois.client.modules.Module;
import org.lwjgl.input.Keyboard;

public class ClickGUIModule extends Module {

    public ClickGUIModule() {
        super("ClickGUI", "Menu Cài Đặt", "Mở ClickGUI", null, Keyboard.KEY_RSHIFT);
    }

    @Override
    public void onEnable() {
        if (mc.currentScreen == null) {
            mc.displayGuiScreen(CasxDuoisClient.getInstance().getClickGui());
        }
        setToggled(false);
    }
}
