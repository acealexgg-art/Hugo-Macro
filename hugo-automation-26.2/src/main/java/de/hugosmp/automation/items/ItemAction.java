package de.hugosmp.automation.items;

/** Was mit einem Item passieren soll. */
public enum ItemAction {
    SELL("SELL"),
    ORDER("ORDER"),
    DROP("DROP"),
    IGNORE("IGNORE");

    public final String label;

    ItemAction(String label) {
        this.label = label;
    }

    public ItemAction next() {
        ItemAction[] v = values();
        return v[(ordinal() + 1) % v.length];
    }

    public static ItemAction parse(String s, ItemAction def) {
        try {
            return valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return def;
        }
    }
}
