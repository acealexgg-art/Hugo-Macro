package de.hugosmp.automation.gui;

import de.hugosmp.automation.HugoAutomationClient;
import de.hugosmp.automation.config.MacroConfig;
import de.hugosmp.automation.core.Macro;
import de.hugosmp.automation.core.MacroManager;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.Screen;

/** Startseite: Sell-Macro, Spawner-Macro, Pause-Banner und Zugang zu Item-Filter und Settings. */
public class MainScreen extends BaseScreen {

    private static final int ROW = 22;
    private static final int CARD_H = 140;

    private int x0, cw, top;
    private Button sellToggle, spawnerToggle, resumeBtn, stopBtn, defaultBtn;
    private String hint = "";
    private long hintUntil;

    public MainScreen(Screen parent) {
        super(Component.literal("HUGO AUTOMATION"), parent);
    }

    private void setHint(String s) {
        hint = s;
        hintUntil = System.currentTimeMillis() + 4000;
    }

    private Component keyText(KeyMapping k) {
        return k.getTranslatedKeyMessage();
    }

    @Override
    protected void init() {
        int total = Math.min(width - 20, 520);
        x0 = (width - total) / 2;
        int gap = 8;
        cw = (total - gap) / 2;
        top = 26;

        int lx = x0 + 8, rx = x0 + cw + gap + 8, iw = cw - 16;
        int y = top + 46;

        // --- Sell-Karte ---
        addRenderableWidget(Button.builder(keyText(HugoAutomationClient.sellKey),
                b -> setHint("Taste ändern: Optionen → Steuerung → Hugo Automation"))
                .bounds(lx + iw - 60, y, 60, 18).build());
        y += ROW;
        defaultBtn = addRenderableWidget(Button.builder(defaultText(), b -> {
            MacroConfig.defaultAction = MacroConfig.defaultAction.next();
            b.setMessage(defaultText());
        }).bounds(lx + iw - 80, y, 80, 18).build());
        y += ROW;
        toggle(lx + iw - 44, y, 44, 18, () -> MacroConfig.skipHotbar, v -> MacroConfig.skipHotbar = v);
        y += ROW;
        sellToggle = button(lx, y, iw, 20, "Start", MacroManager::toggleSell);

        // --- Spawner-Karte ---
        y = top + 46;
        addRenderableWidget(Button.builder(keyText(HugoAutomationClient.spawnerKey),
                b -> setHint("Taste ändern: Optionen → Steuerung → Hugo Automation"))
                .bounds(rx + iw - 60, y, 60, 18).build());
        y += ROW;
        toggle(rx + iw - 44, y, 44, 18, () -> MacroConfig.spawnerUseTrigger, v -> MacroConfig.spawnerUseTrigger = v);
        y += ROW;
        slider(rx, y, iw, 18, 1, 100, () -> MacroConfig.triggerPercent, v -> MacroConfig.triggerPercent = v,
                "Ziel-Auslastung", " %");
        y += ROW;
        spawnerToggle = button(rx, y, iw, 20, "Start", MacroManager::toggleSpawner);

        // --- Pause-Banner ---
        int by = top + CARD_H + 8;
        resumeBtn = button(x0 + 8, by + 22, 90, 18, "Resume", MacroManager::resumeAll);
        stopBtn = button(x0 + 104, by + 22, 90, 18, "Stop", MacroManager::stopAll);

        // --- Unten ---
        int bw = (total - 16) / 3;
        int bottom = height - 28;
        button(x0, bottom, bw, 20, "Item-Filter", () -> HugoAutomationClient.openScreen(minecraft, new ItemFilterScreen(this)));
        button(x0 + bw + 8, bottom, bw, 20, "Settings", () -> HugoAutomationClient.openScreen(minecraft, new SettingsScreen(this)));
        button(x0 + 2 * (bw + 8), bottom, bw, 20, "Schließen", this::onClose);
    }

    private Component defaultText() {
        return Component.literal(MacroConfig.defaultAction.label);
    }

    @Override
    public void tick() {
        sellToggle.setMessage(Component.literal(MacroManager.SELL.isActive() ? "Stop" : "Start"));
        spawnerToggle.setMessage(Component.literal(MacroManager.SPAWNER.isActive() ? "Stop" : "Start"));
        boolean paused = MacroManager.anyPaused();
        resumeBtn.visible = paused;
        stopBtn.visible = paused;
    }

    @Override
    protected void drawBackdrop(GuiGraphicsExtractor g) {
        card(g, x0, top, cw, CARD_H);
        card(g, x0 + cw + 8, top, cw, CARD_H);
        if (MacroManager.anyPaused()) {
            g.fill(x0, top + CARD_H + 8, x0 + 2 * cw + 8, top + CARD_H + 8 + 44, 0xF03A1414);
            g.fill(x0, top + CARD_H + 8, x0 + 3, top + CARD_H + 8 + 44, RED);
        }
    }

    @Override
    protected void drawOverlay(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int lx = x0 + 8, rx = x0 + cw + 16, iw = cw - 16;

        macroHeader(g, lx, top + 8, iw, "SELL MACRO", MacroManager.SELL);
        macroHeader(g, rx, top + 8, iw, "SPAWNER MACRO", MacroManager.SPAWNER);

        int y = top + 46;
        text(g, "Hotkey", lx, y + 5, GRAY);
        text(g, "Standard-Aktion", lx, y + ROW + 5, GRAY);
        text(g, "Hotbar auslassen", lx, y + 2 * ROW + 5, GRAY);

        text(g, "Hotkey", rx, y + 5, GRAY);
        text(g, "100%-Trigger", rx, y + ROW + 5, GRAY);

        if (MacroManager.anyPaused()) {
            int by = top + CARD_H + 8;
            text(g, "⚠ MACRO PAUSED", x0 + 10, by + 5, RED);
            text(g, fit(MacroManager.pauseReason(), 2 * cw - 30), x0 + 10, by + 15, WHITE);
        }

        if (System.currentTimeMillis() < hintUntil) {
            centered(g, hint, width / 2, height - 40, DARK);
        }
    }

    private void macroHeader(GuiGraphicsExtractor g, int x, int y, int w, String name, Macro macro) {
        text(g, name, x, y, WHITE);
        int color = switch (macro.state()) {
            case RUNNING -> GREEN;
            case WAITING -> YELLOW;
            case PAUSED -> RED;
            default -> DARK;
        };
        String label = switch (macro.state()) {
            case RUNNING -> "ACTIVE";
            case WAITING -> "WAITING";
            case PAUSED -> "PAUSED";
            default -> "OFF";
        };
        text(g, "● " + label, x, y + 14, color);
        text(g, fit(macro.detail(), w), x, y + 26, GRAY);
    }
}
