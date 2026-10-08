package de.hugosmp.automation.core;

import de.hugosmp.automation.config.MacroConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;

/** Einziger Ort, an dem geklickt wird. Haelt den eingestellten Abstand zwischen Klicks ein. */
public final class ClickManager {

    private static long nextAllowed = 0;

    private ClickManager() {
    }

    public static boolean ready() {
        return System.currentTimeMillis() >= nextAllowed;
    }

    public static void click(Minecraft mc, int slot, int button, ContainerInput type) {
        if (mc.player == null || mc.gameMode == null) return;
        AbstractContainerMenu m = mc.player.containerMenu;
        mc.gameMode.handleContainerInput(m.containerId, slot, button, type, mc.player);
        nextAllowed = System.currentTimeMillis() + MacroConfig.clickDelayMs;
    }
}
