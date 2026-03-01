package com.sessiontoken;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class TokenConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static TokenConfig instance;

    public boolean showTitleButton = true;
    public boolean maskTokens = true;
    public boolean enablePrism = true;
    public boolean enableMultiMC = true;
    public String customAccountsPath = "";

    public static TokenConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static File getConfigFile() {
        return new File(Minecraft.getInstance().gameDirectory, "config/sessiontoken.json");
    }

    public static TokenConfig load() {
        File file = getConfigFile();
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                TokenConfig config = GSON.fromJson(reader, TokenConfig.class);
                if (config != null) {
                    instance = config;
                    return config;
                }
            } catch (Exception ignored) {}
        }
        instance = new TokenConfig();
        return instance;
    }

    public void save() {
        File file = getConfigFile();
        file.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(this, writer);
        } catch (Exception ignored) {}
    }

    public List<LauncherSource> getEnabledLaunchers() {
        List<LauncherSource> launchers = new ArrayList<>();
        if (customAccountsPath != null && !customAccountsPath.isEmpty()) {
            launchers.add(new LauncherSource("Custom", new File(customAccountsPath)));
            return launchers;
        }
        if (enablePrism) {
            File f = findPrismFile();
            if (f != null) launchers.add(new LauncherSource("Prism", f));
        }
        if (enableMultiMC) {
            File f = findMultiMCFile();
            if (f != null) launchers.add(new LauncherSource("MultiMC", f));
        }
        return launchers;
    }

    private File findPrismFile() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null) return new File(appData, "PrismLauncher/accounts.json");
        } else if (os.contains("mac")) {
            String home = System.getProperty("user.home");
            return new File(home, "Library/Application Support/PrismLauncher/accounts.json");
        } else {
            String home = System.getProperty("user.home");
            return new File(home, ".local/share/PrismLauncher/accounts.json");
        }
        return null;
    }

    private File findMultiMCFile() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null) return new File(appData, "MultiMC/accounts.json");
        } else if (os.contains("mac")) {
            String home = System.getProperty("user.home");
            return new File(home, "Library/Application Support/MultiMC/accounts.json");
        } else {
            String home = System.getProperty("user.home");
            return new File(home, ".local/share/MultiMC/accounts.json");
        }
        return null;
    }

    public record LauncherSource(String name, File file) {}
}
