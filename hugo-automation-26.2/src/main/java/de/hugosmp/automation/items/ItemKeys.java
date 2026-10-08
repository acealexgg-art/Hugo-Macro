package de.hugosmp.automation.items;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Umrechnung Item <-> Text-ID (z. B. "minecraft:bone"). */
public final class ItemKeys {

    private ItemKeys() {
    }

    public static String key(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    public static Item resolve(String key) {
        Identifier id = Identifier.tryParse(key);
        if (id == null) return Items.AIR;
        Item item = BuiltInRegistries.ITEM.getValue(id);
        return item == null ? Items.AIR : item;
    }
}
