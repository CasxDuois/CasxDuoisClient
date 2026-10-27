package com.casxduois.client.config;

import com.casxduois.client.CasxDuoisClient;
import com.casxduois.client.modules.Module;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class ConfigManager {

    private final File configFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public ConfigManager(File clientDir) {
        this.configFile = new File(clientDir, "casxduois_config.json");
    }

    public void saveConfig() {
        try {
            if (!configFile.getParentFile().exists()) {
                configFile.getParentFile().mkdirs();
            }

            JsonObject root = new JsonObject();
            JsonObject modulesObj = new JsonObject();

            for (Module module : CasxDuoisClient.getInstance().getModuleManager().getModules()) {
                JsonObject moduleData = new JsonObject();
                moduleData.addProperty("toggled", module.isToggled());
                moduleData.addProperty("key", module.getKey());

                JsonObject settingsData = new JsonObject();
                for (Setting<?> setting : module.getSettings()) {
                    if (setting instanceof BooleanSetting) {
                        settingsData.addProperty(setting.getName(), ((BooleanSetting) setting).getValue());
                    } else if (setting instanceof NumberSetting) {
                        settingsData.addProperty(setting.getName(), ((NumberSetting) setting).getValue());
                    } else if (setting instanceof ModeSetting) {
                        settingsData.addProperty(setting.getName(), ((ModeSetting) setting).getValue());
                    } else if (setting instanceof StringSetting) {
                        if ("Server Pass".equalsIgnoreCase(setting.getName())) {
                            continue; // Mật khẩu server được quản lý độc lập theo từng server trong server_auth.json
                        }
                        settingsData.addProperty(setting.getName(), ((StringSetting) setting).getValue());
                    }
                }
                moduleData.add("settings", settingsData);
                modulesObj.add(module.getName(), moduleData);
            }

            root.add("modules", modulesObj);

            try (FileWriter writer = new FileWriter(configFile)) {
                gson.toJson(root, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadConfig() {
        if (!configFile.exists()) return;

        try (FileReader reader = new FileReader(configFile)) {
            JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
            if (root.has("modules")) {
                JsonObject modulesObj = root.getAsJsonObject("modules");
                for (Module module : CasxDuoisClient.getInstance().getModuleManager().getModules()) {
                    if (modulesObj.has(module.getName())) {
                        JsonObject moduleData = modulesObj.getAsJsonObject(module.getName());
                        if (moduleData.has("toggled")) {
                            module.setToggled(moduleData.get("toggled").getAsBoolean());
                        }
                        if (moduleData.has("key")) {
                            module.setKey(moduleData.get("key").getAsInt());
                        }
                        if (moduleData.has("settings")) {
                            JsonObject settingsData = moduleData.getAsJsonObject("settings");
                            for (Setting<?> setting : module.getSettings()) {
                                if (settingsData.has(setting.getName())) {
                                    if (setting instanceof BooleanSetting) {
                                        ((BooleanSetting) setting).setValue(settingsData.get(setting.getName()).getAsBoolean());
                                    } else if (setting instanceof NumberSetting) {
                                        ((NumberSetting) setting).setValue(settingsData.get(setting.getName()).getAsDouble());
                                    } else if (setting instanceof ModeSetting) {
                                        ((ModeSetting) setting).setValue(settingsData.get(setting.getName()).getAsString());
                                    } else if (setting instanceof StringSetting) {
                                        if ("Server Pass".equalsIgnoreCase(setting.getName())) {
                                            continue; // Mật khẩu server nạp theo từng server khi join/mở tab
                                        }
                                        ((StringSetting) setting).setValue(settingsData.get(setting.getName()).getAsString());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
