package com.sessiontoken;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class TokenListScreen extends Screen {
    private final Screen parent;
    private final List<AccountEntry> accounts = new ArrayList<>();
    private int scrollOffset = 0;
    private static final int ROW_HEIGHT = 24;
    private static final int MAX_VISIBLE = 10;
    private static final int START_Y = 40;

    public TokenListScreen(Screen parent) {
        super(Component.literal("Session Tokens"));
        this.parent = parent;
        loadAccounts();
    }

    private int getVisibleRows() {
        return Math.min(MAX_VISIBLE, Math.max(1, (this.height - 80) / ROW_HEIGHT));
    }

    private void loadAccounts() {
        String appData = System.getenv("APPDATA");
        if (appData == null) return;

        File file = new File(appData, "PrismLauncher/accounts.json");
        if (!file.exists()) return;

        try (FileReader reader = new FileReader(file)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray arr = root.getAsJsonArray("accounts");
            if (arr == null) return;

            for (JsonElement el : arr) {
                JsonObject acc = el.getAsJsonObject();
                JsonObject profile = acc.getAsJsonObject("profile");
                JsonObject ygg = acc.getAsJsonObject("ygg");

                String name = "Unknown";
                if (profile != null && profile.has("name")) {
                    name = profile.get("name").getAsString();
                }

                String token = null;
                if (ygg != null && ygg.has("token")) {
                    token = ygg.get("token").getAsString();
                }

                accounts.add(new AccountEntry(name, token));
            }
        } catch (Exception e) {
            accounts.clear();
        }
    }

    @Override
    protected void init() {
        scrollOffset = Math.min(scrollOffset, Math.max(0, accounts.size() - getVisibleRows()));
        rebuildList();
    }

    private void rebuildList() {
        this.clearWidgets();

        int visibleRows = getVisibleRows();
        int copyBtnWidth = 50;
        int copyBtnX = this.width - copyBtnWidth - 10;

        for (int i = 0; i < visibleRows && (i + scrollOffset) < accounts.size(); i++) {
            int index = i + scrollOffset;
            AccountEntry entry = accounts.get(index);
            int y = START_Y + i * ROW_HEIGHT;

            if (entry.token != null) {
                this.addRenderableWidget(Button.builder(
                    Component.literal("Copy"),
                    btn -> {
                        Minecraft.getInstance().keyboardHandler.setClipboard(entry.token);
                        btn.setMessage(Component.literal("\u00a7aCopied!"));
                    }
                ).bounds(copyBtnX, y, copyBtnWidth, 20).build());
            }
        }

        // Back button at bottom center
        this.addRenderableWidget(Button.builder(
            Component.literal("Back"),
            btn -> Minecraft.getInstance().setScreen(parent)
        ).bounds(this.width / 2 - 50, this.height - 28, 100, 20).build());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, accounts.size() - getVisibleRows());
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) verticalAmount));
        rebuildList();
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Solid dark background
        graphics.fill(0, 0, this.width, this.height, 0xFF101010);

        // Render widgets (buttons)
        super.render(graphics, mouseX, mouseY, delta);

        int visibleRows = getVisibleRows();
        int textX = 10;

        // Title — full alpha white
        graphics.drawString(this.font, "Session Tokens", this.width / 2 - this.font.width("Session Tokens") / 2, 10, 0xFFFFFFFF);

        // Subtitle
        if (!accounts.isEmpty()) {
            String sub = accounts.size() + " accounts  |  Scroll to see more";
            graphics.drawString(this.font, sub, this.width / 2 - this.font.width(sub) / 2, 24, 0xFF888888);
        }

        if (accounts.isEmpty()) {
            String msg = "No accounts found in PrismLauncher";
            graphics.drawString(this.font, msg, this.width / 2 - this.font.width(msg) / 2, this.height / 2, 0xFFFF5555);
            return;
        }

        // Account rows
        for (int i = 0; i < visibleRows && (i + scrollOffset) < accounts.size(); i++) {
            int index = i + scrollOffset;
            AccountEntry entry = accounts.get(index);
            int y = START_Y + i * ROW_HEIGHT;

            // Row background (alternating)
            int rowColor = (index % 2 == 0) ? 0x30FFFFFF : 0x15FFFFFF;
            graphics.fill(5, y - 2, this.width - 5, y + 22, rowColor);

            // Account number and name — full alpha colors
            String label = (index + 1) + ". " + entry.name;
            int textColor = entry.token != null ? 0xFF55FFFF : 0xFFFF5555;
            graphics.drawString(this.font, label, textX, y + 6, textColor);
        }

        // Scroll bar
        if (accounts.size() > visibleRows) {
            int totalHeight = visibleRows * ROW_HEIGHT;
            int barHeight = Math.max(10, totalHeight * visibleRows / accounts.size());
            int maxScroll = accounts.size() - visibleRows;
            int barY = START_Y + (totalHeight - barHeight) * scrollOffset / Math.max(1, maxScroll);
            graphics.fill(this.width - 4, START_Y, this.width - 1, START_Y + totalHeight, 0x40FFFFFF);
            graphics.fill(this.width - 4, barY, this.width - 1, barY + barHeight, 0xFFAAAAAA);
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    private record AccountEntry(String name, String token) {}
}
