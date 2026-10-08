package de.hugosmp.automation.macros;

import de.hugosmp.automation.config.MacroConfig;
import de.hugosmp.automation.core.Chat;
import de.hugosmp.automation.core.ClickManager;
import de.hugosmp.automation.core.GuiDetector;
import de.hugosmp.automation.core.GuiKind;
import de.hugosmp.automation.core.InventoryManager;
import de.hugosmp.automation.core.Macro;
import de.hugosmp.automation.core.MacroManager;
import de.hugosmp.automation.items.ItemAction;
import de.hugosmp.automation.items.ItemFilter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Set;

/**
 * Sell-Macro: /sell oeffnen -> Items per Shift-Klick einlegen -> Haken klicken -> pruefen -> wiederholen.
 * Jeder Schritt wartet auf die Server-Antwort (Slot-Zustand) und pausiert bei Problemen.
 */
public class SellMacro extends Macro {

    private enum Step { START, OPENING, FILLING, WAIT_MOVE, CONFIRMING, WAIT_SOLD, WAIT_ITEMS }

    private Step step = Step.START;
    private long deadline;
    private int retries;
    private int movedSlot = -1;
    private int sumBefore;
    private int cycles;
    private boolean oneShot;
    private final Set<Integer> stuck = new HashSet<>();

    public SellMacro() {
        super("Sell-Macro");
    }

    public void startNormal() {
        oneShot = false;
        start();
    }

    /** Einmaliger Durchlauf (z. B. nach dem Leeren eines Spawners). */
    public void startOneShot() {
        if (isActive()) return;
        oneShot = true;
        start();
    }

    @Override
    protected void reset() {
        step = Step.START;
        retries = 0;
        movedSlot = -1;
        cycles = 0;
        stuck.clear();
    }

    private long timeout() {
        return System.currentTimeMillis() + MacroConfig.responseTimeoutMs;
    }

    @Override
    protected void run(Minecraft mc) {
        GuiKind kind = GuiDetector.kind(mc);
        switch (step) {
            case START -> start(mc, kind);
            case OPENING -> opening(kind);
            case FILLING -> filling(mc, kind);
            case WAIT_MOVE -> waitMove(mc, kind);
            case CONFIRMING -> confirming(mc, kind);
            case WAIT_SOLD -> waitSold(mc, kind);
            case WAIT_ITEMS -> waitItems(mc, kind);
        }
    }

    private void start(Minecraft mc, GuiKind kind) {
        if (kind == GuiKind.SELL) {
            step = Step.FILLING;
            return;
        }
        if (kind != GuiKind.NONE) {
            if (kind == GuiKind.SPAWNER && MacroManager.SPAWNER.isActive()) {
                waiting("Spawner-Menü ist offen");
                return;
            }
            if (MacroConfig.pauseOnUnknownGui) {
                pause("Anderes Menü offen: \"" + GuiDetector.title() + "\"");
            } else {
                mc.player.closeContainer();
            }
            return;
        }
        if (!hasSellable(mc)) {
            nothingToSell();
            return;
        }
        mc.player.connection.sendCommand(MacroConfig.sellCommand);
        step = Step.OPENING;
        deadline = timeout();
        running("Öffne /" + MacroConfig.sellCommand);
    }

    private void opening(GuiKind kind) {
        if (kind == GuiKind.SELL) {
            retries = 0;
            step = Step.FILLING;
            return;
        }
        if (kind != GuiKind.NONE) {
            pause("Unerwartetes Menü statt Sell-Menü: \"" + GuiDetector.title() + "\"");
            return;
        }
        if (System.currentTimeMillis() > deadline) {
            if (++retries > MacroConfig.retryCount) {
                pause("Sell-Menü öffnet sich nicht (Befehl /" + MacroConfig.sellCommand + " prüfen)");
                return;
            }
            step = Step.START;
        }
    }

    private void filling(Minecraft mc, GuiKind kind) {
        if (kind != GuiKind.SELL) {
            if (++retries > MacroConfig.retryCount) {
                pause("Sell-Menü wurde unerwartet geschlossen");
                return;
            }
            step = Step.START;
            return;
        }
        AbstractContainerMenu m = InventoryManager.menu(mc);
        int top = InventoryManager.topSize(m);
        int confirm = MacroConfig.sellConfirmSlot;
        if (confirm >= top) {
            pause("Bestätigungs-Slot " + confirm + " existiert nicht (Menü hat " + top + " Slots)");
            return;
        }
        if (!ClickManager.ready()) return;

        int empty = InventoryManager.emptySlots(m, 0, top, confirm);
        int candidate = findCandidate(m, top);
        if (empty == 0 || candidate < 0) {
            if (InventoryManager.itemSum(m, 0, top, confirm) > 0) {
                step = Step.CONFIRMING;
            } else {
                nothingToSell();
            }
            return;
        }
        sumBefore = InventoryManager.itemSum(m, 0, top, confirm);
        movedSlot = candidate;
        ClickManager.click(mc, candidate, 0, ContainerInput.QUICK_MOVE);
        step = Step.WAIT_MOVE;
        deadline = timeout();
        running("Lege Items ein");
    }

    private void waitMove(Minecraft mc, GuiKind kind) {
        if (kind != GuiKind.SELL) {
            step = Step.FILLING;
            return;
        }
        AbstractContainerMenu m = InventoryManager.menu(mc);
        int top = InventoryManager.topSize(m);
        boolean moved = InventoryManager.stack(m, movedSlot).isEmpty()
                || InventoryManager.itemSum(m, 0, top, MacroConfig.sellConfirmSlot) > sumBefore;
        if (moved) {
            retries = 0;
            step = Step.FILLING;
            return;
        }
        if (System.currentTimeMillis() > deadline) {
            if (++retries > MacroConfig.retryCount) {
                stuck.add(movedSlot);
                retries = 0;
            }
            step = Step.FILLING;
        }
    }

    private void confirming(Minecraft mc, GuiKind kind) {
        if (kind != GuiKind.SELL) {
            step = Step.FILLING;
            return;
        }
        AbstractContainerMenu m = InventoryManager.menu(mc);
        int confirm = MacroConfig.sellConfirmSlot;
        if (InventoryManager.stack(m, confirm).isEmpty()) {
            pause("Bestätigungs-Button (Slot " + confirm + ") nicht gefunden");
            return;
        }
        if (!ClickManager.ready()) return;
        int top = InventoryManager.topSize(m);
        sumBefore = InventoryManager.itemSum(m, 0, top, confirm);
        ClickManager.click(mc, confirm, 0, ContainerInput.PICKUP);
        step = Step.WAIT_SOLD;
        deadline = timeout();
        running("Bestätige Verkauf");
    }

    private void waitSold(Minecraft mc, GuiKind kind) {
        if (kind != GuiKind.SELL) {
            soldCycle();
            step = Step.START;
            return;
        }
        AbstractContainerMenu m = InventoryManager.menu(mc);
        int top = InventoryManager.topSize(m);
        int sum = InventoryManager.itemSum(m, 0, top, MacroConfig.sellConfirmSlot);
        if (sum == 0 || sum < sumBefore) {
            soldCycle();
            step = Step.FILLING;
            return;
        }
        if (System.currentTimeMillis() > deadline) {
            if (++retries > MacroConfig.retryCount) {
                pause("Verkauf nicht bestätigt (Items bleiben im Menü)");
                return;
            }
            step = Step.CONFIRMING;
        }
    }

    private void soldCycle() {
        cycles++;
        retries = 0;
        stuck.clear();
        running("Verkauf #" + cycles + " abgeschlossen");
    }

    private void waitItems(Minecraft mc, GuiKind kind) {
        if (hasSellable(mc)) {
            step = Step.START;
            return;
        }
        waiting("Warte auf neue Items (" + cycles + " Verkäufe)");
    }

    private void nothingToSell() {
        if (oneShot) {
            Chat.info("Sell-Macro: fertig, nichts mehr zu verkaufen.");
            stop("");
        } else {
            step = Step.WAIT_ITEMS;
            waiting("Warte auf neue Items (" + cycles + " Verkäufe)");
        }
    }

    private int findCandidate(AbstractContainerMenu m, int top) {
        for (int i = top; i < m.slots.size(); i++) {
            if (stuck.contains(i)) continue;
            if (MacroConfig.skipHotbar && InventoryManager.isHotbar(m, i)) continue;
            ItemStack s = InventoryManager.stack(m, i);
            if (ItemFilter.effective(s) == ItemAction.SELL) return i;
        }
        return -1;
    }

    /** Prueft das Spieler-Inventar direkt (funktioniert auch ohne offenes Menue). */
    private boolean hasSellable(Minecraft mc) {
        LocalPlayer p = mc.player;
        Inventory inv = p.getInventory();
        for (int i = 0; i < 36; i++) {
            if (MacroConfig.skipHotbar && i < 9) continue;
            if (ItemFilter.effective(inv.getItem(i)) == ItemAction.SELL) return true;
        }
        return false;
    }
}
