package de.hugosmp.automation.macros;

/**
 * Platzhalter fuer die /order-Automation.
 * Fuer die profitabelste Order braucht es Screenshots des /order-Menues und des Lieferfensters;
 * bis dahin wird nichts geraten. ORDER-Items pausieren das Macro (oder fallen auf SELL zurueck,
 * wenn "ORDER -> SELL Fallback" aktiv ist).
 */
public final class OrderMacro {

    private OrderMacro() {
    }

    public static boolean implemented() {
        return false;
    }
}
