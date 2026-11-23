package com.swiftclient.mod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SwiftConfig {

    public static final List<String> MOD_ORDER = Arrays.asList(
            "Keystrokes",
            "PotionEffect",
            "Armor Status",
            "ToggleSprint",
            "Title",
            "FPS",
            "CPS",
            "Ping",
            "TNT Timer"
    );

    public static class HudElementConfig {
        public boolean enabled;
        public boolean colored;
        public boolean background;
        public double x;
        public double y;
        public float scale;

        public HudElementConfig(
                boolean enabled,
                boolean colored,
                boolean background,
                double x,
                double y,
                float scale
        ) {
            this.enabled = enabled;
            this.colored = colored;
            this.background = background;
            this.x = x;
            this.y = y;
            this.scale = scale;
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final File configFile;
    private final Map<String, HudElementConfig> modules = new LinkedHashMap<>();

    public SwiftConfig(File gameDir) {
        File swiftDir = new File(gameDir, ".swiftclient");
        if (!swiftDir.exists()) {
            swiftDir.mkdirs();
        }
        this.configFile = new File(swiftDir, "swiftclient_hud_mod.json");
        resetDefaults();
        load();
    }

    public void resetDefaults() {
        modules.clear();
        modules.put("Keystrokes", new HudElementConfig(true, true, true, 0.88, 0.80, 1.0F));
        modules.put("PotionEffect", new HudElementConfig(true, true, true, 0.12, 0.55, 1.0F));
        modules.put("Armor Status", new HudElementConfig(true, true, true, 0.88, 0.52, 1.0F));
        modules.put("ToggleSprint", new HudElementConfig(true, true, true, 0.12, 0.36, 1.0F));
        modules.put("Title", new HudElementConfig(true, true, false, 0.50, 0.22, 1.0F));
        modules.put("FPS", new HudElementConfig(true, true, true, 0.12, 0.12, 1.0F));
        modules.put("CPS", new HudElementConfig(true, true, true, 0.12, 0.20, 1.0F));
        modules.put("Ping", new HudElementConfig(true, true, true, 0.12, 0.28, 1.0F));
        modules.put("TNT Timer", new HudElementConfig(true, true, true, 0.50, 0.32, 1.0F));
    }

    public Map<String, HudElementConfig> getAll() {
        return modules;
    }

    public HudElementConfig get(String name) {
        HudElementConfig cfg = modules.get(name);
        if (cfg == null) {
            cfg = new HudElementConfig(true, true, true, 0.50, 0.50, 1.0F);
            modules.put(name, cfg);
        }
        return cfg;
    }

    public void load() {
        if (!configFile.exists()) {
            save();
            return;
        }
        try (FileReader reader = new FileReader(configFile)) {
            JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
            for (String name : MOD_ORDER) {
                HudElementConfig def = get(name);
                if (root.has(name) && root.get(name).isJsonObject()) {
                    JsonObject item = root.getAsJsonObject(name);
                    boolean enabled = item.has("enabled") ? item.get("enabled").getAsBoolean() : def.enabled;
                    boolean colored = item.has("colored") ? item.get("colored").getAsBoolean() : def.colored;
                    boolean background = item.has("background") ? item.get("background").getAsBoolean() : def.background;
                    double x = item.has("x") ? clamp(item.get("x").getAsDouble(), 0.05, 0.95) : def.x;
                    double y = item.has("y") ? clamp(item.get("y").getAsDouble(), 0.05, 0.95) : def.y;
                    float scale = item.has("scale") ? (float) clamp(item.get("scale").getAsDouble(), 0.5, 2.0) : def.scale;
                    modules.put(name, new HudElementConfig(enabled, colored, background, x, y, scale));
                }
            }
        } catch (Exception ignored) {
        }
    }

    public void save() {
        JsonObject root = new JsonObject();
        for (Map.Entry<String, HudElementConfig> entry : modules.entrySet()) {
            HudElementConfig c = entry.getValue();
            JsonObject obj = new JsonObject();
            obj.addProperty("enabled", c.enabled);
            obj.addProperty("colored", c.colored);
            obj.addProperty("background", c.background);
            obj.addProperty("x", Math.round(c.x * 1000.0) / 1000.0);
            obj.addProperty("y", Math.round(c.y * 1000.0) / 1000.0);
            obj.addProperty("scale", Math.round(c.scale * 100.0) / 100.0F);
            root.add(entry.getKey(), obj);
        }
        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(root, writer);
        } catch (Exception ignored) {
        }
    }

    public static double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }
}
