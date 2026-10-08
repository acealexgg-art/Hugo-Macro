package de.hugosmp.automation.core;

import de.hugosmp.automation.config.MacroConfig;
import de.hugosmp.automation.macros.SellMacro;
import de.hugosmp.automation.macros.SpawnerMacro;
import net.minecraft.client.Minecraft;

/** Haelt beide Macros, tickt sie und gibt im Debug-Modus Menue-Infos aus. */
public final class MacroManager {

    public static final SellMacro SELL = new SellMacro();
    public static final SpawnerMacro SPAWNER = new SpawnerMacro();

    private static String lastSignature = "";

    private MacroManager() {
    }

    public static void toggleSell() {
        if (SELL.isActive()) SELL.stop("");
        else SELL.startNormal();
    }

    public static void toggleSpawner() {
        if (SPAWNER.isActive()) SPAWNER.stop("");
        else SPAWNER.start();
    }

    public static boolean anyPaused() {
        return SELL.state() == Macro.State.PAUSED || SPAWNER.state() == Macro.State.PAUSED;
    }

    public static String pauseReason() {
        if (SELL.state() == Macro.State.PAUSED) return SELL.name() + ": " + SELL.detail();
        if (SPAWNER.state() == Macro.State.PAUSED) return SPAWNER.name() + ": " + SPAWNER.detail();
        return "";
    }

    public static void resumeAll() {
        SELL.resume();
        SPAWNER.resume();
    }

    public static void stopAll() {
        SELL.stop("");
        SPAWNER.stop("");
    }

    public static void tick(Minecraft mc) {
        debugMenu(mc);
        SELL.tick(mc);
        SPAWNER.tick(mc);
    }

    private static void debugMenu(Minecraft mc) {
        if (!MacroConfig.debug || mc.player == null) return;
        GuiKind kind = GuiDetector.kind(mc);
        String sig = kind + "|" + GuiDetector.title() + "|" + GuiDetector.slotCount(mc);
        if (!sig.equals(lastSignature)) {
            lastSignature = sig;
            if (kind != GuiKind.NONE) {
                Chat.debug("Menü: \"" + GuiDetector.title() + "\" (" + GuiDetector.slotCount(mc)
                        + " Slots) → erkannt als " + kind);
            }
        }
    }
}
