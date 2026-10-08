package de.hugosmp.automation.core;

import de.hugosmp.automation.config.MacroConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Nachrichten an den Spieler (nur im eigenen Chat sichtbar). */
public final class Chat {

    private Chat() {
    }

    private static void send(String text, ChatFormatting color) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.player.sendSystemMessage(Component.literal("[Auto] " + text).withStyle(color));
    }

    public static void info(String text) { send(text, ChatFormatting.GRAY); }
    public static void warn(String text) { send(text, ChatFormatting.YELLOW); }
    public static void debug(String text) {
        if (MacroConfig.debug) send(text, ChatFormatting.DARK_GRAY);
    }
}
