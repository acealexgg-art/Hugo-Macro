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
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Spawner-Macro. Arbeitet im geoeffneten Spawner-Menue (das du selbst oeffnest):
 * Auslastung lesen -> bei Ziel-Prozent Seite verarbeiten (DROP / SELL / IGNORE) -> naechste Seite ->
 * am Ende zurueck blaettern und warten, bis der Spawner wieder gefuellt ist.
 * SELL-Items werden ins Inventar verschoben und danach per Sell-Macro verkauft.
 */
public class SpawnerMacro extends Macro {

    private enum Step { WAIT_GUI, WATCH, ACT, AFTER_PAGE, WAIT_PAGE, BACK }

    private record Act(int slot, int button, ContainerInput type) {
    }

    private Step step = Step.WAIT_GUI;
    private final ArrayDeque<Act> queue = new ArrayDeque<>();
    private long deadline;
    private long nextCheck;
    private int retries;
    private int page;
    private int backLeft;
    private boolean movedForSell;
    private boolean infoPrinted;
    private String sigBefore = "";

    public SpawnerMacro() {
        super("Spawner-Macro");
    }

    @Override
    protected void reset() {
        step = Step.WAIT_GUI;
        queue.clear();
        retries = 0;
        page = 0;
        backLeft = 0;
        movedForSell = false;
        infoPrinted = false;
        nextCheck = 0;
    }

    private long timeout() {
        return System.currentTimeMillis() + MacroConfig.responseTimeoutMs;
    }

    @Override
    protected void run(Minecraft mc) {
        GuiKind kind = GuiDetector.kind(mc);
        if (kind != GuiKind.SPAWNER) {
            if (step == Step.ACT || step == Step.AFTER_PAGE || step == Step.WAIT_PAGE || step == Step.BACK) {
                pause("Spawner-Menü wurde unerwartet geschlossen");
                return;
            }
            step = Step.WAIT_GUI;
            waiting("Öffne das Spawner-Menü");
            return;
        }
        AbstractContainerMenu m = InventoryManager.menu(mc);
        int top = InventoryManager.topSize(m);
        int maxSlot = Math.max(Math.max(MacroConfig.spawnerDropPageSlot, MacroConfig.spawnerNextSlot),
                Math.max(MacroConfig.spawnerPrevSlot, MacroConfig.spawnerInfoSlot));
        if (maxSlot >= top || MacroConfig.spawnerStorageSlots > top) {
            pause("Spawner-Slots passen nicht zum Menü (" + top + " Slots) – Config prüfen");
            return;
        }
        switch (step) {
            case WAIT_GUI -> {
                step = Step.WATCH;
                page = 0;
                infoPrinted = false;
            }
            case WATCH -> watch(mc, m);
            case ACT -> act(mc);
            case AFTER_PAGE -> afterPage(mc, m);
            case WAIT_PAGE -> waitPage(mc, m);
            case BACK -> back(mc, m);
        }
    }

    // ---------- Beobachten ----------

    private void watch(Minecraft mc, AbstractContainerMenu m) {
        if (System.currentTimeMillis() < nextCheck) return;
        nextCheck = System.currentTimeMillis() + 500;

        if (MacroConfig.spawnerUseTrigger) {
            double pct = readPercent(mc, m);
            if (pct < 0) {
                waiting("Auslastung nicht lesbar (Debug-Modus zeigt die Info-Zeilen)");
                return;
            }
            if (pct < MacroConfig.triggerPercent) {
                waiting(String.format("Auslastung %.0f %% / Ziel %d %%", pct, MacroConfig.triggerPercent));
                return;
            }
        }
        if (!buildQueue(m)) return;
        retries = 0;
        step = queue.isEmpty() ? Step.AFTER_PAGE : Step.ACT;
        deadline = timeout();
        running("Verarbeite Seite " + (page + 1));
    }

    private double readPercent(Minecraft mc, AbstractContainerMenu m) {
        ItemStack info = InventoryManager.stack(m, MacroConfig.spawnerInfoSlot);
        if (info.isEmpty()) return -1;
        List<String> lines = InventoryManager.tooltip(mc, info);
        if (!infoPrinted) {
            infoPrinted = true;
            Chat.debug("Spawner-Info-Slot " + MacroConfig.spawnerInfoSlot + ": " + String.join(" | ", lines));
        }
        try {
            Pattern p = Pattern.compile(MacroConfig.percentRegex);
            for (String line : lines) {
                Matcher mt = p.matcher(line);
                if (mt.find()) {
                    return Double.parseDouble(mt.group(1).replace(',', '.'));
                }
            }
        } catch (RuntimeException ignored) {
        }
        return -1;
    }

    // ---------- Plan fuer die aktuelle Seite ----------

    private boolean buildQueue(AbstractContainerMenu m) {
        queue.clear();
        int end = MacroConfig.spawnerStorageSlots;
        List<Integer> slots = new ArrayList<>();
        List<ItemAction> actions = new ArrayList<>();
        boolean allDrop = true;
        for (int i = 0; i < end; i++) {
            ItemStack s = InventoryManager.stack(m, i);
            if (s.isEmpty()) continue;
            ItemAction a = ItemFilter.effective(s);
            if (a == ItemAction.ORDER) {
                pause("\"" + s.getHoverName().getString() + "\" steht auf ORDER – die /order-Automation ist noch nicht eingebaut "
                        + "(Aktion ändern oder „ORDER → SELL Fallback“ aktivieren)");
                return false;
            }
            slots.add(i);
            actions.add(a);
            if (a != ItemAction.DROP) allDrop = false;
        }
        if (!slots.isEmpty() && allDrop && MacroConfig.useDropButton) {
            queue.add(new Act(MacroConfig.spawnerDropPageSlot, 0, ContainerInput.PICKUP));
            return true;
        }
        for (int k = 0; k < slots.size(); k++) {
            ItemAction a = actions.get(k);
            if (a == ItemAction.DROP) {
                queue.add(new Act(slots.get(k), 1, ContainerInput.THROW));
            } else if (a == ItemAction.SELL) {
                queue.add(new Act(slots.get(k), 0, ContainerInput.QUICK_MOVE));
                movedForSell = true;
            }
        }
        return true;
    }

    private boolean pageHasActionable(AbstractContainerMenu m) {
        for (int i = 0; i < MacroConfig.spawnerStorageSlots; i++) {
            ItemAction a = ItemFilter.effective(InventoryManager.stack(m, i));
            if (a == ItemAction.DROP || a == ItemAction.SELL) return true;
        }
        return false;
    }

    // ---------- Ausfuehren ----------

    private void act(Minecraft mc) {
        if (!ClickManager.ready()) return;
        Act a = queue.poll();
        if (a == null) {
            step = Step.AFTER_PAGE;
            deadline = timeout();
            return;
        }
        ClickManager.click(mc, a.slot(), a.button(), a.type());
    }

    private void afterPage(Minecraft mc, AbstractContainerMenu m) {
        boolean pending = pageHasActionable(m);
        if (pending && System.currentTimeMillis() < deadline) return;
        if (pending) {
            if (++retries > MacroConfig.retryCount) {
                pause("Items konnten nicht verarbeitet werden (Inventar voll?)");
                return;
            }
            if (!buildQueue(m)) return;
            step = Step.ACT;
            deadline = timeout();
            return;
        }
        retries = 0;
        boolean hasNext = !InventoryManager.stack(m, MacroConfig.spawnerNextSlot).isEmpty();
        if (hasNext && page < 50) {
            if (!ClickManager.ready()) return;
            sigBefore = InventoryManager.signature(m, 0, MacroConfig.spawnerStorageSlots);
            ClickManager.click(mc, MacroConfig.spawnerNextSlot, 0, ContainerInput.PICKUP);
            page++;
            step = Step.WAIT_PAGE;
            deadline = timeout();
            running("Nächste Seite (" + (page + 1) + ")");
        } else {
            finish(mc);
        }
    }

    private void waitPage(Minecraft mc, AbstractContainerMenu m) {
        String sig = InventoryManager.signature(m, 0, MacroConfig.spawnerStorageSlots);
        if (!sig.equals(sigBefore)) {
            if (!buildQueue(m)) return;
            retries = 0;
            step = queue.isEmpty() ? Step.AFTER_PAGE : Step.ACT;
            deadline = timeout();
            running("Verarbeite Seite " + (page + 1));
        } else if (System.currentTimeMillis() > deadline) {
            page--;
            finish(mc);
        }
    }

    private void finish(Minecraft mc) {
        if (movedForSell) {
            movedForSell = false;
            step = Step.WAIT_GUI;
            mc.player.closeContainer();
            MacroManager.SELL.startOneShot();
            running("Verkaufe entnommene Items");
            return;
        }
        if (page > 0) {
            backLeft = page;
            step = Step.BACK;
            return;
        }
        step = Step.WATCH;
        nextCheck = System.currentTimeMillis() + 3000;
        waiting("Fertig – warte auf Auffüllung");
    }

    private void back(Minecraft mc, AbstractContainerMenu m) {
        if (backLeft <= 0 || InventoryManager.stack(m, MacroConfig.spawnerPrevSlot).isEmpty()) {
            page = 0;
            step = Step.WATCH;
            nextCheck = System.currentTimeMillis() + 3000;
            waiting("Fertig – warte auf Auffüllung");
            return;
        }
        if (!ClickManager.ready()) return;
        ClickManager.click(mc, MacroConfig.spawnerPrevSlot, 0, ContainerInput.PICKUP);
        backLeft--;
    }
}
