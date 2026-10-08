package de.hugosmp.automation;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import de.hugosmp.automation.gui.MainScreen;

public class HugoAutomationModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new MainScreen(parent);
    }
}
