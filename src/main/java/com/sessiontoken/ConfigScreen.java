package com.sessiontoken;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private final TokenConfig config;
    private EditBox customPathField;

    public ConfigScreen(Screen parent) {
        super(Component.literal("Token Finder Settings"));
        this.parent = parent;
        this.config = TokenConfig.get();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = 50;
        int btnWidth = 200;

        // Show title button toggle
        this.addRenderableWidget(Button.builder(
            Component.literal("Title Screen Button: " + (config.showTitleButton ? "ON" : "OFF")),
            btn -> {
                config.showTitleButton = !config.showTitleButton;
                btn.setMessage(Component.literal("Title Screen Button: " + (config.showTitleButton ? "ON" : "OFF")));
            }
        ).bounds(centerX - btnWidth / 2, y, btnWidth, 20).build());

        y += 26;

        // Mask tokens toggle
        this.addRenderableWidget(Button.builder(
            Component.literal("Mask Tokens: " + (config.maskTokens ? "ON" : "OFF")),
            btn -> {
                config.maskTokens = !config.maskTokens;
                btn.setMessage(Component.literal("Mask Tokens: " + (config.maskTokens ? "ON" : "OFF")));
            }
        ).bounds(centerX - btnWidth / 2, y, btnWidth, 20).build());

        y += 26;

        // Prism Launcher toggle
        this.addRenderableWidget(Button.builder(
            Component.literal("Prism Launcher: " + (config.enablePrism ? "ON" : "OFF")),
            btn -> {
                config.enablePrism = !config.enablePrism;
                btn.setMessage(Component.literal("Prism Launcher: " + (config.enablePrism ? "ON" : "OFF")));
            }
        ).bounds(centerX - btnWidth / 2, y, btnWidth, 20).build());

        y += 26;

        // MultiMC toggle
        this.addRenderableWidget(Button.builder(
            Component.literal("MultiMC: " + (config.enableMultiMC ? "ON" : "OFF")),
            btn -> {
                config.enableMultiMC = !config.enableMultiMC;
                btn.setMessage(Component.literal("MultiMC: " + (config.enableMultiMC ? "ON" : "OFF")));
            }
        ).bounds(centerX - btnWidth / 2, y, btnWidth, 20).build());

        y += 32;

        // Custom path field
        customPathField = new EditBox(this.font, centerX - btnWidth / 2, y, btnWidth, 20, Component.literal("Custom Path"));
        customPathField.setMaxLength(512);
        customPathField.setValue(config.customAccountsPath != null ? config.customAccountsPath : "");
        customPathField.setHint(Component.literal("Custom accounts.json path (optional)"));
        this.addRenderableWidget(customPathField);

        y += 36;

        // Save button
        this.addRenderableWidget(Button.builder(
            Component.literal("Save & Back"),
            btn -> {
                config.customAccountsPath = customPathField.getValue();
                config.save();
                Minecraft.getInstance().setScreen(parent);
            }
        ).bounds(centerX - btnWidth / 2, y, btnWidth, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, this.width, this.height, 0xFF101010);
        super.render(graphics, mouseX, mouseY, delta);

        // Title
        String title = "Token Finder Settings";
        graphics.drawString(this.font, title, this.width / 2 - this.font.width(title) / 2, 20, 0xFFFFFFFF);

        // Label for custom path
        int centerX = this.width / 2;
        graphics.drawString(this.font, "Custom accounts.json path:", centerX - 100, customPathField.getY() - 12, 0xFF888888);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
