package com.sessiontoken;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class SessionTokenMod implements ClientModInitializer {
    private static KeyMapping openTokensKey;

    @Override
    public void onInitializeClient() {
        TokenConfig.load();

        // Register keybind (K key) under Misc category
        openTokensKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.sessiontoken.open",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            KeyMapping.Category.MISC
        ));

        // Add title screen button
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof TitleScreen && TokenConfig.get().showTitleButton) {
                Button button = Button.builder(
                    Component.literal("Session Tokens"),
                    btn -> Minecraft.getInstance().setScreen(new TokenListScreen(screen))
                ).bounds(5, scaledHeight / 4, 100, 20).build();

                Screens.getButtons(screen).add(button);
            }
        });

        // Handle keybind
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openTokensKey.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new TokenListScreen(null));
                }
            }
        });
    }
}
