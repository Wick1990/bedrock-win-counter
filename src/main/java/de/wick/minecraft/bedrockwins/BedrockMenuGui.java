package de.wick.minecraft.bedrockwins;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public final class BedrockMenuGui implements Listener {

    private static final int DEFAULT_MENU_SIZE = 27;

    private enum Screen {
        MAIN("main"),
        TARGET("target"),
        CHANGE("change"),
        SET("set");

        private final String configKey;

        Screen(String configKey) {
            this.configKey = configKey;
        }

        public String getConfigKey() {
            return configKey;
        }
    }

    private record BedrockMenuHolder(Screen screen) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private final BedrockWinCounterPlugin plugin;

    public BedrockMenuGui(BedrockWinCounterPlugin plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        Inventory inventory = createInventory(
                Screen.MAIN,
                plugin.localize("menu.main.title", "&8Bedrock Menue"),
                getConfiguredSize(Screen.MAIN, DEFAULT_MENU_SIZE)
        );
        fillInventory(inventory);

        setConfiguredItem(inventory, Screen.MAIN, "header", 4, createLocalizedItem(
                plugin.getMenuMaterial("materials.main.header", Material.OBSIDIAN),
                1,
                "menu.main.header.name",
                "&5&lBedrock Menue",
                "menu.main.header.lore",
                List.of()
        ));
        setConfiguredItem(inventory, Screen.MAIN, "teleport", 10, createLocalizedItem(
                plugin.getMenuMaterial("materials.main.teleport", Material.ENDER_PEARL),
                1,
                "menu.main.teleport.name",
                "&bZur Box teleportieren",
                "menu.main.teleport.lore",
                List.of("&8Befehl: &7/bedrock tp")
        ));
        setConfiguredItem(inventory, Screen.MAIN, "main-user", 11, createLocalizedHead(
                player,
                "menu.main.main-user.name",
                "&aMain User setzen",
                "menu.main.main-user.lore",
                List.of(
                        "&7Setzt dich direkt als Main User.",
                        "",
                        "&8Befehl: &f/bedrock set_main_user %player%"
                ),
                "%player%", player.getName()
        ));
        setConfiguredItem(inventory, Screen.MAIN, "toplock", 12, createLocalizedItem(
                plugin.getMenuMaterial("materials.main.toplock", Material.IRON_BARS),
                1,
                "menu.main.toplock.name",
                "&7Toplock toggeln",
                "menu.main.toplock.lore",
                List.of("&8Befehl: &7/bedrock toplock")
        ));
        setConfiguredItem(inventory, Screen.MAIN, "block-range", 13, createLocalizedItem(
                plugin.getMenuMaterial("materials.main.block-range", Material.SPYGLASS),
                1,
                "menu.main.block-range.name",
                "&eBlock Range 20 setzen",
                "menu.main.block-range.lore",
                List.of(
                        "&7Setzt deine eigene",
                        "&7Block-Interaction-Range auf 20.",
                        "",
                        "&8Befehl: &f/attribute %player% ... 20"
                ),
                "%player%", player.getName()
        ));
        setConfiguredItem(inventory, Screen.MAIN, "fill", 14, createLocalizedItem(
                plugin.getMenuMaterial("materials.main.fill", Material.IRON_BLOCK),
                1,
                "menu.main.fill.name",
                "&fBox fuellen",
                "menu.main.fill.lore",
                List.of("&8Befehl: &7/bedrock fill")
        ));
        setConfiguredItem(inventory, Screen.MAIN, "clear", 15, createLocalizedItem(
                plugin.getMenuMaterial("materials.main.clear", Material.WATER_BUCKET),
                1,
                "menu.main.clear.name",
                "&9Box leeren",
                "menu.main.clear.lore",
                List.of("&8Befehl: &7/bedrock clear")
        ));
        setConfiguredItem(inventory, Screen.MAIN, "target", 19, createLocalizedItem(
                plugin.getMenuMaterial("materials.main.target", Material.TARGET),
                1,
                "menu.main.target.name",
                "&eWin-Ziel setzen",
                "menu.main.target.lore",
                List.of(
                        "&7Oeffnet ein Untermenue.",
                        "&7Dort stellst du den Zielwert",
                        "&7mit +1, +5 und +10 ein."
                )
        ));
        setConfiguredItem(inventory, Screen.MAIN, "change", 20, createLocalizedItem(
                plugin.getMenuMaterial("materials.main.change", Material.LIME_DYE),
                1,
                "menu.main.change.name",
                "&aWins aendern",
                "menu.main.change.lore",
                List.of(
                        "&7Oeffnet ein Untermenue.",
                        "&7Gruene Felder addieren Wins.",
                        "&7Rote Felder ziehen direkt ab."
                )
        ));
        setConfiguredItem(inventory, Screen.MAIN, "set", 21, createLocalizedItem(
                plugin.getMenuMaterial("materials.main.set", Material.WRITABLE_BOOK),
                1,
                "menu.main.set.name",
                "&6Wins set",
                "menu.main.set.lore",
                List.of(
                        "&7Oeffnet ein Untermenue.",
                        "&7Dort kannst du den Wert",
                        "&7auch negativ einstellen."
                )
        ));
        setConfiguredItem(inventory, Screen.MAIN, "reset", 22, createLocalizedItem(
                plugin.getMenuMaterial("materials.main.reset", Material.BARRIER),
                1,
                "menu.main.reset.name",
                "&4Wins reset",
                "menu.main.reset.lore",
                List.of("&8Befehl: &7/bedrockwins reset")
        ));
        setConfiguredItem(inventory, Screen.MAIN, "display-toggle", 23, createLocalizedItem(
                plugin.getMenuMaterial("materials.main.display-toggle", Material.REDSTONE_TORCH),
                1,
                "menu.main.display-toggle.name",
                "&dDisplay toggle",
                "menu.main.display-toggle.lore",
                List.of(
                        "&8Befehl: &7/bedrockwins display toggle",
                        "&7Aktuell: &f%mode%"
                ),
                "%mode%", plugin.getDisplayModeLabel()
        ));
        setConfiguredItem(inventory, Screen.MAIN, "wins-info", 24, createLocalizedItem(
                plugin.getMenuMaterial("materials.main.wins-info", Material.PAPER),
                1,
                "menu.main.wins-info.name",
                "&fAktuelle Wins",
                "menu.main.wins-info.lore",
                List.of("&7Stand: &6%wins%"),
                "%wins%", plugin.getWinsDisplay()
        ));
        setConfiguredItem(inventory, Screen.MAIN, "close", 26, createLocalizedItem(
                plugin.getMenuMaterial("materials.shared.close", Material.BARRIER),
                1,
                "menu.shared.close.name",
                "&cSchliessen",
                "menu.shared.close.lore",
                List.of()
        ));

        player.openInventory(inventory);
    }

    public void openTargetMenu(Player player) {
        int currentValue = plugin.getMenuValue(player, BedrockWinCounterPlugin.MenuValueType.TARGET);
        Inventory inventory = createInventory(
                Screen.TARGET,
                plugin.localize("menu.target.title", "&8Win-Ziel"),
                getConfiguredSize(Screen.TARGET, DEFAULT_MENU_SIZE)
        );
        fillInventory(inventory);

        setConfiguredItem(inventory, Screen.TARGET, "header", 4, createLocalizedItem(
                plugin.getMenuMaterial("materials.target.header", Material.TARGET),
                1,
                "menu.target.header.name",
                "&e&lWin-Ziel setzen",
                "menu.target.header.lore",
                List.of(
                        "&7Stelle den Zielwert mit den",
                        "&7Schaltflaechen unten ein."
                )
        ));
        setConfiguredItem(inventory, Screen.TARGET, "info", 13, createLocalizedItem(
                plugin.getMenuMaterial("materials.shared.info", Material.PAPER),
                1,
                "menu.target.info.name",
                "&fAktueller Zielwert",
                "menu.target.info.lore",
                List.of(
                        "&7Ziel: &e%value%",
                        "&7Aktuelle Wins: &6%wins%"
                ),
                "%value%", Integer.toString(currentValue),
                "%wins%", plugin.getWinsDisplay()
        ));
        setConfiguredItem(inventory, Screen.TARGET, "minus-ten", 10, createDeltaButton(
                false,
                plugin.getMenuAmount("amounts.minus-ten", 10),
                "menu.shared.minus-ten.name",
                "&c-10"
        ));
        setConfiguredItem(inventory, Screen.TARGET, "minus-five", 11, createDeltaButton(
                false,
                plugin.getMenuAmount("amounts.minus-five", 5),
                "menu.shared.minus-five.name",
                "&c-5"
        ));
        setConfiguredItem(inventory, Screen.TARGET, "minus-one", 12, createDeltaButton(
                false,
                plugin.getMenuAmount("amounts.minus-one", 1),
                "menu.shared.minus-one.name",
                "&c-1"
        ));
        setConfiguredItem(inventory, Screen.TARGET, "plus-one", 14, createDeltaButton(
                true,
                plugin.getMenuAmount("amounts.plus-one", 1),
                "menu.shared.plus-one.name",
                "&a+1"
        ));
        setConfiguredItem(inventory, Screen.TARGET, "plus-five", 15, createDeltaButton(
                true,
                plugin.getMenuAmount("amounts.plus-five", 5),
                "menu.shared.plus-five.name",
                "&a+5"
        ));
        setConfiguredItem(inventory, Screen.TARGET, "plus-ten", 16, createDeltaButton(
                true,
                plugin.getMenuAmount("amounts.plus-ten", 10),
                "menu.shared.plus-ten.name",
                "&a+10"
        ));
        setConfiguredItem(inventory, Screen.TARGET, "reset", 18, createLocalizedItem(
                plugin.getMenuMaterial("materials.shared.reset", Material.BARRIER),
                1,
                "menu.target.reset.name",
                "&eAuf 0 setzen",
                "menu.target.reset.lore",
                List.of()
        ));
        setConfiguredItem(inventory, Screen.TARGET, "back", 20, createLocalizedItem(
                plugin.getMenuMaterial("materials.shared.back", Material.ARROW),
                1,
                "menu.shared.back.name",
                "&7Zurueck",
                "menu.shared.back.lore",
                List.of()
        ));
        setConfiguredItem(inventory, Screen.TARGET, "confirm", 22, createLocalizedItem(
                plugin.getMenuMaterial("materials.target.confirm", Material.LIME_CONCRETE),
                1,
                "menu.target.confirm.name",
                "&aZiel uebernehmen",
                "menu.target.confirm.lore",
                List.of(
                        "&7Uebernimmt den aktuell",
                        "&7eingestellten Zielwert."
                )
        ));
        setConfiguredItem(inventory, Screen.TARGET, "close", 26, createLocalizedItem(
                plugin.getMenuMaterial("materials.shared.close-door", Material.OAK_DOOR),
                1,
                "menu.shared.close.name",
                "&cSchliessen",
                "menu.shared.close.lore",
                List.of()
        ));

        player.openInventory(inventory);
    }

    public void openChangeMenu(Player player) {
        Inventory inventory = createInventory(
                Screen.CHANGE,
                plugin.localize("menu.change.title", "&8Wins aendern"),
                getConfiguredSize(Screen.CHANGE, DEFAULT_MENU_SIZE)
        );
        fillInventory(inventory);

        setConfiguredItem(inventory, Screen.CHANGE, "header", 4, createLocalizedItem(
                plugin.getMenuMaterial("materials.change.header", Material.LIME_DYE),
                1,
                "menu.change.header.name",
                "&a&lWins aendern",
                "menu.change.header.lore",
                List.of(
                        "&7Jeder Klick fuehrt die",
                        "&7Aenderung sofort live aus."
                )
        ));
        setConfiguredItem(inventory, Screen.CHANGE, "info", 13, createLocalizedItem(
                plugin.getMenuMaterial("materials.shared.info", Material.PAPER),
                1,
                "menu.change.info.name",
                "&fDirekte Aenderung",
                "menu.change.info.lore",
                List.of(
                        "&aGruen = add",
                        "&cRot = remove",
                        "&7Stand: &6%wins%"
                ),
                "%wins%", plugin.getWinsDisplay()
        ));
        setConfiguredItem(inventory, Screen.CHANGE, "minus-ten", 10, createDeltaButton(
                false,
                plugin.getMenuAmount("amounts.minus-ten", 10),
                "menu.shared.minus-ten.name",
                "&c-10"
        ));
        setConfiguredItem(inventory, Screen.CHANGE, "minus-five", 11, createDeltaButton(
                false,
                plugin.getMenuAmount("amounts.minus-five", 5),
                "menu.shared.minus-five.name",
                "&c-5"
        ));
        setConfiguredItem(inventory, Screen.CHANGE, "minus-one", 12, createDeltaButton(
                false,
                plugin.getMenuAmount("amounts.minus-one", 1),
                "menu.shared.minus-one.name",
                "&c-1"
        ));
        setConfiguredItem(inventory, Screen.CHANGE, "plus-one", 14, createDeltaButton(
                true,
                plugin.getMenuAmount("amounts.plus-one", 1),
                "menu.shared.plus-one.name",
                "&a+1"
        ));
        setConfiguredItem(inventory, Screen.CHANGE, "plus-five", 15, createDeltaButton(
                true,
                plugin.getMenuAmount("amounts.plus-five", 5),
                "menu.shared.plus-five.name",
                "&a+5"
        ));
        setConfiguredItem(inventory, Screen.CHANGE, "plus-ten", 16, createDeltaButton(
                true,
                plugin.getMenuAmount("amounts.plus-ten", 10),
                "menu.shared.plus-ten.name",
                "&a+10"
        ));
        setConfiguredItem(inventory, Screen.CHANGE, "back", 22, createLocalizedItem(
                plugin.getMenuMaterial("materials.shared.back", Material.ARROW),
                1,
                "menu.shared.back.name",
                "&7Zurueck",
                "menu.shared.back.lore",
                List.of()
        ));
        setConfiguredItem(inventory, Screen.CHANGE, "close", 26, createLocalizedItem(
                plugin.getMenuMaterial("materials.shared.close-door", Material.OAK_DOOR),
                1,
                "menu.shared.close.name",
                "&cSchliessen",
                "menu.shared.close.lore",
                List.of()
        ));

        player.openInventory(inventory);
    }

    public void openSetMenu(Player player) {
        int currentValue = plugin.getMenuValue(player, BedrockWinCounterPlugin.MenuValueType.SET);
        Inventory inventory = createInventory(
                Screen.SET,
                plugin.localize("menu.set.title", "&8Wins set"),
                getConfiguredSize(Screen.SET, DEFAULT_MENU_SIZE)
        );
        fillInventory(inventory);

        setConfiguredItem(inventory, Screen.SET, "header", 4, createLocalizedItem(
                plugin.getMenuMaterial("materials.set.header", Material.WRITABLE_BOOK),
                1,
                "menu.set.header.name",
                "&6&lWins set",
                "menu.set.header.lore",
                List.of(
                        "&7Hier sind auch negative Werte",
                        "&7ausdruecklich erlaubt."
                )
        ));
        setConfiguredItem(inventory, Screen.SET, "info", 13, createLocalizedItem(
                plugin.getMenuMaterial("materials.shared.info", Material.PAPER),
                1,
                "menu.set.info.name",
                "&fAktueller Set-Wert",
                "menu.set.info.lore",
                List.of(
                        "&7Wert: &6%value%",
                        "&7Aktuelle Wins: &6%wins%"
                ),
                "%value%", Integer.toString(currentValue),
                "%wins%", plugin.getWinsDisplay()
        ));
        setConfiguredItem(inventory, Screen.SET, "minus-ten", 10, createDeltaButton(
                false,
                plugin.getMenuAmount("amounts.minus-ten", 10),
                "menu.shared.minus-ten.name",
                "&c-10"
        ));
        setConfiguredItem(inventory, Screen.SET, "minus-five", 11, createDeltaButton(
                false,
                plugin.getMenuAmount("amounts.minus-five", 5),
                "menu.shared.minus-five.name",
                "&c-5"
        ));
        setConfiguredItem(inventory, Screen.SET, "minus-one", 12, createDeltaButton(
                false,
                plugin.getMenuAmount("amounts.minus-one", 1),
                "menu.shared.minus-one.name",
                "&c-1"
        ));
        setConfiguredItem(inventory, Screen.SET, "plus-one", 14, createDeltaButton(
                true,
                plugin.getMenuAmount("amounts.plus-one", 1),
                "menu.shared.plus-one.name",
                "&a+1"
        ));
        setConfiguredItem(inventory, Screen.SET, "plus-five", 15, createDeltaButton(
                true,
                plugin.getMenuAmount("amounts.plus-five", 5),
                "menu.shared.plus-five.name",
                "&a+5"
        ));
        setConfiguredItem(inventory, Screen.SET, "plus-ten", 16, createDeltaButton(
                true,
                plugin.getMenuAmount("amounts.plus-ten", 10),
                "menu.shared.plus-ten.name",
                "&a+10"
        ));
        setConfiguredItem(inventory, Screen.SET, "reset", 18, createLocalizedItem(
                plugin.getMenuMaterial("materials.shared.reset", Material.BARRIER),
                1,
                "menu.set.reset.name",
                "&eAuf 0 setzen",
                "menu.set.reset.lore",
                List.of()
        ));
        setConfiguredItem(inventory, Screen.SET, "back", 20, createLocalizedItem(
                plugin.getMenuMaterial("materials.shared.back", Material.ARROW),
                1,
                "menu.shared.back.name",
                "&7Zurueck",
                "menu.shared.back.lore",
                List.of()
        ));
        setConfiguredItem(inventory, Screen.SET, "confirm", 22, createLocalizedItem(
                plugin.getMenuMaterial("materials.set.confirm", Material.GOLD_BLOCK),
                1,
                "menu.set.confirm.name",
                "&6Set ausfuehren",
                "menu.set.confirm.lore",
                List.of(
                        "&7Uebernimmt den aktuell",
                        "&7eingestellten Set-Wert."
                )
        ));
        setConfiguredItem(inventory, Screen.SET, "close", 26, createLocalizedItem(
                plugin.getMenuMaterial("materials.shared.close-door", Material.OAK_DOOR),
                1,
                "menu.shared.close.name",
                "&cSchliessen",
                "menu.shared.close.lore",
                List.of()
        ));

        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!(event.getView().getTopInventory().getHolder() instanceof BedrockMenuHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        Inventory inventory = event.getView().getTopInventory();
        switch (holder.screen()) {
            case MAIN -> handleMainMenuClick(player, inventory, event.getSlot());
            case TARGET -> handleTargetMenuClick(player, inventory, event.getSlot());
            case CHANGE -> handleChangeMenuClick(player, inventory, event.getSlot());
            case SET -> handleSetMenuClick(player, inventory, event.getSlot());
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof BedrockMenuHolder) {
            event.setCancelled(true);
        }
    }

    private void handleMainMenuClick(Player player, Inventory inventory, int slot) {
        if (isConfiguredSlot(inventory, Screen.MAIN, "teleport", 10, slot)) {
            runPlayerCommand(player, "bedrock tp");
            return;
        }
        if (isConfiguredSlot(inventory, Screen.MAIN, "main-user", 11, slot)) {
            runPlayerCommand(player, "bedrock set_main_user " + player.getName());
            return;
        }
        if (isConfiguredSlot(inventory, Screen.MAIN, "toplock", 12, slot)) {
            runPlayerCommand(player, "bedrock toplock");
            return;
        }
        if (isConfiguredSlot(inventory, Screen.MAIN, "block-range", 13, slot)) {
            runConsoleCommand("minecraft:attribute " + player.getName() + " minecraft:block_interaction_range base set 20");
            return;
        }
        if (isConfiguredSlot(inventory, Screen.MAIN, "fill", 14, slot)) {
            runPlayerCommand(player, "bedrock fill");
            return;
        }
        if (isConfiguredSlot(inventory, Screen.MAIN, "clear", 15, slot)) {
            runPlayerCommand(player, "bedrock clear");
            return;
        }
        if (isConfiguredSlot(inventory, Screen.MAIN, "target", 19, slot)) {
            plugin.initializeMenuValue(player, BedrockWinCounterPlugin.MenuValueType.TARGET);
            openTargetMenu(player);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.MAIN, "change", 20, slot)) {
            openChangeMenu(player);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.MAIN, "set", 21, slot)) {
            plugin.initializeMenuValue(player, BedrockWinCounterPlugin.MenuValueType.SET);
            openSetMenu(player);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.MAIN, "reset", 22, slot)) {
            if (ensureWinsAdmin(player)) {
                plugin.setWins(0);
                plugin.sendLocalized(player, "messages.wins-reset", "&aBedrock-Wins wurden zurueckgesetzt.");
                reopenMenu(player, Screen.MAIN);
            }
            return;
        }
        if (isConfiguredSlot(inventory, Screen.MAIN, "display-toggle", 23, slot)) {
            if (ensureWinsAdmin(player)) {
                plugin.toggleDisplay();
                plugin.sendLocalized(
                        player,
                        "messages.display-toggled",
                        "&aAnzeige umgeschaltet: &6%mode%",
                        "%mode%", plugin.getDisplayModeLabel()
                );
                reopenMenu(player, Screen.MAIN);
            }
            return;
        }
        if (isConfiguredSlot(inventory, Screen.MAIN, "close", 26, slot)) {
            player.closeInventory();
        }
    }

    private void handleTargetMenuClick(Player player, Inventory inventory, int slot) {
        if (!ensureWinsAdmin(player)) {
            return;
        }

        if (isConfiguredSlot(inventory, Screen.TARGET, "minus-ten", 10, slot)) {
            adjustAndReopen(player, BedrockWinCounterPlugin.MenuValueType.TARGET, -10, Screen.TARGET);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.TARGET, "minus-five", 11, slot)) {
            adjustAndReopen(player, BedrockWinCounterPlugin.MenuValueType.TARGET, -5, Screen.TARGET);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.TARGET, "minus-one", 12, slot)) {
            adjustAndReopen(player, BedrockWinCounterPlugin.MenuValueType.TARGET, -1, Screen.TARGET);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.TARGET, "plus-one", 14, slot)) {
            adjustAndReopen(player, BedrockWinCounterPlugin.MenuValueType.TARGET, 1, Screen.TARGET);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.TARGET, "plus-five", 15, slot)) {
            adjustAndReopen(player, BedrockWinCounterPlugin.MenuValueType.TARGET, 5, Screen.TARGET);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.TARGET, "plus-ten", 16, slot)) {
            adjustAndReopen(player, BedrockWinCounterPlugin.MenuValueType.TARGET, 10, Screen.TARGET);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.TARGET, "reset", 18, slot)) {
            int resetValue = plugin.setMenuValue(player, BedrockWinCounterPlugin.MenuValueType.TARGET, 0);
            plugin.sendLocalized(
                    player,
                    "messages.menu-value-set",
                    "&7Menuewert &8(%type%&8) &7gesetzt auf: &6%value%",
                    "%type%", "target",
                    "%value%", Integer.toString(resetValue)
            );
            reopenMenu(player, Screen.TARGET);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.TARGET, "back", 20, slot)) {
            openMainMenu(player);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.TARGET, "confirm", 22, slot)) {
            int value = plugin.getMenuValue(player, BedrockWinCounterPlugin.MenuValueType.TARGET);
            plugin.setTargetWins(value);
            plugin.sendLocalized(
                    player,
                    "messages.target-set",
                    "&aWin-Ziel gesetzt: &6%value%",
                    "%value%", Integer.toString(value)
            );
            openMainMenu(player);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.TARGET, "close", 26, slot)) {
            player.closeInventory();
        }
    }

    private void handleChangeMenuClick(Player player, Inventory inventory, int slot) {
        if (!ensureWinsAdmin(player)) {
            return;
        }

        if (isConfiguredSlot(inventory, Screen.CHANGE, "minus-ten", 10, slot)) {
            applyLiveChange(player, -10);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.CHANGE, "minus-five", 11, slot)) {
            applyLiveChange(player, -5);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.CHANGE, "minus-one", 12, slot)) {
            applyLiveChange(player, -1);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.CHANGE, "plus-one", 14, slot)) {
            applyLiveChange(player, 1);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.CHANGE, "plus-five", 15, slot)) {
            applyLiveChange(player, 5);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.CHANGE, "plus-ten", 16, slot)) {
            applyLiveChange(player, 10);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.CHANGE, "back", 22, slot)) {
            openMainMenu(player);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.CHANGE, "close", 26, slot)) {
            player.closeInventory();
        }
    }

    private void handleSetMenuClick(Player player, Inventory inventory, int slot) {
        if (!ensureWinsAdmin(player)) {
            return;
        }

        if (isConfiguredSlot(inventory, Screen.SET, "minus-ten", 10, slot)) {
            adjustAndReopen(player, BedrockWinCounterPlugin.MenuValueType.SET, -10, Screen.SET);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.SET, "minus-five", 11, slot)) {
            adjustAndReopen(player, BedrockWinCounterPlugin.MenuValueType.SET, -5, Screen.SET);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.SET, "minus-one", 12, slot)) {
            adjustAndReopen(player, BedrockWinCounterPlugin.MenuValueType.SET, -1, Screen.SET);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.SET, "plus-one", 14, slot)) {
            adjustAndReopen(player, BedrockWinCounterPlugin.MenuValueType.SET, 1, Screen.SET);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.SET, "plus-five", 15, slot)) {
            adjustAndReopen(player, BedrockWinCounterPlugin.MenuValueType.SET, 5, Screen.SET);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.SET, "plus-ten", 16, slot)) {
            adjustAndReopen(player, BedrockWinCounterPlugin.MenuValueType.SET, 10, Screen.SET);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.SET, "reset", 18, slot)) {
            int resetValue = plugin.setMenuValue(player, BedrockWinCounterPlugin.MenuValueType.SET, 0);
            plugin.sendLocalized(
                    player,
                    "messages.menu-value-set",
                    "&7Menuewert &8(%type%&8) &7gesetzt auf: &6%value%",
                    "%type%", "set",
                    "%value%", Integer.toString(resetValue)
            );
            reopenMenu(player, Screen.SET);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.SET, "back", 20, slot)) {
            openMainMenu(player);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.SET, "confirm", 22, slot)) {
            int value = plugin.getMenuValue(player, BedrockWinCounterPlugin.MenuValueType.SET);
            plugin.setWins(value);
            plugin.sendLocalized(
                    player,
                    "messages.wins-set",
                    "&aBedrock-Wins gesetzt. Neuer Stand: &6%wins%",
                    "%wins%", Integer.toString(plugin.getWins())
            );
            openMainMenu(player);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.SET, "close", 26, slot)) {
            player.closeInventory();
        }
    }

    private void applyLiveChange(Player player, int delta) {
        plugin.addWins(delta);
        if (delta >= 0) {
            plugin.sendLocalized(
                    player,
                    "messages.wins-adjusted",
                    "&aBedrock-Wins angepasst. Neuer Stand: &6%wins%",
                    "%wins%", Integer.toString(plugin.getWins())
            );
        } else {
            plugin.sendLocalized(
                    player,
                    "messages.wins-reduced",
                    "&aBedrock-Wins reduziert. Neuer Stand: &6%wins%",
                    "%wins%", Integer.toString(plugin.getWins())
            );
        }
        reopenMenu(player, Screen.CHANGE);
    }

    private void adjustAndReopen(Player player, BedrockWinCounterPlugin.MenuValueType type, int delta, Screen screen) {
        int newValue = plugin.adjustMenuValue(player, type, delta);
        plugin.sendLocalized(
                player,
                "messages.menu-value-now",
                "&7Menuewert &8(%type%&8) &7jetzt: &6%value%",
                "%type%", type.getKey(),
                "%value%", Integer.toString(newValue)
        );
        reopenMenu(player, screen);
    }

    private void reopenMenu(Player player, Screen screen) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            switch (screen) {
                case MAIN -> openMainMenu(player);
                case TARGET -> openTargetMenu(player);
                case CHANGE -> openChangeMenu(player);
                case SET -> openSetMenu(player);
            }
        });
    }

    private boolean ensureWinsAdmin(CommandSender sender) {
        if (sender.hasPermission("bedrockwins.admin")) {
            return true;
        }

        plugin.sendLocalized(sender, "messages.no-permission", "&cDafuer fehlen dir die Rechte.");
        return false;
    }

    private void runPlayerCommand(Player player, String command) {
        Bukkit.dispatchCommand(player, command);
    }

    private void runConsoleCommand(String command) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    private Inventory createInventory(Screen screen, String title, int size) {
        return Bukkit.createInventory(new BedrockMenuHolder(screen), size, plugin.colorize(title));
    }

    private void fillInventory(Inventory inventory) {
        ItemStack filler = createItem(
                plugin.getMenuMaterial("materials.shared.filler", Material.BLACK_STAINED_GLASS_PANE),
                " "
        );
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
    }

    private ItemStack createLocalizedHead(
            Player player,
            String namePath,
            String fallbackName,
            String lorePath,
            List<String> fallbackLore,
            String... replacements
    ) {
        ItemStack item = createLocalizedItem(
                plugin.getMenuMaterial("materials.main.main-user", Material.PLAYER_HEAD),
                1,
                namePath,
                fallbackName,
                lorePath,
                fallbackLore,
                replacements
        );
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(player);
            item.setItemMeta(skullMeta);
        }
        return item;
    }

    private ItemStack createLocalizedItem(
            Material material,
            int amount,
            String namePath,
            String fallbackName,
            String lorePath,
            List<String> fallbackLore,
            String... replacements
    ) {
        String name = plugin.localize(namePath, fallbackName, replacements);
        List<String> lore = plugin.localizeList(lorePath, fallbackLore, replacements);
        return createItem(material, amount, name, lore);
    }

    private ItemStack createDeltaButton(boolean positive, int amount, String namePath, String fallbackName) {
        Material material = plugin.getMenuMaterial(
                positive ? "materials.shared.positive" : "materials.shared.negative",
                positive ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE
        );
        return createLocalizedItem(
                material,
                amount,
                namePath,
                fallbackName,
                positive ? "menu.shared.positive-lore" : "menu.shared.negative-lore",
                List.of()
        );
    }

    private ItemStack createItem(Material material, int amount, String name, List<String> lore) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (!lore.isEmpty()) {
                meta.setLore(lore);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        return createItem(material, 1, plugin.colorize(name), loreLines(lore));
    }

    private List<String> loreLines(String... lines) {
        return java.util.Arrays.stream(lines)
                .map(plugin::colorize)
                .toList();
    }

    private int getConfiguredSize(Screen screen, int fallback) {
        return plugin.getMenuSize("layout." + screen.getConfigKey() + ".size", fallback);
    }

    private void setConfiguredItem(Inventory inventory, Screen screen, String key, int fallbackSlot, ItemStack item) {
        inventory.setItem(getConfiguredSlot(inventory, screen, key, fallbackSlot), item);
    }

    private boolean isConfiguredSlot(Inventory inventory, Screen screen, String key, int fallbackSlot, int slot) {
        return getConfiguredSlot(inventory, screen, key, fallbackSlot) == slot;
    }

    private int getConfiguredSlot(Inventory inventory, Screen screen, String key, int fallbackSlot) {
        return plugin.getMenuSlot("layout." + screen.getConfigKey() + "." + key, fallbackSlot, inventory.getSize());
    }
}
