package de.hugosmp.automation.items;

import de.hugosmp.automation.config.MacroConfig;
import net.minecraft.world.item.ItemStack;

/** Entscheidet, was mit einem ItemStack passiert. */
public final class ItemFilter {

    private ItemFilter() {
    }

    /** Aktion laut Regel, sonst Standard-Aktion. */
    public static ItemAction rawAction(ItemStack stack) {
        ItemAction a = MacroConfig.rules.get(ItemKeys.key(stack.getItem()));
        return a != null ? a : MacroConfig.defaultAction;
    }

    /** Verzauberte oder beschaedigte Items werden (auf Wunsch) nie angefasst. */
    public static boolean isProtected(ItemStack stack) {
        return MacroConfig.protectSpecialItems && (stack.isEnchanted() || stack.isDamaged());
    }

    /** Endgueltige Aktion: leere und geschuetzte Items -> IGNORE. */
    public static ItemAction effective(ItemStack stack) {
        if (stack.isEmpty() || isProtected(stack)) return ItemAction.IGNORE;
        ItemAction a = rawAction(stack);
        if (a == ItemAction.ORDER && MacroConfig.orderFallbackSell) return ItemAction.SELL;
        return a;
    }
}
