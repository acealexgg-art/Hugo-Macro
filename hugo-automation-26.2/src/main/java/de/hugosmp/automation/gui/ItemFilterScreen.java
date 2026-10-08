package de.hugosmp.automation.gui;

import de.hugosmp.automation.config.MacroConfig;
import de.hugosmp.automation.items.DropCatalog;
import de.hugosmp.automation.items.ItemAction;
import de.hugosmp.automation.items.ItemKeys;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Item-Filter: pro Item eine Aktion (SELL / ORDER / DROP / IGNORE). */
public class ItemFilterScreen extends BaseScreen {

    private static final int ROW_H = 22;

    private int x0, w, top, perPage;
    private int page = 0;
    private final List<String> shown = new ArrayList<>();

    public ItemFilterScreen(Screen parent) {
        super(Component.literal("ITEM FILTER"), parent);
    }

    @Override
    protected void init() {
        shown.clear();
        w = Math.min(width - 20, 380);
        x0 = (width - w) / 2;
        top = 26;
        int listTop = top + 22;
        perPage = Math.max(1, (height - 82 - listTop) / ROW_H);

        List<String> all = new ArrayList<>(MacroConfig.rules.keySet());
        int pages = Math.max(1, (int) Math.ceil(all.size() / (double) perPage));
        page = Math.min(page, pages - 1);

        int from = page * perPage;
        for (int i = from; i < Math.min(all.size(), from + perPage); i++) {
            String key = all.get(i);
            shown.add(key);
            int y = listTop + (i - from) * ROW_H;
            addRenderableWidget(Button.builder(Component.literal(MacroConfig.rules.get(key).label), b -> {
                MacroConfig.rules.put(key, MacroConfig.rules.get(key).next());
                b.setMessage(Component.literal(MacroConfig.rules.get(key).label));
            }).bounds(x0 + w - 88, y, 62, ROW_H - 4).build());
            addRenderableWidget(Button.builder(Component.literal("x"), b -> {
                MacroConfig.rules.remove(key);
                rebuildWidgets();
            }).bounds(x0 + w - 22, y, 18, ROW_H - 4).build());
        }

        Button prev = button(x0 + w - 46, top + 2, 20, 16, "<", () -> {
            page = Math.max(0, page - 1);
            rebuildWidgets();
        });
        Button next = button(x0 + w - 24, top + 2, 20, 16, ">", () -> {
            page++;
            rebuildWidgets();
        });
        prev.active = page > 0;
        next.active = page < pages - 1;

        int bw = (w - 8) / 3;
        int by1 = height - 52, by2 = height - 28;
        button(x0, by1, bw, 20, "+ Item in Hand", this::addHand);
        button(x0 + bw + 4, by1, bw, 20, "+ Inventar", this::addInventory);
        button(x0 + 2 * (bw + 4), by1, bw, 20, "+ Spawner-Items", this::addCatalog);
        button(x0, by2, w / 2 - 2, 20, "Alle entfernen", () -> {
            MacroConfig.rules.clear();
            rebuildWidgets();
        });
        button(x0 + w / 2 + 2, by2, w / 2 - 2, 20, "Zurück", this::onClose);
    }

    private void addRule(Item item) {
        if (item == Items.AIR) return;
        MacroConfig.rules.putIfAbsent(ItemKeys.key(item), ItemAction.SELL);
    }

    private void addHand() {
        if (minecraft != null && minecraft.player != null) {
            ItemStack s = minecraft.player.getMainHandItem();
            if (!s.isEmpty()) addRule(s.getItem());
        }
        rebuildWidgets();
    }

    private void addInventory() {
        if (minecraft != null && minecraft.player != null) {
            Inventory inv = minecraft.player.getInventory();
            for (int i = 0; i < 36; i++) {
                ItemStack s = inv.getItem(i);
                if (!s.isEmpty()) addRule(s.getItem());
            }
        }
        rebuildWidgets();
    }

    private void addCatalog() {
        for (DropCatalog.Group g : DropCatalog.GROUPS) {
            for (DropCatalog.Entry e : g.entries()) {
                MacroConfig.rules.putIfAbsent(e.key(), ItemAction.IGNORE);
            }
        }
        rebuildWidgets();
    }

    @Override
    protected void drawBackdrop(GuiGraphicsExtractor g) {
        card(g, x0, top, w, height - 58 - top);
    }

    @Override
    protected void drawOverlay(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        text(g, "Items: " + MacroConfig.rules.size() + "   (Standard: " + MacroConfig.defaultAction.label + ")",
                x0 + 8, top + 7, GRAY);
        int listTop = top + 22;
        if (MacroConfig.rules.isEmpty()) {
            text(g, fit("Noch leer – Item in die Hand nehmen oder Spawner-Items hinzufügen.", w - 16),
                    x0 + 8, listTop + 6, DARK);
        }
        for (int i = 0; i < shown.size(); i++) {
            int y = listTop + i * ROW_H;
            Item item = ItemKeys.resolve(shown.get(i));
            ItemStack stack = new ItemStack(item);
            g.item(stack, x0 + 8, y);
            String name = item == Items.AIR ? shown.get(i) : stack.getHoverName().getString();
            text(g, fit(name, w - 120), x0 + 30, y + 5, WHITE);
        }
    }
}
