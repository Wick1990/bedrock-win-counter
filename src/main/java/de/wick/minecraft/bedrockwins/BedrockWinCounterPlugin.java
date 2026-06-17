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

    public enum BoxType {
        BEDROCK(
                "bedrock",
                "Bedrock",
                "Bedrock-Wins",
                "bedrock",
                "bedrockwins",
                "bedrockwins.menu.use",
                "bedrockwins.view",
                "bedrockwins.admin",
                true,
                List.of("bedrock_win")
        ),
        SANDBOX(
                "sandbox",
                "Sandbox",
                "Sandbox-Wins",
                "sandbox",
                "sandboxwins",
                "sandboxwins.menu.use",
                "sandboxwins.view",
                "sandboxwins.admin",
                true,
                List.of("[s2e-sand-box] win-up", "win-up")
        ),
        SHEEP_OUT(
                "sheepout",
                "Sheep Out",
                "Sheep-Out-Wins",
                "sheepout",
                "sheepoutwins",
                "sheepout.menu.use",
                "sheepout.view",
                "sheepout.admin",
                false,
                List.of()
        );

        private final String key;
        private final String displayName;
        private final String winsLabel;
        private final String boxCommand;
        private final String winsCommand;
        private final String menuPermission;
        private final String viewPermission;
        private final String adminPermission;
        private final boolean tracksWins;
        private final List<String> defaultMarkers;

        BoxType(
                String key,
                String displayName,
                String winsLabel,
                String boxCommand,
                String winsCommand,
                String menuPermission,
                String viewPermission,
                String adminPermission,
                boolean tracksWins,
                List<String> defaultMarkers
        ) {
            this.key = key;
            this.displayName = displayName;
            this.winsLabel = winsLabel;
            this.boxCommand = boxCommand;
            this.winsCommand = winsCommand;
            this.menuPermission = menuPermission;
            this.viewPermission = viewPermission;
            this.adminPermission = adminPermission;
            this.tracksWins = tracksWins;
            this.defaultMarkers = defaultMarkers;
        }

        public String getKey() {
            return key;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getWinsLabel() {
            return winsLabel;
        }

        public String getBoxCommand() {
            return boxCommand;
        }

        public String getWinsCommand() {
            return winsCommand;
        }

        public String getMenuPermission() {
            return menuPermission;
        }

        public String getViewPermission() {
            return viewPermission;
        }

        public String getAdminPermission() {
            return adminPermission;
        }

        public boolean tracksWins() {
            return tracksWins;
        }

        public List<String> getDefaultMarkers() {
            return defaultMarkers;
        }

        public static BoxType fromKey(String value) {
            if (value == null) {
                return null;
            }

            for (BoxType boxType : values()) {
                if (boxType.key.equalsIgnoreCase(value)) {
                    return boxType;
                }
            }

            return null;
        }

        public static List<BoxType> trackedBoxes() {
            return List.of(BEDROCK, SANDBOX);
        }
    }

    private static final class CounterState {
        private int wins;
        private Integer targetWins;
    }

    private final EnumMap<BoxType, CounterState> counterStates = new EnumMap<>(BoxType.class);
    private final EnumMap<BoxType, Set<String>> winMarkers = new EnumMap<>(BoxType.class);
    private final Map<UUID, EnumMap<BoxType, EnumMap<MenuValueType, Integer>>> menuValues = new HashMap<>();

    private File dataFile;
    private File langFile;
    private File menuFile;
    private File latestLogFile;
    private FileConfiguration dataConfig;
    private FileConfiguration langConfig;
    private FileConfiguration menuConfig;
    private long latestLogPosition;
    private int logWatcherTaskId = -1;
    private Objective objective;
    private DisplaySlot scoreboardDisplaySlot;
    private String lastScoreboardEntryName;
    private BossBar bossBar;
    private BoxType displayedBox = BoxType.BEDROCK;
    private DisplayMode activeDisplayMode = DisplayMode.NONE;
    private DisplayMode configuredDisplayMode = DisplayMode.BOSSBAR;
    private DisplayMode lastNonNoneDisplayMode = DisplayMode.BOSSBAR;

    public BedrockWinCounterPlugin() {
        for (BoxType boxType : BoxType.trackedBoxes()) {
            counterStates.put(boxType, new CounterState());
            winMarkers.put(boxType, new HashSet<>());
        }
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSupportFiles();
        loadDataFile();
        reloadPluginState();

        BedrockWinsCommand bedrockCommand = new BedrockWinsCommand(this, BoxType.BEDROCK);
        BedrockWinsCommand sandboxCommand = new BedrockWinsCommand(this, BoxType.SANDBOX);
        BedrockMenuGui menuGui = new BedrockMenuGui(this);

        if (getCommand("bedrockwins") != null) {
            getCommand("bedrockwins").setExecutor(bedrockCommand);
            getCommand("bedrockwins").setTabCompleter(bedrockCommand);
        }
        if (getCommand("sandboxwins") != null) {
            getCommand("sandboxwins").setExecutor(sandboxCommand);
            getCommand("sandboxwins").setTabCompleter(sandboxCommand);
        }
        if (getCommand("bedrockmenu") != null) {
            getCommand("bedrockmenu").setExecutor(new BedrockMenuCommand(this, menuGui));
        }

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(menuGui, this);

        for (BoxType boxType : BoxType.trackedBoxes()) {
            getLogger().info(
                    "Listening for " + boxType.getDisplayName() + " win markers: " + String.join(", ", winMarkers.get(boxType))
            );
        }

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
        if (lastNonNoneDisplayMode == DisplayMode.NONE) {
            lastNonNoneDisplayMode = configuredDisplayMode;
        }
        setupDisplays();
        persistDisplayState();
    }

    public int getWins(BoxType boxType) {
        return getCounterState(boxType).wins;
    }

    public Integer getTargetWins(BoxType boxType) {
        return getCounterState(boxType).targetWins;
    }

    public void addWins(BoxType boxType, int amount) {
        setWins(boxType, getWins(boxType) + amount);
    }

    public void setWins(BoxType boxType, int wins) {
        CounterState counterState = getCounterState(boxType);
        counterState.wins = wins;
        saveDataFile();
        updateDisplays();
    }

    public void setTargetWins(BoxType boxType, Integer targetWins) {
        CounterState counterState = getCounterState(boxType);
        if (targetWins != null && targetWins < 0) {
            targetWins = 0;
        }

        counterState.targetWins = targetWins;
        saveDataFile();
        updateDisplays();
    }

    public void incrementFromMarker(BoxType boxType, String marker) {
        addWins(boxType, 1);
        getLogger().info(
                "Counted " + boxType.getDisplayName() + " win from marker: " + marker + " (total: " + getWins(boxType) + ")"
        );
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

    public BoxType getDisplayedBox() {
        return displayedBox;
    }

    public void setDisplayMode(BoxType boxType, DisplayMode mode) {
        BoxType resolvedBox = boxType == null ? BoxType.BEDROCK : boxType;
        DisplayMode targetMode = mode == null ? DisplayMode.NONE : mode;
        displayedBox = resolvedBox;

        if (targetMode != DisplayMode.NONE) {
            lastNonNoneDisplayMode = targetMode;
        }

        activeDisplayMode = targetMode;
        applyDisplayMode();
        persistDisplayState();
    }

    public void toggleDisplay(BoxType boxType) {
        BoxType resolvedBox = boxType == null ? BoxType.BEDROCK : boxType;
        if (activeDisplayMode == DisplayMode.NONE || displayedBox != resolvedBox) {
            setDisplayMode(resolvedBox, lastNonNoneDisplayMode == DisplayMode.NONE ? configuredDisplayMode : lastNonNoneDisplayMode);
            return;
        }

        setDisplayMode(resolvedBox, DisplayMode.NONE);
    }

    public String getDisplayModeLabel() {
        return activeDisplayMode.name().toLowerCase(Locale.ROOT);
    }

    public String getDisplaySummary() {
        if (activeDisplayMode == DisplayMode.NONE) {
            return "off";
        }

        return displayedBox.getDisplayName() + " / " + getDisplayModeLabel();
    }

    public String getWinsDisplay(BoxType boxType) {
        Integer targetWins = getTargetWins(boxType);
        if (targetWins == null) {
            return Integer.toString(getWins(boxType));
        }

        return getWins(boxType) + "/" + targetWins;
    }

    public int initializeMenuValue(Player player, BoxType boxType, MenuValueType type) {
        return setMenuValue(player, boxType, type, type.getDefaultValue());
    }

    public int adjustMenuValue(Player player, BoxType boxType, MenuValueType type, int delta) {
        int currentValue = getMenuValue(player, boxType, type);
        return setMenuValue(player, boxType, type, currentValue + delta);
    }

    public int resetMenuValue(Player player, BoxType boxType, MenuValueType type) {
        return setMenuValue(player, boxType, type, type.getDefaultValue());
    }

    public int getMenuValue(Player player, BoxType boxType, MenuValueType type) {
        EnumMap<BoxType, EnumMap<MenuValueType, Integer>> valuesByBox = menuValues.get(player.getUniqueId());
        if (valuesByBox == null) {
            return type.getDefaultValue();
        }

        EnumMap<MenuValueType, Integer> valuesByType = valuesByBox.get(boxType);
        if (valuesByType == null) {
            return type.getDefaultValue();
        }

        return valuesByType.getOrDefault(type, type.getDefaultValue());
    }

    public int setMenuValue(Player player, BoxType boxType, MenuValueType type, int value) {
        EnumMap<BoxType, EnumMap<MenuValueType, Integer>> valuesByBox = menuValues.computeIfAbsent(
                player.getUniqueId(),
                ignored -> new EnumMap<>(BoxType.class)
        );
        EnumMap<MenuValueType, Integer> valuesByType = valuesByBox.computeIfAbsent(
                boxType,
                ignored -> new EnumMap<>(MenuValueType.class)
        );
        int clampedValue = type.clamp(value);
        valuesByType.put(type, clampedValue);
        return clampedValue;
    }

    public boolean canOpenAnyMenu(Player player) {
        return hasMenuAccess(player, BoxType.BEDROCK) || hasMenuAccess(player, BoxType.SANDBOX);
    }

    public boolean hasMenuAccess(Player player, BoxType boxType) {
        return player.hasPermission(boxType.getMenuPermission()) || player.hasPermission(boxType.getAdminPermission());
    }

    public boolean hasViewAccess(CommandSender sender, BoxType boxType) {
        if (!(sender instanceof Player)) {
            return true;
        }

        return sender.hasPermission(boxType.getViewPermission()) || sender.hasPermission(boxType.getAdminPermission());
    }

    public boolean hasAdminAccess(CommandSender sender, BoxType boxType) {
        if (!(sender instanceof Player)) {
            return true;
        }

        return sender.hasPermission(boxType.getAdminPermission());
    }

    public String getCounterMessageLabel(BoxType boxType) {
        return boxType.getWinsLabel();
    }

    public String getCounterDisplayName(BoxType boxType) {
        return boxType.getDisplayName();
    }

    public String getBoxCommand(BoxType boxType) {
        return boxType.getBoxCommand();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (bossBar != null
                && activeDisplayMode == DisplayMode.BOSSBAR
                && hasViewAccess(event.getPlayer(), displayedBox)) {
            bossBar.addPlayer(event.getPlayer());
        }
    }

    private void loadMarkers() {
        for (BoxType boxType : BoxType.trackedBoxes()) {
            winMarkers.get(boxType).clear();
        }

        boolean hasPerBoxMarkers = false;
        for (BoxType boxType : BoxType.trackedBoxes()) {
            String path = "boxes." + boxType.getKey() + ".win-markers";
            List<String> configuredMarkers = getConfig().getStringList(path);
            if (!configuredMarkers.isEmpty()) {
                hasPerBoxMarkers = true;
                configuredMarkers.forEach(marker -> addNormalizedMarker(boxType, marker));
            }
        }

        if (!hasPerBoxMarkers) {
            List<String> legacyMarkers = getConfig().getStringList("win-markers");
            if (legacyMarkers.isEmpty()) {
                for (BoxType boxType : BoxType.trackedBoxes()) {
                    boxType.getDefaultMarkers().forEach(marker -> addNormalizedMarker(boxType, marker));
                }
            } else {
                for (String marker : legacyMarkers) {
                    BoxType inferredBox = inferBoxTypeFromLegacyMarker(marker);
                    addNormalizedMarker(inferredBox, marker);
                }
            }
        }

        for (BoxType boxType : BoxType.trackedBoxes()) {
            if (winMarkers.get(boxType).isEmpty()) {
                boxType.getDefaultMarkers().forEach(marker -> addNormalizedMarker(boxType, marker));
            }
        }
    }

    private void loadDataFile() {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            throw new IllegalStateException("Could not create plugin data folder");
        }

        dataFile = new File(getDataFolder(), "data.yml");
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        for (BoxType boxType : BoxType.trackedBoxes()) {
            CounterState counterState = getCounterState(boxType);
            String basePath = "boxes." + boxType.getKey();

            if (dataConfig.isSet(basePath + ".wins")) {
                counterState.wins = dataConfig.getInt(basePath + ".wins");
            } else if (boxType == BoxType.BEDROCK) {
                counterState.wins = dataConfig.getInt("wins", 0);
            } else {
                counterState.wins = 0;
            }

            if (dataConfig.isSet(basePath + ".target-wins")) {
                counterState.targetWins = dataConfig.getInt(basePath + ".target-wins");
            } else if (boxType == BoxType.BEDROCK && dataConfig.isSet("target-wins")) {
                counterState.targetWins = dataConfig.getInt("target-wins");
            } else {
                counterState.targetWins = null;
            }
        }

        displayedBox = parseBoxType(dataConfig.getString("display.box"), BoxType.BEDROCK);
        activeDisplayMode = parseDisplayMode(dataConfig.getString("display.mode", "BOSSBAR"), DisplayMode.BOSSBAR);
        lastNonNoneDisplayMode = parseDisplayMode(
                dataConfig.getString("display.last-non-none-mode", "BOSSBAR"),
                DisplayMode.BOSSBAR
        );

        if (!dataFile.exists()) {
            saveDataFile();
        }
    }

    private void saveDataFile() {
        if (dataConfig == null || dataFile == null) {
            return;
        }

        dataConfig.set("wins", null);
        dataConfig.set("target-wins", null);

        for (BoxType boxType : BoxType.trackedBoxes()) {
            CounterState counterState = getCounterState(boxType);
            String basePath = "boxes." + boxType.getKey();
            dataConfig.set(basePath + ".wins", counterState.wins);
            dataConfig.set(basePath + ".target-wins", counterState.targetWins);
        }

        dataConfig.set("display.box", displayedBox.getKey());
        dataConfig.set("display.mode", activeDisplayMode.name());
        dataConfig.set("display.last-non-none-mode", lastNonNoneDisplayMode.name());

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
        if (isOwnPluginLogLine(message)) {
            return;
        }

        BoxType boxType = findMatchingBox(message);
        if (boxType == null) {
            return;
        }

        Bukkit.getScheduler().runTask(this, () -> incrementFromMarker(boxType, message));
    }

    private String extractLogMessage(String rawLine) {
        int separatorIndex = rawLine.indexOf("]: ");
        if (separatorIndex == -1) {
            return rawLine.trim();
        }

        return rawLine.substring(separatorIndex + 3).trim();
    }

    private void persistDisplayState() {
        if (dataConfig == null) {
            return;
        }

        dataConfig.set("display.box", displayedBox.getKey());
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
        lastScoreboardEntryName = null;

        disableSidebarDisplay();

        if (Bukkit.getScoreboardManager() == null) {
            getLogger().warning("Scoreboard manager is not available yet.");
            return;
        }

        String objectiveName = getConfig().getString("display.scoreboard.objective-name", "bedrock_wins");
        String displaySlotName = getConfig().getString("display.scoreboard.display-slot", "SIDEBAR");

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Objective existingObjective = scoreboard.getObjective(objectiveName);

        if (existingObjective == null) {
            objective = scoreboard.registerNewObjective(objectiveName, Criteria.DUMMY, getScoreboardDisplayName(displayedBox));
        } else {
            objective = existingObjective;
            objective.setDisplayName(getScoreboardDisplayName(displayedBox));
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
        if (objective == null) {
            return;
        }

        objective.setDisplayName(getScoreboardDisplayName(displayedBox));
        String entryName = getScoreboardEntryName(displayedBox);
        if (lastScoreboardEntryName != null && !lastScoreboardEntryName.equals(entryName)) {
            objective.getScoreboard().resetScores(lastScoreboardEntryName);
        }
        lastScoreboardEntryName = entryName;
        objective.getScore(entryName).setScore(getWins(displayedBox));

        if (activeDisplayMode == DisplayMode.SIDEBAR && scoreboardDisplaySlot != null) {
            objective.setDisplaySlot(scoreboardDisplaySlot);
        }
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
        if (objective == null || scoreboardDisplaySlot == null) {
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
            bossBar.removeAll();
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

    private String buildBossBarTitle() {
        String label = getBossBarTitleLabel(displayedBox);
        String color = getBossBarTextColor();
        return colorize(color + label + ": " + getWinsDisplay(displayedBox));
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

    private BoxType parseBoxType(String value, BoxType fallback) {
        BoxType parsed = BoxType.fromKey(value);
        return parsed == null ? fallback : parsed;
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

    private String getBossBarTitleLabel(BoxType boxType) {
        String path = "boxes." + boxType.getKey() + ".display.bossbar.title-label";
        String legacy = boxType == BoxType.BEDROCK
                ? getConfig().getString("display.bossbar.title-label", "Bedrock Wins")
                : boxType.getDisplayName() + " Wins";
        return getConfig().getString(path, legacy);
    }

    private String getBossBarTextColor() {
        int wins = getWins(displayedBox);
        if (wins < 0) {
            return getConfig().getString("display.bossbar.text-colors.negative", "&c");
        }

        if (wins == 0) {
            return getConfig().getString("display.bossbar.text-colors.zero", "&6");
        }

        return getConfig().getString("display.bossbar.text-colors.positive", "&a");
    }

    private BarColor getBossBarColorForWins() {
        int wins = getWins(displayedBox);
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

        Integer targetWins = getTargetWins(displayedBox);
        if (targetWins == null || targetWins <= 0) {
            return 1.0D;
        }

        double progress = (double) getWins(displayedBox) / (double) targetWins;
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
            if (hasViewAccess(player, displayedBox)) {
                bossBar.addPlayer(player);
            } else {
                bossBar.removePlayer(player);
            }
        }
    }

    private String getScoreboardDisplayName(BoxType boxType) {
        String path = "boxes." + boxType.getKey() + ".display.scoreboard.display-name";
        String legacy = boxType == BoxType.BEDROCK
                ? getConfig().getString("display.scoreboard.display-name", "&6Bedrock Wins")
                : "&6Sandbox Wins";
        return colorize(getConfig().getString(path, legacy));
    }

    private String getScoreboardEntryName(BoxType boxType) {
        String path = "boxes." + boxType.getKey() + ".display.scoreboard.entry-name";
        String legacy = boxType == BoxType.BEDROCK
                ? getConfig().getString("display.scoreboard.entry-name", "&eWins")
                : "&eSandbox";
        return colorize(getConfig().getString(path, legacy));
    }

    private CounterState getCounterState(BoxType boxType) {
        CounterState counterState = counterStates.get(boxType);
        if (counterState == null) {
            throw new IllegalArgumentException("Unsupported counter type: " + boxType);
        }
        return counterState;
    }

    private void addNormalizedMarker(BoxType boxType, String marker) {
        if (marker == null) {
            return;
        }

        String normalized = marker.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return;
        }

        winMarkers.get(boxType).add(normalized);
    }

    private BoxType inferBoxTypeFromLegacyMarker(String marker) {
        String normalized = marker == null ? "" : marker.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("sand") || normalized.contains("win-up")) {
            return BoxType.SANDBOX;
        }

        return BoxType.BEDROCK;
    }

    private boolean isOwnPluginLogLine(String message) {
        String normalizedMessage = message == null ? "" : message.trim().toLowerCase(Locale.ROOT);
        if (normalizedMessage.isEmpty()) {
            return false;
        }

        return normalizedMessage.startsWith("[bedrockwincounter]");
    }

    private BoxType findMatchingBox(String message) {
        String normalizedMessage = message == null ? "" : message.trim().toLowerCase(Locale.ROOT);
        if (normalizedMessage.isEmpty()) {
            return null;
        }

        for (BoxType boxType : BoxType.trackedBoxes()) {
            for (String marker : winMarkers.get(boxType)) {
                if (normalizedMessage.equals(marker) || normalizedMessage.endsWith(marker)) {
                    return boxType;
                }
            }
        }

        return null;
    }
}
