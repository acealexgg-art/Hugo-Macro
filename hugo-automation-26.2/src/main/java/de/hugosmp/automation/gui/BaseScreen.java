package de.hugosmp.automation.gui;

import de.hugosmp.automation.HugoAutomationClient;
import de.hugosmp.automation.config.MacroConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Gemeinsamer Look: dunkle Karten, violette Akzente. Panels werden VOR den Widgets gezeichnet. */
public abstract class BaseScreen extends Screen {

    protected static final int CARD = 0xF0141418;
    protected static final int ACCENT = 0xFFB050D0;
    protected static final int WHITE = 0xFFFFFFFF;
    protected static final int GRAY = 0xFFAAAAAA;
    protected static final int DARK = 0xFF666666;
    protected static final int GREEN = 0xFF55FF55;
    protected static final int YELLOW = 0xFFFFDD55;
    protected static final int RED = 0xFFFF5555;

    protected final Screen parent;

    protected BaseScreen(Component title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    /** Hintergrund-Karten (werden unter den Widgets gezeichnet). */
    protected abstract void drawBackdrop(GuiGraphicsExtractor g);

    /** Texte und Icons (werden ueber den Widgets gezeichnet). */
    protected abstract void drawOverlay(GuiGraphicsExtractor g, int mouseX, int mouseY);

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        drawBackdrop(g);
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        centered(g, title.getString(), width / 2, 9, WHITE);
        drawOverlay(g, mouseX, mouseY);
    }

    @Override
    public void onClose() {
        MacroConfig.save();
        HugoAutomationClient.openScreen(minecraft, parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---------- Zeichnen ----------

    protected void card(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, CARD);
        g.fill(x, y, x + w, y + 2, ACCENT);
    }

    protected void text(GuiGraphicsExtractor g, String s, int x, int y, int color) {
        g.text(font, Component.literal(s), x, y, color, true);
    }

    protected void text(GuiGraphicsExtractor g, Component c, int x, int y, int color) {
        g.text(font, c, x, y, color, true);
    }

    protected void centered(GuiGraphicsExtractor g, String s, int cx, int y, int color) {
        g.centeredText(font, Component.literal(s), cx, y, color);
    }

    protected String fit(String s, int maxW) {
        if (font.width(s) <= maxW) return s;
        while (s.length() > 1 && font.width(s + "…") > maxW) {
            s = s.substring(0, s.length() - 1);
        }
        return s + "…";
    }

    // ---------- Widgets ----------

    protected static Component onOff(boolean v) {
        return Component.literal(v ? "AN" : "AUS").withStyle(v ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    protected Button toggle(int x, int y, int w, int h, BooleanSupplier get, Consumer<Boolean> set) {
        return addRenderableWidget(Button.builder(onOff(get.getAsBoolean()), b -> {
            boolean v = !get.getAsBoolean();
            set.accept(v);
            b.setMessage(onOff(v));
        }).bounds(x, y, w, h).build());
    }

    protected Button button(int x, int y, int w, int h, String label, Runnable action) {
        return addRenderableWidget(Button.builder(Component.literal(label), b -> action.run())
                .bounds(x, y, w, h).build());
    }

    protected IntSlider slider(int x, int y, int w, int h, int min, int max, IntSupplier get, IntConsumer set,
                               String label, String unit) {
        return addRenderableWidget(new IntSlider(x, y, w, h, min, max, get, set, label, unit));
    }

    /** Slider mit ganzzahligem Wert. */
    protected static class IntSlider extends AbstractSliderButton {
        private final int min;
        private final int max;
        private final IntConsumer setter;
        private final String label;
        private final String unit;

        IntSlider(int x, int y, int w, int h, int min, int max, IntSupplier getter, IntConsumer setter,
                  String label, String unit) {
            super(x, y, w, h, Component.empty(), (getter.getAsInt() - min) / (double) (max - min));
            this.min = min;
            this.max = max;
            this.setter = setter;
            this.label = label;
            this.unit = unit;
            updateMessage();
        }

        private int current() {
            return min + (int) Math.round(value * (max - min));
        }

        @Override
        protected void updateMessage() {
            if (label == null) {
                setMessage(Component.empty());
            } else {
                setMessage(Component.literal(label + ": " + current() + unit));
            }
        }

        @Override
        protected void applyValue() {
            if (setter != null) {
                setter.accept(current());
            }
        }
    }
}
