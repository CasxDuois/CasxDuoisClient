package com.casxduois.client.modules;

import com.casxduois.client.CasxDuoisClient;
import com.casxduois.client.config.Setting;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {

    protected static final Minecraft mc = Minecraft.getMinecraft();

    private final String name;
    private final String vietnameseName;
    private final String description;
    private final Category category;
    private int key;
    private boolean toggled;
    private final List<Setting<?>> settings = new ArrayList<>();

    public Module(String name, String vietnameseName, String description, Category category, int key) {
        this.name = name;
        this.vietnameseName = vietnameseName;
        this.description = description;
        this.category = category;
        this.key = key;
        this.toggled = false;
    }

    public void addSetting(Setting<?> setting) {
        this.settings.add(setting);
    }

    public List<Setting<?>> getSettings() {
        return settings;
    }

    public String getName() {
        return name;
    }

    public String getVietnameseName() {
        return vietnameseName;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public boolean isToggled() {
        return toggled;
    }

    public void setToggled(boolean toggled) {
        this.toggled = toggled;
        if (toggled) {
            onEnable();
            CasxDuoisClient.getInstance().getEventBus().register(this);
        } else {
            CasxDuoisClient.getInstance().getEventBus().unregister(this);
            onDisable();
        }
    }

    public void toggle() {
        setToggled(!this.toggled);
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    public void onTick() {
    }

    public void onRender2D(float partialTicks) {
    }
}
