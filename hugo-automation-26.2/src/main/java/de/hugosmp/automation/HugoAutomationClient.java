package de.hugosmp.automation;

import com.mojang.blaze3d.platform.InputConstants;
import de.hugosmp.automation.config.MacroConfig;
import de.hugosmp.automation.core.GuiDetector;
import de.hugosmp.automation.core.MacroManager;
import de.hugosmp.automation.gui.MainScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class HugoAutomationClient implements ClientModInitializer {

    public static KeyMapping sellKey;
    public static KeyMapping spawnerKey;
    public static KeyMapping menuKey;

    @Override
    public void onInitializeClient() {
        MacroConfig.load();
        GuiDetector.init();

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("hugoautomation", "main"));
        sellKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.hugoautomation.sell", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, category));
        spawnerKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.hugoautomation.spawner", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, category));
        menuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.hugoautomation.menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (sellKey.consumeClick()) {
                MacroManager.toggleSell();
            }
            while (spawnerKey.consumeClick()) {
                MacroManager.toggleSpawner();
            }
            while (menuKey.consumeClick()) {
                if (client.player != null) {
                    openScreen(client, new MainScreen(null));
                }
            }
            MacroManager.tick(client);
        });
    }

    /** Falls setScreen in 26.2 anders heisst, nur diese Zeile anpassen. */
    public static void openScreen(Minecraft mc, Screen screen) {
        mc.gui.setScreen(screen);
    }
}
