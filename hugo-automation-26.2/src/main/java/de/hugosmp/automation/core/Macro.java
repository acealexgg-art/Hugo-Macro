package de.hugosmp.automation.core;

import net.minecraft.client.Minecraft;

/** Gemeinsame Basis: Zustand, Status-Text, Pause mit Grund. */
public abstract class Macro {

    public enum State { OFF, RUNNING, WAITING, PAUSED }

    private final String name;
    private State state = State.OFF;
    private String detail = "Aus";

    protected Macro(String name) {
        this.name = name;
    }

    public String name() { return name; }
    public State state() { return state; }
    public String detail() { return detail; }
    public boolean isActive() { return state != State.OFF; }

    protected abstract void reset();

    protected abstract void run(Minecraft mc);

    public void start() {
        if (state != State.OFF) return;
        reset();
        state = State.RUNNING;
        detail = "Gestartet";
        Chat.info(name + ": AN");
    }

    public void stop(String why) {
        if (state == State.OFF) return;
        state = State.OFF;
        detail = "Aus";
        Chat.info(name + ": AUS" + (why.isEmpty() ? "" : " (" + why + ")"));
    }

    public void pause(String reason) {
        state = State.PAUSED;
        detail = reason;
        Chat.warn(name + " pausiert: " + reason);
    }

    public void resume() {
        if (state == State.PAUSED) {
            reset();
            state = State.RUNNING;
            detail = "Fortgesetzt";
        }
    }

    protected void running(String text) {
        state = State.RUNNING;
        detail = text;
    }

    protected void waiting(String text) {
        state = State.WAITING;
        detail = text;
    }

    public final void tick(Minecraft mc) {
        if (state == State.OFF || state == State.PAUSED) return;
        if (mc.player == null || mc.level == null || mc.gameMode == null) {
            stop("Verbindung unterbrochen");
            return;
        }
        run(mc);
    }
}
