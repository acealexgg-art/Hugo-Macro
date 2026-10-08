package de.hugosmp.automation.gui;

import de.hugosmp.automation.config.MacroConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Zeiten, Sicherheits-Optionen und Debug. Server-Details stehen in der Config-Datei. */
public class SettingsScreen extends BaseScreen {

    private static final int ROW = 22;
    private int x0, w, top;

    public SettingsScreen(Screen parent) {
        super(Component.literal("SETTINGS"), parent);
    }

    @Override
    protected void init() {
        w = Math.min(width - 20, 360);
        x0 = (width - w) / 2;
        top = 26;
        int y = top + 12;
        int right = x0 + w - 8;

        slider(x0 + 8, y, w - 16, 18, 20, 500, () -> MacroConfig.clickDelayMs, v -> MacroConfig.clickDelayMs = v,
                "Click Delay", " ms");
        y += ROW;
        slider(x0 + 8, y, w - 16, 18, 500, 8000, () -> MacroConfig.responseTimeoutMs, v -> MacroConfig.responseTimeoutMs = v,
                "Server Response Delay", " ms");
        y += ROW;
        slider(x0 + 8, y, w - 16, 18, 0, 10, () -> MacroConfig.retryCount, v -> MacroConfig.retryCount = v,
                "Retry Count", "");
        y += ROW + 4;
        toggle(right - 44, y, 44, 18, () -> MacroConfig.pauseOnUnknownGui, v -> MacroConfig.pauseOnUnknownGui = v);
        y += ROW;
        toggle(right - 44, y, 44, 18, () -> MacroConfig.protectSpecialItems, v -> MacroConfig.protectSpecialItems = v);
        y += ROW;
        toggle(right - 44, y, 44, 18, () -> MacroConfig.skipHotbar, v -> MacroConfig.skipHotbar = v);
        y += ROW;
        toggle(right - 44, y, 44, 18, () -> MacroConfig.orderFallbackSell, v -> MacroConfig.orderFallbackSell = v);
        y += ROW;
        toggle(right - 44, y, 44, 18, () -> MacroConfig.useDropButton, v -> MacroConfig.useDropButton = v);
        y += ROW;
        toggle(right - 44, y, 44, 18, () -> MacroConfig.debug, v -> MacroConfig.debug = v);

        button(x0 + w / 2 - 50, height - 28, 100, 20, "Zurück", this::onClose);
    }

    @Override
    protected void drawBackdrop(GuiGraphicsExtractor g) {
        card(g, x0, top, w, height - 28 - top - 6);
    }

    @Override
    protected void drawOverlay(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int y = top + 12 + 3 * ROW + 4 + 5;
        int lx = x0 + 8;
        text(g, "Pause bei unbekanntem GUI", lx, y, GRAY);
        text(g, "Verzauberte/beschädigte Items schützen", lx, y + ROW, GRAY);
        text(g, "Hotbar auslassen", lx, y + 2 * ROW, GRAY);
        text(g, "ORDER → SELL Fallback", lx, y + 3 * ROW, GRAY);
        text(g, "Dropper-Button nutzen (nur DROP-Seiten)", lx, y + 4 * ROW, GRAY);
        text(g, "Debug Mode (Menü-Infos im Chat)", lx, y + 5 * ROW, GRAY);
        text(g, fit("Menü-Titel, Slots, Befehl: config/hugoautomation.properties", w - 16), lx, y + 6 * ROW + 2, DARK);
    }
}
