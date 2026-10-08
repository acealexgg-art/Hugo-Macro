package de.hugosmp.automation.config;

import de.hugosmp.automation.items.ItemAction;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Alle Einstellungen, gespeichert in config/hugoautomation.properties.
 * Die Server-Details (Titel, Slots, Befehl) sind ANNAHMEN aus den Screenshots und koennen
 * in der Datei angepasst werden, ohne Code zu aendern.
 */
public final class MacroConfig {

    // --- Allgemein (im Settings-Menue) ---
    public static int clickDelayMs = 50;
    public static int responseTimeoutMs = 2500;
    public static int retryCount = 3;
    public static boolean pauseOnUnknownGui = true;
    public static boolean protectSpecialItems = true;
    public static boolean skipHotbar = true;
    public static boolean orderFallbackSell = false;
    public static boolean debug = true;

    // --- Items ---
    public static ItemAction defaultAction = ItemAction.IGNORE;
    public static final Map<String, ItemAction> rules = new LinkedHashMap<>();

    // --- Spawner ---
    public static boolean spawnerUseTrigger = true;
    public static int triggerPercent = 100;
    public static boolean useDropButton = true;

    // --- Server-Details (ANNAHMEN, in der Datei aenderbar) ---
    public static String sellCommand = "sell";
    public static String sellTitle = "Items verkaufen";
    public static int sellConfirmSlot = 35;
    public static String spawnerTitle = "SPAWNER";
    public static int spawnerStorageSlots = 36;
    public static int spawnerPrevSlot = 39;
    public static int spawnerInfoSlot = 40;
    public static int spawnerNextSlot = 41;
    public static int spawnerDropPageSlot = 44;
    public static String percentRegex = "(\\d+(?:[.,]\\d+)?)\\s*%";
    public static String orderTitle = "";

    private MacroConfig() {
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("hugoautomation.properties");
    }

    public static void load() {
        Path f = file();
        if (!Files.exists(f)) return;
        try (InputStream in = Files.newInputStream(f)) {
            Properties p = new Properties();
            p.load(in);
            clickDelayMs = num(p, "clickDelayMs", clickDelayMs, 20, 1000);
            responseTimeoutMs = num(p, "responseTimeoutMs", responseTimeoutMs, 300, 10000);
            retryCount = num(p, "retryCount", retryCount, 0, 20);
            pauseOnUnknownGui = bool(p, "pauseOnUnknownGui", pauseOnUnknownGui);
            protectSpecialItems = bool(p, "protectSpecialItems", protectSpecialItems);
            skipHotbar = bool(p, "skipHotbar", skipHotbar);
            orderFallbackSell = bool(p, "orderFallbackSell", orderFallbackSell);
            debug = bool(p, "debug", debug);
            defaultAction = ItemAction.parse(p.getProperty("defaultAction", defaultAction.name()), defaultAction);
            spawnerUseTrigger = bool(p, "spawnerUseTrigger", spawnerUseTrigger);
            triggerPercent = num(p, "triggerPercent", triggerPercent, 1, 100);
            useDropButton = bool(p, "useDropButton", useDropButton);
            sellCommand = p.getProperty("sellCommand", sellCommand);
            sellTitle = p.getProperty("sellTitle", sellTitle);
            sellConfirmSlot = num(p, "sellConfirmSlot", sellConfirmSlot, 0, 200);
            spawnerTitle = p.getProperty("spawnerTitle", spawnerTitle);
            spawnerStorageSlots = num(p, "spawnerStorageSlots", spawnerStorageSlots, 1, 200);
            spawnerPrevSlot = num(p, "spawnerPrevSlot", spawnerPrevSlot, 0, 200);
            spawnerInfoSlot = num(p, "spawnerInfoSlot", spawnerInfoSlot, 0, 200);
            spawnerNextSlot = num(p, "spawnerNextSlot", spawnerNextSlot, 0, 200);
            spawnerDropPageSlot = num(p, "spawnerDropPageSlot", spawnerDropPageSlot, 0, 200);
            percentRegex = p.getProperty("percentRegex", percentRegex);
            orderTitle = p.getProperty("orderTitle", orderTitle);

            rules.clear();
            String raw = p.getProperty("rules", "");
            for (String part : raw.split(",")) {
                String[] kv = part.split("=");
                if (kv.length == 2 && !kv[0].isBlank()) {
                    rules.put(kv[0].trim(), ItemAction.parse(kv[1], ItemAction.IGNORE));
                }
            }
        } catch (IOException ignored) {
        }
    }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("clickDelayMs", Integer.toString(clickDelayMs));
        p.setProperty("responseTimeoutMs", Integer.toString(responseTimeoutMs));
        p.setProperty("retryCount", Integer.toString(retryCount));
        p.setProperty("pauseOnUnknownGui", Boolean.toString(pauseOnUnknownGui));
        p.setProperty("protectSpecialItems", Boolean.toString(protectSpecialItems));
        p.setProperty("skipHotbar", Boolean.toString(skipHotbar));
        p.setProperty("orderFallbackSell", Boolean.toString(orderFallbackSell));
        p.setProperty("debug", Boolean.toString(debug));
        p.setProperty("defaultAction", defaultAction.name());
        p.setProperty("spawnerUseTrigger", Boolean.toString(spawnerUseTrigger));
        p.setProperty("triggerPercent", Integer.toString(triggerPercent));
        p.setProperty("useDropButton", Boolean.toString(useDropButton));
        p.setProperty("sellCommand", sellCommand);
        p.setProperty("sellTitle", sellTitle);
        p.setProperty("sellConfirmSlot", Integer.toString(sellConfirmSlot));
        p.setProperty("spawnerTitle", spawnerTitle);
        p.setProperty("spawnerStorageSlots", Integer.toString(spawnerStorageSlots));
        p.setProperty("spawnerPrevSlot", Integer.toString(spawnerPrevSlot));
        p.setProperty("spawnerInfoSlot", Integer.toString(spawnerInfoSlot));
        p.setProperty("spawnerNextSlot", Integer.toString(spawnerNextSlot));
        p.setProperty("spawnerDropPageSlot", Integer.toString(spawnerDropPageSlot));
        p.setProperty("percentRegex", percentRegex);
        p.setProperty("orderTitle", orderTitle);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, ItemAction> e : rules.entrySet()) {
            if (sb.length() > 0) sb.append(',');
            sb.append(e.getKey()).append('=').append(e.getValue().name());
        }
        p.setProperty("rules", sb.toString());
        try (OutputStream out = Files.newOutputStream(file())) {
            p.store(out, "Hugo Automation");
        } catch (IOException ignored) {
        }
    }

    private static boolean bool(Properties p, String key, boolean def) {
        String v = p.getProperty(key);
        return v == null ? def : Boolean.parseBoolean(v);
    }

    private static int num(Properties p, String key, int def, int min, int max) {
        try {
            int v = Integer.parseInt(p.getProperty(key, Integer.toString(def)));
            return Math.min(Math.max(v, min), max);
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
