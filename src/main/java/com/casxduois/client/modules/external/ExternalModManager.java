package com.casxduois.client.modules.external;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * ExternalModManager: Quản lý, quét và bật/tắt các file mod (.jar <-> .jar.disabled) trong thư mục mods/
 */
public class ExternalModManager {

    private final File modsDir;
    private final List<ModEntry> mods = new ArrayList<>();

    public static class ModEntry {
        private File file;
        private String modId;
        private String name;
        private String version;
        private String description;
        private boolean enabled;

        public ModEntry(File file, String modId, String name, String version, String description, boolean enabled) {
            this.file = file;
            this.modId = modId;
            this.name = name;
            this.version = version;
            this.description = description;
            this.enabled = enabled;
        }

        public File getFile() {
            return file;
        }

        public String getModId() {
            return modId;
        }

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }

        public String getDescription() {
            return description;
        }

        public boolean isEnabled() {
            return enabled;
        }
    }

    public ExternalModManager() {
        Minecraft mc = Minecraft.getMinecraft();
        this.modsDir = new File(mc.gameDir, "mods");
        if (!this.modsDir.exists()) {
            this.modsDir.mkdirs();
        }
    }

    public synchronized List<ModEntry> scanMods() {
        mods.clear();
        if (!modsDir.exists()) return mods;

        File[] files = modsDir.listFiles();
        if (files == null) return mods;

        for (File f : files) {
            if (f.isDirectory()) continue;

            String fname = f.getName().toLowerCase();
            // Bỏ qua file jar của chính CasxDuois Client
            if (fname.startsWith("casxduoismcclient") || fname.startsWith("casxduoisclient")) {
                continue;
            }

            boolean isEnabled = fname.endsWith(".jar");
            boolean isDisabled = fname.endsWith(".jar.disabled");

            if (isEnabled || isDisabled) {
                ModEntry entry = parseModInfo(f, isEnabled);
                mods.add(entry);
            }
        }

        return mods;
    }

    private ModEntry parseModInfo(File file, boolean enabled) {
        String baseName = file.getName();
        if (baseName.endsWith(".disabled")) {
            baseName = baseName.substring(0, baseName.length() - 9);
        }
        if (baseName.endsWith(".jar")) {
            baseName = baseName.substring(0, baseName.length() - 4);
        }

        String modId = baseName;
        String name = baseName;
        String version = "1.0";
        String description = "Mod ngoài Minecraft 1.12.2";

        try (JarFile jar = new JarFile(file)) {
            JarEntry infoEntry = jar.getJarEntry("mcmod.info");
            if (infoEntry != null) {
                try (InputStream is = jar.getInputStream(infoEntry);
                     InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    JsonElement parsed = new JsonParser().parse(reader);
                    JsonObject obj = null;
                    if (parsed.isJsonArray()) {
                        JsonArray arr = parsed.getAsJsonArray();
                        if (arr.size() > 0 && arr.get(0).isJsonObject()) {
                            obj = arr.get(0).getAsJsonObject();
                        }
                    } else if (parsed.isJsonObject()) {
                        JsonObject rootObj = parsed.getAsJsonObject();
                        if (rootObj.has("modList") && rootObj.get("modList").isJsonArray()) {
                            JsonArray arr = rootObj.getAsJsonArray("modList");
                            if (arr.size() > 0 && arr.get(0).isJsonObject()) {
                                obj = arr.get(0).getAsJsonObject();
                            }
                        } else {
                            obj = rootObj;
                        }
                    }

                    if (obj != null) {
                        if (obj.has("modid")) modId = obj.get("modid").getAsString();
                        if (obj.has("name")) name = obj.get("name").getAsString();
                        if (obj.has("version")) version = obj.get("version").getAsString();
                        if (obj.has("description")) description = obj.get("description").getAsString();
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return new ModEntry(file, modId, name, version, description, enabled);
    }

    public synchronized boolean toggleMod(ModEntry entry) {
        if (entry == null || entry.file == null) return false;

        File src = entry.file;
        File dest;
        if (entry.enabled) {
            dest = new File(src.getParentFile(), src.getName() + ".disabled");
        } else {
            String newName = src.getName();
            if (newName.endsWith(".disabled")) {
                newName = newName.substring(0, newName.length() - 9);
            }
            dest = new File(src.getParentFile(), newName);
        }

        boolean success = src.renameTo(dest);
        if (success) {
            entry.file = dest;
            entry.enabled = !entry.enabled;
        }
        return success;
    }

    public void openModsFolder() {
        try {
            if (modsDir.exists()) {
                Runtime.getRuntime().exec("explorer.exe " + modsDir.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<ModEntry> getMods() {
        return mods;
    }

    public File getModsDir() {
        return modsDir;
    }
}
