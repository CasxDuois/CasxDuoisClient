package com.casxduois.client.modules;

import com.casxduois.client.modules.afk.AntiAFKModule;
import com.casxduois.client.modules.afk.AutoEatModule;
import com.casxduois.client.modules.afk.AutoReconnectModule;
import com.casxduois.client.modules.performance.DynamicFPSModule;
import com.casxduois.client.modules.performance.EntityCullingModule;
import com.casxduois.client.modules.performance.LowGPUModule;
import com.casxduois.client.modules.performance.TileEntityCullingModule;
import com.casxduois.client.modules.render.ClickGUIModule;
import com.casxduois.client.modules.render.HUDModule;
import com.casxduois.client.modules.utility.AutoClickerModule;
import com.casxduois.client.modules.utility.AutoCommandModule;
import com.casxduois.client.modules.utility.AutoSprintModule;
import com.casxduois.client.modules.utility.DiscordRemoteModule;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ModuleManager: Quản lý các nhóm tính năng trong client:
 * 1. Webhook Discord (DiscordRemoteModule)
 * 2. Giao diện HUD (HUDModule)
 * 3. Giới Hạn Render (EntityCullingModule, TileEntityCullingModule, FullbrightModule)
 * 4. Hạ FPS, GPU, CPU, RAM Khi Treo Máy (DynamicFPSModule, LowGPUModule)
 * 5. Tự Động Hóa & Treo Máy (AutoCommandModule, AntiAFKModule, AutoEatModule, AutoSprintModule, AutoClickerModule)
 */
public class ModuleManager {

    private final List<Module> modules = new ArrayList<>();

    public ModuleManager() {
        // 1. Nhóm Webhook Discord
        registerModule(new DiscordRemoteModule());

        // 2. Nhóm Giao Diện HUD
        registerModule(new HUDModule());

        // 3. Nhóm Giới Hạn Render & Tăng Sáng Fullbright
        registerModule(new com.casxduois.client.modules.render.FullbrightModule());
        registerModule(new EntityCullingModule());
        registerModule(new TileEntityCullingModule());

        // 4. Nhóm Hạ FPS, GPU, CPU, RAM Khi Treo
        registerModule(new DynamicFPSModule());
        registerModule(new LowGPUModule());

        // 5. Nhóm Tự Động Hóa & Hỗ Trợ Treo Máy
        registerModule(new AutoCommandModule());
        registerModule(new AntiAFKModule());
        registerModule(new AutoEatModule());
        registerModule(new AutoSprintModule());
        registerModule(new AutoClickerModule());

        // Module hệ thống nội bộ (không hiển thị trong danh mục R-Shift)
        registerModule(new ClickGUIModule());
        registerModule(new AutoReconnectModule());
    }

    public void registerModule(Module module) {
        this.modules.add(module);
    }

    public List<Module> getModules() {
        return modules;
    }

    public List<Module> getModulesByCategory(Category category) {
        if (category == null) return new ArrayList<>();
        return modules.stream().filter(m -> m.getCategory() == category).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T getModule(Class<T> clazz) {
        for (Module m : modules) {
            if (m.getClass().equals(clazz)) {
                return (T) m;
            }
        }
        return null;
    }

    public void onKey(int key) {
        if (key == 0) return;
        for (Module m : modules) {
            if (m.getKey() == key) {
                m.toggle();
            }
        }
    }
}
