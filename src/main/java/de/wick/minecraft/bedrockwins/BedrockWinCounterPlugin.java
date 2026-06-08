package de.wick.minecraft.bedrockwins;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

public final class BedrockWinCounterPlugin extends JavaPlugin implements Listener {

    public enum DisplayMode {
        NONE,
        SIDEBAR,
        BOSSBAR
    }

    public enum MenuValueType {
        TARGET("target", 10, 0),
        CHANGE("change", 0, Integer.MIN_VALUE),
        ADD("add", 1, 1),
        REMOVE("remove", 1, 1),
        SET("set", 0, Integer.MIN_VALUE);

        private final String key;
        private final int defaultValue;
        private final int minimumValue;

        MenuValueType(String key, int defaultValue, int minimumValue) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.minimumValue = minimumValue;
        }

        public String getKey() {
            return key;
        }

        public int getDefaultValue() {
            return defaultValue;
        }

        public int clamp(int value) {
            return Math.max(minimumValue, value);
        }

        public static MenuValueType fromKey(String value) {
            if (value == null) {
                return null;
            }

            for (MenuValueType type : values()) {
                if (type.key.equalsIgnoreCase(value)) {
                    return type;
                }
            }

            return null;
        }
    }

    private final Set<String> winMarkers = new HashSet<>();
    private final Map<UUID, EnumMap<MenuValueType, Integer>> menuValues = new HashMap<>();

    private File dataFile;
    private File langFile;
    private File menuFile;
    private File latestLogFile;
    private FileConfiguration dataConfig;
    private FileConfiguration langConfig;
    private FileConfiguration menuConfig;
    private int wins;
    private Integer targetWins;
    private long latestLogPosition;
    private int logWatcherTaskId = -1;
    private Objective objective;
    private DisplaySlot scoreboardDisplaySlot;
    private String scoreboardEntryName;
    private BossBar bossBar;
    private DisplayMode activeDisplayMode = DisplayMode.NONE;
    private DisplayMode configuredDisplayMode = DisplayMode.BOSSBAR;
    private DisplayMode lastNonNoneDisplayMode = DisplayMode.BOSSBAR;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSupportFiles();
        loadDataFile();
        reloadPluginState();

        BedrockWinsCommand command = new BedrockWinsCommand(this);
        BedrockMenuGui menuGui = new BedrockMenuGui(this);
        if (getCommand("bedrockwins") != null) {
            getCommand("bedrockwins").setExecutor(command);
            getCommand("bedrockwins").setTabCompleter(command);
        }
        if (getCommand("bedrockmenu") != null) {
            getCommand("bedrockmenu").setExecutor(new BedrockMenuCommand(this, menuGui));
        }

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(menuGui, this);
        getLogger().info("Listening for win markers: " + String.join(", ", winMarkers));
        startLatestLogWatcher();
    }

    @Override
    public void onDisable() {
        stopLatestLogWatcher();
        disableAllDisplays();
        saveDataFile();
    }

    public void reloadPluginState() {
        reloadConfig();
        loadSupportFiles();
        loadMarkers();
        configuredDisplayMode = parseDisplayMode(getConfig().getString("display.mode", "BOSSBAR"), DisplayMode.BOSSBAR);
        activeDisplayMode = parseDisplayMode(dataConfig.getString("display.mode", configuredDisplayMode.name()), configuredDisplayMode);
        lastNonNoneDisplayMode = parseDisplayMode(
                dataConfig.getString("display.last-non-none-mode", configuredDisplayMode.name()),
                configuredDisplayMode
        );
        setupDisplays();
        persistDisplayState();
    }

    public int getWins() {
        return wins;
    }

    public Integer getTargetWins() {
        return targetWins;
    }

    public void addWins(int amount) {
        setWins(this.wins + amount);
    }

    public void setWins(int wins) {
        this.wins = wins;
        dataConfig.set("wins", this.wins);
        saveDataFile();
        updateDisplays();
    }

    public void setTargetWins(Integer targetWins) {
        if (targetWins != null && targetWins < 0) {
            targetWins = 0;
        }

        this.targetWins = targetWins;
        dataConfig.set("target-wins", this.targetWins);
        saveDataFile();
        updateDisplays();
    }

    public void incrementFromMarker(String marker) {
        setWins(this.wins + 1);
        getLogger().info("Counted Bedrock win from marker: " + marker + " (total: " + this.wins + ")");
    }

    public String format(String message) {
        return ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.prefix", ""))
                + ChatColor.translateAlternateColorCodes('&', message);
    }

    public String localize(String path, String fallback, String... replacements) {
        String raw = langConfig == null ? fallback : langConfig.getString(path, fallback);
        return applyReplacements(colorize(raw), replacements);
    }

    public List<String> localizeList(String path, List<String> fallback, String... replacements) {
        List<String> rawLines = langConfig == null ? fallback : langConfig.getStringList(path);
        if (rawLines == null || rawLines.isEmpty()) {
            rawLines = fallback;
        }

        return rawLines.stream()
                .map(this::colorize)
                .map(line -> applyReplacements(line, replacements))
                .toList();
    }

    public void sendLocalized(CommandSender sender, String path, String fallback, String... replacements) {
        sender.sendMessage(colorize(getConfig().getString("messages.prefix", "")) + localize(path, fallback, replacements));
    }

    public Material getMenuMaterial(String path, Material fallback) {
        String raw = menuConfig == null ? null : menuConfig.getString(path);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }

        try {
            return Material.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            getLogger().warning("Unknown menu material '" + raw + "' at path '" + path + "'. Using fallback " + fallback + ".");
            return fallback;
        }
    }

    public int getMenuAmount(String path, int fallback) {
        int value = menuConfig == null ? fallback : menuConfig.getInt(path, fallback);
        if (value < 1) {
            return 1;
        }

        return Math.min(value, 64);
    }

    public int getMenuSize(String path, int fallback) {
        int value = menuConfig == null ? fallback : menuConfig.getInt(path, fallback);
        if (value < 9 || value > 54 || value % 9 != 0) {
            getLogger().warning("Invalid menu size at path '" + path + "': " + value + ". Using fallback " + fallback + ".");
            return fallback;
        }

        return value;
    }

    public int getMenuSlot(String path, int fallback, int inventorySize) {
        int value = menuConfig == null ? fallback : menuConfig.getInt(path, fallback);
        if (value < 0 || value >= inventorySize) {
            getLogger().warning(
                    "Invalid menu slot at path '" + path + "': " + value + ". Using fallback " + fallback + "."
            );
            return fallback;
        }

        return value;
    }

    public void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(format(message));
    }

    public DisplayMode getActiveDisplayMode() {
        return activeDisplayMode;
    }

    public void setDisplayMode(DisplayMode mode) {
        DisplayMode targetMode = mode == null ? DisplayMode.NONE : mode;
        if (targetMode != DisplayMode.NONE) {
            lastNonNoneDisplayMode = targetMode;
        }

        activeDisplayMode = targetMode;
        applyDisplayMode();
        persistDisplayState();
    }

    public void toggleDisplay() {
        if (activeDisplayMode == DisplayMode.NONE) {
            setDisplayMode(lastNonNoneDisplayMode == DisplayMode.NONE ? configuredDisplayMode : lastNonNoneDisplayMode);
            return;
        }

        setDisplayMode(DisplayMode.NONE);
    }

    public String getDisplayModeLabel() {
        return activeDisplayMode.name().toLowerCase(Locale.ROOT);
    }

    public String getWinsDisplay() {
        if (targetWins == null) {
            return Integer.toString(wins);
        }

        return wins + "/" + targetWins;
    }

    public int initializeMenuValue(Player player, MenuValueType type) {
        return setMenuValue(player, type, type.getDefaultValue());
    }

    public int adjustMenuValue(Player player, MenuValueType type, int delta) {
        int currentValue = getMenuValue(player, type);
        return setMenuValue(player, type, currentValue + delta);
    }

    public int resetMenuValue(Player player, MenuValueType type) {
        return setMenuValue(player, type, type.getDefaultValue());
    }

    public int getMenuValue(Player player, MenuValueType type) {
        EnumMap<MenuValueType, Integer> valuesByType = menuValues.get(player.getUniqueId());
        if (valuesByType == null) {
            return type.getDefaultValue();
        }

        return valuesByType.getOrDefault(type, type.getDefaultValue());
    }

    public int setMenuValue(Player player, MenuValueType type, int value) {
        EnumMap<MenuValueType, Integer> valuesByType = menuValues.computeIfAbsent(
                player.getUniqueId(),
                ignored -> new EnumMap<>(MenuValueType.class)
        );
        int clampedValue = type.clamp(value);
        valuesByType.put(type, clampedValue);
        return clampedValue;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (bossBar != null && activeDisplayMode == DisplayMode.BOSSBAR && event.getPlayer().hasPermission("bedrockwins.view")) {
            bossBar.addPlayer(event.getPlayer());
        }
    }

    private void loadMarkers() {
        winMarkers.clear();
        List<String> configuredMarkers = getConfig().getStringList("win-markers");
        if (configuredMarkers.isEmpty()) {
            configuredMarkers = List.of("bedrock_win");
        }

        for (String marker : configuredMarkers) {
            if (marker == null) {
                continue;
            }

            String normalized = marker.trim().toLowerCase(Locale.ROOT);
            if (!normalized.isEmpty()) {
                winMarkers.add(normalized);
            }
        }
    }

    private void loadDataFile() {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            throw new IllegalStateException("Could not create plugin data folder");
        }

        dataFile = new File(getDataFolder(), "data.yml");
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        wins = dataConfig.getInt("wins", 0);
        targetWins = dataConfig.isSet("target-wins") ? dataConfig.getInt("target-wins") : null;

        if (!dataFile.exists()) {
            dataConfig.set("wins", wins);
            dataConfig.set("target-wins", targetWins);
            saveDataFile();
        }
    }

    private void saveDataFile() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException exception) {
            getLogger().severe("Could not save data.yml: " + exception.getMessage());
        }
    }

    private void loadSupportFiles() {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            throw new IllegalStateException("Could not create plugin data folder");
        }

        langFile = new File(getDataFolder(), "lang.yml");
        menuFile = new File(getDataFolder(), "menu.yml");
        saveBundledFileIfMissing("lang.yml", langFile);
        saveBundledFileIfMissing("menu.yml", menuFile);
        langConfig = YamlConfiguration.loadConfiguration(langFile);
        menuConfig = YamlConfiguration.loadConfiguration(menuFile);
        mergeMissingDefaults(langConfig, langFile, "lang.yml");
        mergeMissingDefaults(menuConfig, menuFile, "menu.yml");
    }

    private void saveBundledFileIfMissing(String resourceName, File targetFile) {
        if (!targetFile.exists()) {
            saveResource(resourceName, false);
        }
    }

    private void mergeMissingDefaults(FileConfiguration targetConfig, File targetFile, String resourceName) {
        try (InputStream resourceStream = getResource(resourceName)) {
            if (resourceStream == null) {
                return;
            }

            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(resourceStream, StandardCharsets.UTF_8)
            );

            boolean changed = false;
            for (String key : defaults.getKeys(true)) {
                if (defaults.isConfigurationSection(key) || targetConfig.isSet(key)) {
                    continue;
                }

                targetConfig.set(key, defaults.get(key));
                changed = true;
            }

            if (changed) {
                targetConfig.save(targetFile);
            }
        } catch (IOException exception) {
            getLogger().warning("Could not merge defaults into " + resourceName + ": " + exception.getMessage());
        }
    }

    private void startLatestLogWatcher() {
        stopLatestLogWatcher();

        File pluginDirectory = getDataFolder().getAbsoluteFile().getParentFile();
        File serverDirectory = pluginDirectory == null ? null : pluginDirectory.getParentFile();
        latestLogFile = serverDirectory == null ? null : new File(serverDirectory, "logs/latest.log");

        if (latestLogFile == null) {
            getLogger().warning("Could not resolve latest.log path.");
            return;
        }

        latestLogPosition = latestLogFile.exists() ? latestLogFile.length() : 0L;
        logWatcherTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(
                this,
                this::pollLatestLog,
                20L,
                20L
        ).getTaskId();

        getLogger().info("Watching latest.log for win markers: " + latestLogFile.getAbsolutePath());
    }

    private void stopLatestLogWatcher() {
        if (logWatcherTaskId != -1) {
            Bukkit.getScheduler().cancelTask(logWatcherTaskId);
            logWatcherTaskId = -1;
        }
    }

    private void pollLatestLog() {
        if (latestLogFile == null || !latestLogFile.exists()) {
            return;
        }

        long fileLength = latestLogFile.length();
        if (fileLength < latestLogPosition) {
            latestLogPosition = 0L;
        }

        if (fileLength == latestLogPosition) {
            return;
        }

        try (RandomAccessFile logReader = new RandomAccessFile(latestLogFile, "r")) {
            logReader.seek(latestLogPosition);

            String line;
            while ((line = logReader.readLine()) != null) {
                handleLatestLogLine(line);
            }

            latestLogPosition = logReader.getFilePointer();
        } catch (IOException exception) {
            getLogger().warning("Could not read latest.log: " + exception.getMessage());
        }
    }

    private void handleLatestLogLine(String rawLine) {
        String message = extractLogMessage(rawLine);
        if (!isWinMarker(message)) {
            return;
        }

        Bukkit.getScheduler().runTask(this, () -> incrementFromMarker(message));
    }

    private String extractLogMessage(String rawLine) {
        int separatorIndex = rawLine.indexOf("]: ");
        if (separatorIndex == -1) {
            return rawLine.trim();
        }

        return rawLine.substring(separatorIndex + 3).trim();
    }

    private void persistDisplayState() {
        dataConfig.set("display.mode", activeDisplayMode.name());
        dataConfig.set("display.last-non-none-mode", lastNonNoneDisplayMode.name());
        saveDataFile();
    }

    private void setupDisplays() {
        setupScoreboard();
        setupBossBar();
        applyDisplayMode();
        updateDisplays();
    }

    private void setupScoreboard() {
        objective = null;
        scoreboardDisplaySlot = null;
        scoreboardEntryName = colorize(getConfig().getString("display.scoreboard.entry-name", "&eWins"));

        disableSidebarDisplay();

        if (Bukkit.getScoreboardManager() == null) {
            getLogger().warning("Scoreboard manager is not available yet.");
            return;
        }

        String objectiveName = getConfig().getString("display.scoreboard.objective-name", "bedrock_wins");
        String displayName = colorize(getConfig().getString("display.scoreboard.display-name", "&6Bedrock Wins"));
        String displaySlotName = getConfig().getString("display.scoreboard.display-slot", "SIDEBAR");

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Objective existingObjective = scoreboard.getObjective(objectiveName);

        if (existingObjective == null) {
            objective = scoreboard.registerNewObjective(objectiveName, Criteria.DUMMY, displayName);
        } else {
            objective = existingObjective;
            objective.setDisplayName(displayName);
        }

        if (displaySlotName != null && !displaySlotName.equalsIgnoreCase("NONE")) {
            try {
                scoreboardDisplaySlot = DisplaySlot.valueOf(displaySlotName.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                getLogger().warning("Unknown display slot '" + displaySlotName + "'. Scoreboard will stay hidden.");
            }
        }
    }

    private void setupBossBar() {
        disableBossBarDisplay();

        String title = buildBossBarTitle();
        BarColor color = getBossBarColorForWins();
        BarStyle style = parseBarStyle(getConfig().getString("display.bossbar.style", "SOLID"));

        bossBar = Bukkit.createBossBar(title, color, style);
        bossBar.setVisible(false);
        syncBossBarPlayers();
    }

    private void applyDisplayMode() {
        disableAllDisplays();

        if (activeDisplayMode == DisplayMode.SIDEBAR) {
            enableSidebarDisplay();
            return;
        }

        if (activeDisplayMode == DisplayMode.BOSSBAR) {
            enableBossBarDisplay();
        }
    }

    private void updateDisplays() {
        updateScoreboard();
        updateBossBar();
    }

    private void updateScoreboard() {
        if (objective == null || activeDisplayMode != DisplayMode.SIDEBAR) {
            return;
        }

        objective.setDisplaySlot(scoreboardDisplaySlot);
        objective.getScore(scoreboardEntryName).setScore(wins);
    }

    private void updateBossBar() {
        if (bossBar == null) {
            return;
        }

        bossBar.setTitle(buildBossBarTitle());
        bossBar.setColor(getBossBarColorForWins());
        bossBar.setProgress(getBossBarProgress());
        syncBossBarPlayers();
    }

    private void enableSidebarDisplay() {
        if (objective == null) {
            return;
        }

        objective.setDisplaySlot(scoreboardDisplaySlot);
        updateScoreboard();
    }

    private void disableSidebarDisplay() {
        if (objective != null) {
            objective.setDisplaySlot(null);
        }
    }

    private void enableBossBarDisplay() {
        if (bossBar == null) {
            return;
        }

        syncBossBarPlayers();
        bossBar.setVisible(true);
        updateBossBar();
    }

    private void disableBossBarDisplay() {
        if (bossBar != null) {
            bossBar.setVisible(false);
            bossBar.removeAll();
            bossBar = null;
        }
    }

    private void disableAllDisplays() {
        disableSidebarDisplay();
        if (bossBar != null) {
            bossBar.setVisible(false);
        }
    }

    public String colorize(String value) {
        return ChatColor.translateAlternateColorCodes('&', value == null ? "" : value);
    }

    private String applyReplacements(String value, String... replacements) {
        String resolved = value == null ? "" : value;
        for (int index = 0; index + 1 < replacements.length; index += 2) {
            resolved = resolved.replace(replacements[index], replacements[index + 1]);
        }
        return resolved;
    }

    private String formatWins(String value) {
        return colorize((value == null ? "" : value).replace("%wins%", Integer.toString(wins)));
    }

    private String buildBossBarTitle() {
        String label = getConfig().getString("display.bossbar.title-label", "Wins");
        String color = getBossBarTextColor();
        return colorize(color + label + ": " + getWinsDisplay());
    }

    private boolean isWinMarker(String message) {
        return winMarkers.contains(message.trim().toLowerCase(Locale.ROOT));
    }

    private DisplayMode parseDisplayMode(String value, DisplayMode fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        try {
            return DisplayMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    private BarColor parseBarColor(String value) {
        if (value == null || value.isBlank()) {
            return BarColor.YELLOW;
        }

        try {
            return BarColor.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return BarColor.YELLOW;
        }
    }

    private BarStyle parseBarStyle(String value) {
        if (value == null || value.isBlank()) {
            return BarStyle.SOLID;
        }

        try {
            return BarStyle.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return BarStyle.SOLID;
        }
    }

    private String getBossBarTextColor() {
        if (wins < 0) {
            return getConfig().getString("display.bossbar.text-colors.negative", "&c");
        }

        if (wins == 0) {
            return getConfig().getString("display.bossbar.text-colors.zero", "&6");
        }

        return getConfig().getString("display.bossbar.text-colors.positive", "&a");
    }

    private BarColor getBossBarColorForWins() {
        if (wins < 0) {
            return BarColor.RED;
        }

        if (wins == 0) {
            return parseBarColor(getConfig().getString("display.bossbar.color", "YELLOW"));
        }

        return BarColor.GREEN;
    }

    private double getBossBarProgress() {
        if (!getConfig().getBoolean("display.bossbar.progress.enabled", true)) {
            return 1.0D;
        }

        if (targetWins == null || targetWins <= 0) {
            return 1.0D;
        }

        double progress = (double) wins / (double) targetWins;
        if (progress < 0.0D) {
            return 0.0D;
        }

        if (progress > 1.0D) {
            return 1.0D;
        }

        return progress;
    }

    private void syncBossBarPlayers() {
        if (bossBar == null) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("bedrockwins.view")) {
                bossBar.addPlayer(player);
            } else {
                bossBar.removePlayer(player);
            }
        }
    }
}
