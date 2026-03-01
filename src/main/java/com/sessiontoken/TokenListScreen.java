package com.sessiontoken;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.GsonBuilder;
import com.google.gson.Gson;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TokenListScreen extends Screen {
    private final Screen parent;
    private final List<AccountEntry> allAccounts = new ArrayList<>();
    private final List<AccountEntry> filteredAccounts = new ArrayList<>();
    private final Map<Integer, Boolean> revealedTokens = new HashMap<>();
    private int scrollOffset = 0;
    private static final int ROW_HEIGHT = 24;
    private static final int MAX_VISIBLE = 10;
    private static final int START_Y = 60;
    private EditBox searchField;
    private long lastRefreshTime = 0;
    private final Map<File, Long> fileTimestamps = new HashMap<>();
    private static final long REFRESH_INTERVAL = 5000;

    public TokenListScreen(Screen parent) {
        super(Component.literal("Session Tokens"));
        this.parent = parent;
        loadAccounts();
    }

    private int getVisibleRows() {
        return Math.min(MAX_VISIBLE, Math.max(1, (this.height - 100) / ROW_HEIGHT));
    }

    private void loadAccounts() {
        allAccounts.clear();
        List<TokenConfig.LauncherSource> launchers = TokenConfig.get().getEnabledLaunchers();

        for (TokenConfig.LauncherSource launcher : launchers) {
            File file = launcher.file();
            if (!file.exists()) continue;

            fileTimestamps.put(file, file.lastModified());

            try (FileReader reader = new FileReader(file)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                JsonArray arr = root.getAsJsonArray("accounts");
                if (arr == null) continue;

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

                    TokenStatus status = TokenStatus.NO_TOKEN;
                    if (token != null && !token.isEmpty()) {
                        status = token.length() > 20 ? TokenStatus.VALID : TokenStatus.SHORT;
                    }

                    allAccounts.add(new AccountEntry(name, token, launcher.name(), status));
                }
            } catch (Exception ignored) {}
        }

        applyFilter();
        lastRefreshTime = System.currentTimeMillis();
    }

    private void checkForRefresh() {
        if (System.currentTimeMillis() - lastRefreshTime < REFRESH_INTERVAL) return;

        boolean changed = false;
        for (Map.Entry<File, Long> entry : fileTimestamps.entrySet()) {
            if (entry.getKey().exists() && entry.getKey().lastModified() != entry.getValue()) {
                changed = true;
                break;
            }
        }

        if (changed) {
            loadAccounts();
            rebuildList();
        }
        lastRefreshTime = System.currentTimeMillis();
    }

    private void applyFilter() {
        filteredAccounts.clear();
        String query = (searchField != null) ? searchField.getValue().toLowerCase() : "";

        for (AccountEntry entry : allAccounts) {
            if (query.isEmpty() || entry.name.toLowerCase().contains(query) || entry.launcher.toLowerCase().contains(query)) {
                filteredAccounts.add(entry);
            }
        }

        scrollOffset = Math.min(scrollOffset, Math.max(0, filteredAccounts.size() - getVisibleRows()));
    }

    @Override
    protected void init() {
        scrollOffset = Math.min(scrollOffset, Math.max(0, filteredAccounts.size() - getVisibleRows()));

        // Search field
        int searchWidth = Math.min(200, this.width - 20);
        searchField = new EditBox(this.font, this.width / 2 - searchWidth / 2, 36, searchWidth, 16, Component.literal("Search"));
        searchField.setMaxLength(100);
        searchField.setHint(Component.literal("Search accounts..."));
        searchField.setResponder(text -> {
            applyFilter();
            scrollOffset = 0;
            rebuildList();
        });
        this.addRenderableWidget(searchField);

        rebuildList();
    }

    private void rebuildList() {
        // Remove all widgets except search field, then re-add
        this.clearWidgets();
        this.addRenderableWidget(searchField);

        int visibleRows = getVisibleRows();
        int copyBtnWidth = 45;
        int revealBtnWidth = 45;
        int btnGap = 4;

        for (int i = 0; i < visibleRows && (i + scrollOffset) < filteredAccounts.size(); i++) {
            int index = i + scrollOffset;
            AccountEntry entry = filteredAccounts.get(index);
            int y = START_Y + i * ROW_HEIGHT;
            int globalIndex = allAccounts.indexOf(entry);

            if (entry.token != null) {
                boolean revealed = revealedTokens.getOrDefault(globalIndex, !TokenConfig.get().maskTokens);

                // Reveal/Hide button
                int revealBtnX = this.width - copyBtnWidth - btnGap - revealBtnWidth - 10;
                this.addRenderableWidget(Button.builder(
                    Component.literal(revealed ? "Hide" : "Show"),
                    btn -> {
                        boolean current = revealedTokens.getOrDefault(globalIndex, !TokenConfig.get().maskTokens);
                        revealedTokens.put(globalIndex, !current);
                        rebuildList();
                    }
                ).bounds(revealBtnX, y, revealBtnWidth, 20).build());

                // Copy button
                int copyBtnX = this.width - copyBtnWidth - 10;
                this.addRenderableWidget(Button.builder(
                    Component.literal("Copy"),
                    btn -> {
                        Minecraft.getInstance().keyboardHandler.setClipboard(entry.token);
                        SystemToast.add(
                            Minecraft.getInstance().getToastManager(),
                            SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                            Component.literal("Token Copied"),
                            Component.literal(entry.name + "'s token copied to clipboard")
                        );
                    }
                ).bounds(copyBtnX, y, copyBtnWidth, 20).build());
            }
        }

        // Bottom buttons
        int bottomY = this.height - 28;
        int bottomBtnWidth = 80;
        int totalBottomWidth = bottomBtnWidth * 3 + 8;
        int startX = this.width / 2 - totalBottomWidth / 2;

        // Settings button
        this.addRenderableWidget(Button.builder(
            Component.literal("Settings"),
            btn -> Minecraft.getInstance().setScreen(new ConfigScreen(this))
        ).bounds(startX, bottomY, bottomBtnWidth, 20).build());

        // Export button
        this.addRenderableWidget(Button.builder(
            Component.literal("Export"),
            btn -> exportTokens()
        ).bounds(startX + bottomBtnWidth + 4, bottomY, bottomBtnWidth, 20).build());

        // Back button
        this.addRenderableWidget(Button.builder(
            Component.literal("Back"),
            btn -> Minecraft.getInstance().setScreen(parent)
        ).bounds(startX + (bottomBtnWidth + 4) * 2, bottomY, bottomBtnWidth, 20).build());
    }

    private void exportTokens() {
        try {
            List<Map<String, String>> exportData = new ArrayList<>();
            for (AccountEntry entry : allAccounts) {
                if (entry.token != null) {
                    Map<String, String> map = new HashMap<>();
                    map.put("name", entry.name);
                    map.put("token", entry.token);
                    map.put("launcher", entry.launcher);
                    exportData.add(map);
                }
            }
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(exportData);
            String encoded = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));

            File exportFile = new File(Minecraft.getInstance().gameDirectory, "sessiontoken_export.txt");
            try (FileWriter writer = new FileWriter(exportFile)) {
                writer.write(encoded);
            }

            SystemToast.add(
                Minecraft.getInstance().getToastManager(),
                SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                Component.literal("Tokens Exported"),
                Component.literal("Saved to sessiontoken_export.txt")
            );
        } catch (Exception e) {
            SystemToast.add(
                Minecraft.getInstance().getToastManager(),
                SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                Component.literal("Export Failed"),
                Component.literal(e.getMessage())
            );
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, filteredAccounts.size() - getVisibleRows());
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) verticalAmount));
        rebuildList();
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        checkForRefresh();

        // Solid dark background
        graphics.fill(0, 0, this.width, this.height, 0xFF101010);

        // Render widgets (buttons, search field)
        super.render(graphics, mouseX, mouseY, delta);

        int visibleRows = getVisibleRows();
        int textX = 10;

        // Title
        String titleStr = "Session Tokens";
        graphics.drawString(this.font, titleStr, this.width / 2 - this.font.width(titleStr) / 2, 10, 0xFFFFFFFF);

        // Subtitle
        if (!allAccounts.isEmpty()) {
            String sub = filteredAccounts.size() + "/" + allAccounts.size() + " accounts  |  Press K to open  |  Scroll to see more";
            graphics.drawString(this.font, sub, this.width / 2 - this.font.width(sub) / 2, 22, 0xFF888888);
        }

        if (filteredAccounts.isEmpty()) {
            String msg = allAccounts.isEmpty() ? "No accounts found" : "No matches for \"" + searchField.getValue() + "\"";
            graphics.drawString(this.font, msg, this.width / 2 - this.font.width(msg) / 2, this.height / 2, 0xFFFF5555);
            return;
        }

        // Account rows
        for (int i = 0; i < visibleRows && (i + scrollOffset) < filteredAccounts.size(); i++) {
            int index = i + scrollOffset;
            AccountEntry entry = filteredAccounts.get(index);
            int y = START_Y + i * ROW_HEIGHT;
            int globalIndex = allAccounts.indexOf(entry);

            // Row background (alternating)
            int rowColor = (index % 2 == 0) ? 0x30FFFFFF : 0x15FFFFFF;
            graphics.fill(5, y - 2, this.width - 5, y + 22, rowColor);

            // Token status indicator dot
            int dotColor = switch (entry.status) {
                case VALID -> 0xFF55FF55;
                case SHORT -> 0xFFFFFF55;
                case NO_TOKEN -> 0xFFFF5555;
            };
            graphics.fill(textX, y + 7, textX + 6, y + 13, dotColor);

            // Account name with launcher tag
            String label = entry.name;
            int textColor = entry.token != null ? 0xFF55FFFF : 0xFFFF5555;
            graphics.drawString(this.font, label, textX + 10, y + 3, textColor);

            // Launcher tag
            String tag = "[" + entry.launcher + "]";
            graphics.drawString(this.font, tag, textX + 10, y + 13, 0xFF666666);

            // Token preview (masked or revealed)
            if (entry.token != null) {
                boolean revealed = revealedTokens.getOrDefault(globalIndex, !TokenConfig.get().maskTokens);
                String tokenPreview;
                if (revealed) {
                    tokenPreview = entry.token.length() > 24 ? entry.token.substring(0, 24) + "..." : entry.token;
                } else {
                    tokenPreview = "\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022";
                }
                int tokenX = textX + 10 + this.font.width(label) + 8;
                graphics.drawString(this.font, tokenPreview, tokenX, y + 3, 0xFF999999);
            }
        }

        // Scroll bar
        if (filteredAccounts.size() > visibleRows) {
            int totalHeight = visibleRows * ROW_HEIGHT;
            int barHeight = Math.max(10, totalHeight * visibleRows / filteredAccounts.size());
            int maxScroll = filteredAccounts.size() - visibleRows;
            int barY = START_Y + (totalHeight - barHeight) * scrollOffset / Math.max(1, maxScroll);
            graphics.fill(this.width - 4, START_Y, this.width - 1, START_Y + totalHeight, 0x40FFFFFF);
            graphics.fill(this.width - 4, barY, this.width - 1, barY + barHeight, 0xFFAAAAAA);
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    private enum TokenStatus { VALID, SHORT, NO_TOKEN }
    private record AccountEntry(String name, String token, String launcher, TokenStatus status) {}
}
