package com.sessiontoken;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class SessionTokenMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof TitleScreen) {
                Button button = Button.builder(
                    Component.literal("Session Tokens"),
                    btn -> Minecraft.getInstance().setScreen(new TokenListScreen(screen))
                ).bounds(5, scaledHeight / 4, 100, 20).build();

                Screens.getButtons(screen).add(button);
            }
        });
    }
}
