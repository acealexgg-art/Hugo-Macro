package de.hugosmp.automation.core;

import de.hugosmp.automation.items.ItemKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.List;

/** Hilfsfunktionen zum Lesen der offenen Container-Menues. */
public final class InventoryManager {

    private InventoryManager() {
    }

    public static AbstractContainerMenu menu(Minecraft mc) {
        return mc.player == null ? null : mc.player.containerMenu;
    }

    /** Anzahl Slots des oberen Containers (ohne die 36 Spieler-Slots). */
    public static int topSize(AbstractContainerMenu m) {
        return m.slots.size() - 36;
    }

    public static ItemStack stack(AbstractContainerMenu m, int slot) {
        if (slot < 0 || slot >= m.slots.size()) return ItemStack.EMPTY;
        return m.getSlot(slot).getItem();
    }

    /** Ist der Slot ein Hotbar-Slot des Spielers (letzte 9 Slots)? */
    public static boolean isHotbar(AbstractContainerMenu m, int slot) {
        return slot >= m.slots.size() - 9;
    }

    public static int emptySlots(AbstractContainerMenu m, int from, int to, int exclude) {
        int n = 0;
        for (int i = from; i < to; i++) {
            if (i != exclude && stack(m, i).isEmpty()) n++;
        }
        return n;
    }

    public static int itemSum(AbstractContainerMenu m, int from, int to, int exclude) {
        int n = 0;
        for (int i = from; i < to; i++) {
            if (i != exclude) n += stack(m, i).getCount();
        }
        return n;
    }

    public static String signature(AbstractContainerMenu m, int from, int to) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < to; i++) {
            ItemStack s = stack(m, i);
            if (!s.isEmpty()) {
                sb.append(i).append(':').append(ItemKeys.key(s.getItem())).append('x').append(s.getCount()).append(';');
            }
        }
        return sb.toString();
    }

    /** Alle Tooltip-Zeilen (inkl. Name) als Text. */
    public static List<String> tooltip(Minecraft mc, ItemStack stack) {
        List<String> out = new ArrayList<>();
        if (stack.isEmpty() || mc.player == null || mc.level == null) return out;
        for (Component c : stack.getTooltipLines(Item.TooltipContext.of(mc.level), mc.player, TooltipFlag.NORMAL)) {
            out.add(c.getString());
        }
        return out;
    }
}
