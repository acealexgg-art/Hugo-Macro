package de.hugosmp.automation.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;

/** Spawner und ihre Drops (nach Angaben aus HugoSMP, Raten pro Minute). */
public final class DropCatalog {

    public record Entry(String key, String label, Item item, String rate) {
    }

    public record Group(String mob, List<Entry> entries) {
    }

    private DropCatalog() {
    }

    private static Entry e(Item item, String label, String rate) {
        return new Entry(ItemKeys.key(item), label, item, rate);
    }

    public static final List<Group> GROUPS = List.of(
            new Group("Skelett", List.of(e(Items.BONE, "Bone", "3,98/min"), e(Items.ARROW, "Arrow", "1,88/min"))),
            new Group("Creeper", List.of(e(Items.GUNPOWDER, "Gunpowder", "5/min"))),
            new Group("Eisengolem", List.of(e(Items.IRON_INGOT, "Iron Ingot", "2/min"))),
            new Group("Blaze", List.of(e(Items.BLAZE_POWDER, "Blaze Powder", "6/min"))),
            new Group("Spinne", List.of(e(Items.STRING, "String", "6/min"), e(Items.SPIDER_EYE, "Spider Eye", "2/min"))),
            new Group("Kuh", List.of(e(Items.BEEF, "Raw Beef", "4/min"))),
            new Group("Zombifizierter Piglin", List.of(e(Items.GOLD_NUGGET, "Gold Nugget", "6/min"), e(Items.ROTTEN_FLESH, "Rotten Flesh", "12/min")))
    );
}
