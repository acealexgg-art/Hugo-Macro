package de.hugosmp.automation.core;

import de.hugosmp.automation.config.MacroConfig;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Erkennt, welches Menue offen ist. Der Titel wird beim Oeffnen eines Container-Screens
 * mitgeschrieben (Minecraft 26.2 hat kein oeffentliches "aktueller Screen"-Feld).
 */
public final class GuiDetector {

    private static String title = "";

    private GuiDetector() {
    }

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof AbstractContainerScreen<?>) {
                title = screen.getTitle().getString();
                ScreenEvents.remove(screen).register(s -> title = "");
            }
        });
    }

    public static String title() {
        return title;
    }

    public static boolean containerOpen(Minecraft mc) {
        LocalPlayer p = mc.player;
        return p != null && p.containerMenu != null && p.containerMenu != p.inventoryMenu;
    }

    public static GuiKind kind(Minecraft mc) {
        if (!containerOpen(mc)) return GuiKind.NONE;
        String t = title.toLowerCase();
        if (!MacroConfig.sellTitle.isBlank() && t.contains(MacroConfig.sellTitle.toLowerCase())) return GuiKind.SELL;
        if (!MacroConfig.spawnerTitle.isBlank() && t.contains(MacroConfig.spawnerTitle.toLowerCase())) return GuiKind.SPAWNER;
        if (!MacroConfig.orderTitle.isBlank() && t.contains(MacroConfig.orderTitle.toLowerCase())) return GuiKind.ORDER;
        return GuiKind.UNKNOWN;
    }

    public static int slotCount(Minecraft mc) {
        AbstractContainerMenu m = mc.player == null ? null : mc.player.containerMenu;
        return m == null ? 0 : m.slots.size();
    }
}
