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
        ROOT("root"),
        BOX_MAIN("box-main"),
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

    private record BedrockMenuHolder(Screen screen, BedrockWinCounterPlugin.BoxType boxType) implements InventoryHolder {
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
                Screen.ROOT,
                null,
                plugin.localize("menu.root.title", "&8Box Hub"),
                getConfiguredSize(Screen.ROOT, DEFAULT_MENU_SIZE)
        );
        fillInventory(inventory);

        setConfiguredItem(inventory, Screen.ROOT, "header", 4, createLocalizedItem(
                plugin.getMenuMaterial("materials.root.header", Material.NETHER_STAR),
                1,
                "menu.root.header.name",
                "&6&lBox Hub",
                "menu.root.header.lore",
                List.of("&7Waehle die gewuenschte Box.")
        ));
        setConfiguredItem(inventory, Screen.ROOT, "bedrock", 11, createLocalizedItem(
                plugin.getMenuMaterial("materials.root.bedrock", Material.OBSIDIAN),
                1,
                "menu.root.bedrock.name",
                "&5BedrockBox",
                "menu.root.bedrock.lore",
                List.of(
                        "&7Oeffnet das Bedrock-Menue.",
                        "&7Wins: &6%wins%"
                ),
                "%wins%", plugin.getWinsDisplay(BedrockWinCounterPlugin.BoxType.BEDROCK)
        ));
        setConfiguredItem(inventory, Screen.ROOT, "sandbox", 13, createLocalizedItem(
                plugin.getMenuMaterial("materials.root.sandbox", Material.SAND),
                1,
                "menu.root.sandbox.name",
                "&6Sandbox",
                "menu.root.sandbox.lore",
                List.of(
                        "&7Oeffnet das Sandbox-Menue.",
                        "&7Wins: &6%wins%"
                ),
                "%wins%", plugin.getWinsDisplay(BedrockWinCounterPlugin.BoxType.SANDBOX)
        ));
        setConfiguredItem(inventory, Screen.ROOT, "sheepout", 15, createLocalizedItem(
                plugin.getMenuMaterial("materials.root.sheepout", Material.WHITE_WOOL),
                1,
                "menu.root.sheepout.name",
                "&fSheep Out",
                "menu.root.sheepout.lore",
                List.of("&7Aktuell nur ein Platzhalter.")
        ));
        setConfiguredItem(inventory, Screen.ROOT, "display-info", 22, createLocalizedItem(
                plugin.getMenuMaterial("materials.root.display-info", Material.REDSTONE_LAMP),
                1,
                "menu.root.display-info.name",
                "&dAktive Anzeige",
                "menu.root.display-info.lore",
                List.of("&7Status: &f%mode%"),
                "%mode%", plugin.getDisplaySummary()
        ));
        setConfiguredItem(inventory, Screen.ROOT, "close", 26, createLocalizedItem(
                plugin.getMenuMaterial("materials.shared.close", Material.BARRIER),
                1,
                "menu.shared.close.name",
                "&cSchliessen",
                "menu.shared.close.lore",
                List.of()
        ));

        player.openInventory(inventory);
    }

    public void openBoxMenu(Player player, BedrockWinCounterPlugin.BoxType boxType) {
        if (boxType == null || !boxType.tracksWins()) {
            plugin.sendLocalized(
                    player,
                    "messages.box-coming-soon",
                    "&e%box% ist aktuell nur ein Platzhalter.",
                    "%box%", boxType == null ? "Diese Box" : boxType.getDisplayName()
            );
            return;
        }

        Inventory inventory = createInventory(
                Screen.BOX_MAIN,
                boxType,
                plugin.localize(
                        "menu.box-main.title",
                        "&8%box% Menue",
                        "%box%", boxType.getDisplayName()
                ),
                getConfiguredSize(Screen.BOX_MAIN, DEFAULT_MENU_SIZE)
        );
        fillInventory(inventory);

        setConfiguredItem(inventory, Screen.BOX_MAIN, "header", 4, createLocalizedItem(
                getBoxMaterial(boxType, "main.header", boxType == BedrockWinCounterPlugin.BoxType.BEDROCK ? Material.OBSIDIAN : Material.SANDSTONE),
                1,
                boxPath(boxType, "main.header.name"),
                boxType == BedrockWinCounterPlugin.BoxType.BEDROCK ? "&5&lBedrock Menue" : "&6&lSandbox Menue",
                boxPath(boxType, "main.header.lore"),
                List.of()
        ));

        if (boxType == BedrockWinCounterPlugin.BoxType.BEDROCK) {
            populateBedrockActionItems(inventory, player);
        } else if (boxType == BedrockWinCounterPlugin.BoxType.SANDBOX) {
            populateSandboxActionItems(inventory, player);
        }

        setConfiguredItem(inventory, Screen.BOX_MAIN, "back", 18, createLocalizedItem(
                plugin.getMenuMaterial("materials.shared.back", Material.ARROW),
                1,
                "menu.shared.back.name",
                "&7Zurueck",
                "menu.shared.back.lore",
                List.of()
        ));
        setConfiguredItem(inventory, Screen.BOX_MAIN, "target", 19, createLocalizedItem(
                plugin.getMenuMaterial("materials.box-main.target", Material.TARGET),
                1,
                "menu.box-main.target.name",
                "&eWin-Ziel setzen",
                "menu.box-main.target.lore",
                List.of(
                        "&7Oeffnet ein Untermenue.",
                        "&7Dort stellst du den Zielwert ein."
                )
        ));
        setConfiguredItem(inventory, Screen.BOX_MAIN, "change", 20, createLocalizedItem(
                plugin.getMenuMaterial("materials.box-main.change", Material.LIME_DYE),
                1,
                "menu.box-main.change.name",
                "&aWins aendern",
                "menu.box-main.change.lore",
                List.of(
                        "&7Gruene Felder addieren Wins.",
                        "&7Rote Felder ziehen direkt ab."
                )
        ));
        setConfiguredItem(inventory, Screen.BOX_MAIN, "set", 21, createLocalizedItem(
                plugin.getMenuMaterial("materials.box-main.set", Material.WRITABLE_BOOK),
                1,
                "menu.box-main.set.name",
                "&6Wins set",
                "menu.box-main.set.lore",
                List.of(
                        "&7Setzt den Win-Stand direkt.",
                        "&7Negative Werte sind erlaubt."
                )
        ));
        setConfiguredItem(inventory, Screen.BOX_MAIN, "reset", 22, createLocalizedItem(
                plugin.getMenuMaterial("materials.box-main.reset", Material.BARRIER),
                1,
                "menu.box-main.reset.name",
                "&4Wins reset",
                "menu.box-main.reset.lore",
                List.of("&8Befehl: &7/%command% reset"),
                "%command%", boxType.getWinsCommand()
        ));
        setConfiguredItem(inventory, Screen.BOX_MAIN, "display-toggle", 23, createLocalizedItem(
                plugin.getMenuMaterial("materials.box-main.display-toggle", Material.REDSTONE_TORCH),
                1,
                "menu.box-main.display-toggle.name",
                "&dDisplay toggle",
                "menu.box-main.display-toggle.lore",
                List.of(
                        "&8Befehl: &7/%command% display toggle",
                        "&7Aktuell: &f%mode%"
                ),
                "%command%", boxType.getWinsCommand(),
                "%mode%", plugin.getDisplaySummary()
        ));
        setConfiguredItem(inventory, Screen.BOX_MAIN, "wins-info", 24, createLocalizedItem(
                plugin.getMenuMaterial("materials.box-main.wins-info", Material.PAPER),
                1,
                "menu.box-main.wins-info.name",
                "&fAktuelle Wins",
                "menu.box-main.wins-info.lore",
                List.of(
                        "&7Box: &f%box%",
                        "&7Stand: &6%wins%"
                ),
                "%box%", boxType.getDisplayName(),
                "%wins%", plugin.getWinsDisplay(boxType)
        ));
        setConfiguredItem(inventory, Screen.BOX_MAIN, "close", 26, createLocalizedItem(
                plugin.getMenuMaterial("materials.shared.close", Material.BARRIER),
                1,
                "menu.shared.close.name",
                "&cSchliessen",
                "menu.shared.close.lore",
                List.of()
        ));

        player.openInventory(inventory);
    }

    public void openTargetMenu(Player player, BedrockWinCounterPlugin.BoxType boxType) {
        int currentValue = plugin.getMenuValue(player, boxType, BedrockWinCounterPlugin.MenuValueType.TARGET);
        Inventory inventory = createInventory(
                Screen.TARGET,
                boxType,
                plugin.localize("menu.target.title", "&8%box% Ziel", "%box%", boxType.getDisplayName()),
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
                        "&7Box: &f%box%",
                        "&7Ziel: &e%value%",
                        "&7Aktuelle Wins: &6%wins%"
                ),
                "%box%", boxType.getDisplayName(),
                "%value%", Integer.toString(currentValue),
                "%wins%", plugin.getWinsDisplay(boxType)
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

    public void openChangeMenu(Player player, BedrockWinCounterPlugin.BoxType boxType) {
        Inventory inventory = createInventory(
                Screen.CHANGE,
                boxType,
                plugin.localize("menu.change.title", "&8%box% Wins aendern", "%box%", boxType.getDisplayName()),
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
                        "&7Box: &f%box%",
                        "&aGruen = add",
                        "&cRot = remove",
                        "&7Stand: &6%wins%"
                ),
                "%box%", boxType.getDisplayName(),
                "%wins%", plugin.getWinsDisplay(boxType)
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

    public void openSetMenu(Player player, BedrockWinCounterPlugin.BoxType boxType) {
        int currentValue = plugin.getMenuValue(player, boxType, BedrockWinCounterPlugin.MenuValueType.SET);
        Inventory inventory = createInventory(
                Screen.SET,
                boxType,
                plugin.localize("menu.set.title", "&8%box% Wins set", "%box%", boxType.getDisplayName()),
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
                        "&7Box: &f%box%",
                        "&7Wert: &6%value%",
                        "&7Aktuelle Wins: &6%wins%"
                ),
                "%box%", boxType.getDisplayName(),
                "%value%", Integer.toString(currentValue),
                "%wins%", plugin.getWinsDisplay(boxType)
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
            case ROOT -> handleRootMenuClick(player, inventory, event.getSlot());
            case BOX_MAIN -> {
                if (holder.boxType() != null) {
                    handleBoxMainMenuClick(player, holder.boxType(), inventory, event.getSlot());
                }
            }
            case TARGET -> {
                if (holder.boxType() != null) {
                    handleTargetMenuClick(player, holder.boxType(), inventory, event.getSlot());
                }
            }
            case CHANGE -> {
                if (holder.boxType() != null) {
                    handleChangeMenuClick(player, holder.boxType(), inventory, event.getSlot());
                }
            }
            case SET -> {
                if (holder.boxType() != null) {
                    handleSetMenuClick(player, holder.boxType(), inventory, event.getSlot());
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof BedrockMenuHolder) {
            event.setCancelled(true);
        }
    }

    private void populateBedrockActionItems(Inventory inventory, Player player) {
        BedrockWinCounterPlugin.BoxType boxType = BedrockWinCounterPlugin.BoxType.BEDROCK;
        setConfiguredItem(inventory, Screen.BOX_MAIN, "teleport", 10, createLocalizedItem(
                getBoxMaterial(boxType, "main.teleport", Material.ENDER_PEARL),
                1,
                boxPath(boxType, "main.teleport.name"),
                "&bZur Box teleportieren",
                boxPath(boxType, "main.teleport.lore"),
                List.of("&8Befehl: &7/bedrock tp")
        ));
        setConfiguredItem(inventory, Screen.BOX_MAIN, "main-user", 11, createLocalizedHead(
                player,
                getBoxMaterial(boxType, "main.main-user", Material.PLAYER_HEAD),
                boxPath(boxType, "main.main-user.name"),
                "&aMain User setzen",
                boxPath(boxType, "main.main-user.lore"),
                List.of(
                        "&7Setzt dich direkt als Main User.",
                        "",
                        "&8Befehl: &f/bedrock set_main_user %player%"
                ),
                "%player%", player.getName()
        ));
        setConfiguredItem(inventory, Screen.BOX_MAIN, "action-three", 12, createLocalizedItem(
                getBoxMaterial(boxType, "main.toplock", Material.IRON_BARS),
                1,
                boxPath(boxType, "main.toplock.name"),
                "&7Toplock toggeln",
                boxPath(boxType, "main.toplock.lore"),
                List.of("&8Befehl: &7/bedrock toplock")
        ));
        setConfiguredItem(inventory, Screen.BOX_MAIN, "action-four", 13, createLocalizedItem(
                getBoxMaterial(boxType, "main.block-range", Material.SPYGLASS),
                1,
                boxPath(boxType, "main.block-range.name"),
                "&eBlock Range 20 setzen",
                boxPath(boxType, "main.block-range.lore"),
                List.of(
                        "&7Setzt deine eigene",
                        "&7Block-Interaction-Range auf 20.",
                        "",
                        "&8Befehl: &f/attribute %player% ... 20"
                ),
                "%player%", player.getName()
        ));
        setConfiguredItem(inventory, Screen.BOX_MAIN, "fill", 14, createLocalizedItem(
                getBoxMaterial(boxType, "main.fill", Material.IRON_BLOCK),
                1,
                boxPath(boxType, "main.fill.name"),
                "&fBox fuellen",
                boxPath(boxType, "main.fill.lore"),
                List.of("&8Befehl: &7/bedrock fill")
        ));
        setConfiguredItem(inventory, Screen.BOX_MAIN, "clear", 15, createLocalizedItem(
                getBoxMaterial(boxType, "main.clear", Material.WATER_BUCKET),
                1,
                boxPath(boxType, "main.clear.name"),
                "&9Box leeren",
                boxPath(boxType, "main.clear.lore"),
                List.of("&8Befehl: &7/bedrock clear")
        ));
    }

    private void populateSandboxActionItems(Inventory inventory, Player player) {
        BedrockWinCounterPlugin.BoxType boxType = BedrockWinCounterPlugin.BoxType.SANDBOX;
        setConfiguredItem(inventory, Screen.BOX_MAIN, "teleport", 10, createLocalizedItem(
                getBoxMaterial(boxType, "main.teleport", Material.ENDER_PEARL),
                1,
                boxPath(boxType, "main.teleport.name"),
                "&bZur Sandbox teleportieren",
                boxPath(boxType, "main.teleport.lore"),
                List.of("&8Befehl: &7/sandbox tp")
        ));
        setConfiguredItem(inventory, Screen.BOX_MAIN, "main-user", 11, createLocalizedHead(
                player,
                getBoxMaterial(boxType, "main.main-user", Material.PLAYER_HEAD),
                boxPath(boxType, "main.main-user.name"),
                "&aMain User setzen",
                boxPath(boxType, "main.main-user.lore"),
                List.of(
                        "&7Setzt dich direkt als Main User.",
                        "",
                        "&8Befehl: &f/sandbox set_main_user %player%"
                ),
                "%player%", player.getName()
        ));
        setConfiguredItem(inventory, Screen.BOX_MAIN, "action-three", 12, createLocalizedItem(
                getBoxMaterial(boxType, "main.timer", Material.CLOCK),
                1,
                boxPath(boxType, "main.timer.name"),
                "&eTimer starten",
                boxPath(boxType, "main.timer.lore"),
                List.of("&8Befehl: &7/sandbox timer 10")
        ));
        setConfiguredItem(inventory, Screen.BOX_MAIN, "action-four", 13, createLocalizedItem(
                getBoxMaterial(boxType, "main.stop", Material.BARRIER),
                1,
                boxPath(boxType, "main.stop.name"),
                "&cSandbox stoppen",
                boxPath(boxType, "main.stop.lore"),
                List.of("&8Befehl: &7/sandbox stop")
        ));
        setConfiguredItem(inventory, Screen.BOX_MAIN, "fill", 14, createLocalizedItem(
                getBoxMaterial(boxType, "main.fill", Material.SAND),
                1,
                boxPath(boxType, "main.fill.name"),
                "&6Sandbox fuellen",
                boxPath(boxType, "main.fill.lore"),
                List.of("&8Befehl: &7/sandbox fill")
        ));
        setConfiguredItem(inventory, Screen.BOX_MAIN, "clear", 15, createLocalizedItem(
                getBoxMaterial(boxType, "main.clear", Material.WATER_BUCKET),
                1,
                boxPath(boxType, "main.clear.name"),
                "&9Sandbox leeren",
                boxPath(boxType, "main.clear.lore"),
                List.of("&8Befehl: &7/sandbox clear")
        ));
    }

    private void handleRootMenuClick(Player player, Inventory inventory, int slot) {
        if (isConfiguredSlot(inventory, Screen.ROOT, "bedrock", 11, slot)) {
            if (ensureMenuAccess(player, BedrockWinCounterPlugin.BoxType.BEDROCK)) {
                openBoxMenu(player, BedrockWinCounterPlugin.BoxType.BEDROCK);
            }
            return;
        }
        if (isConfiguredSlot(inventory, Screen.ROOT, "sandbox", 13, slot)) {
            if (ensureMenuAccess(player, BedrockWinCounterPlugin.BoxType.SANDBOX)) {
                openBoxMenu(player, BedrockWinCounterPlugin.BoxType.SANDBOX);
            }
            return;
        }
        if (isConfiguredSlot(inventory, Screen.ROOT, "sheepout", 15, slot)) {
            plugin.sendLocalized(
                    player,
                    "messages.box-coming-soon",
                    "&e%box% ist aktuell nur ein Platzhalter.",
                    "%box%", BedrockWinCounterPlugin.BoxType.SHEEP_OUT.getDisplayName()
            );
            return;
        }
        if (isConfiguredSlot(inventory, Screen.ROOT, "close", 26, slot)) {
            player.closeInventory();
        }
    }

    private void handleBoxMainMenuClick(
            Player player,
            BedrockWinCounterPlugin.BoxType boxType,
            Inventory inventory,
            int slot
    ) {
        if (boxType == BedrockWinCounterPlugin.BoxType.BEDROCK) {
            if (handleBedrockActionClick(player, inventory, slot)) {
                return;
            }
        } else if (boxType == BedrockWinCounterPlugin.BoxType.SANDBOX) {
            if (handleSandboxActionClick(player, inventory, slot)) {
                return;
            }
        }

        if (isConfiguredSlot(inventory, Screen.BOX_MAIN, "back", 18, slot)) {
            openMainMenu(player);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.BOX_MAIN, "target", 19, slot)) {
            if (ensureWinsAdmin(player, boxType)) {
                plugin.initializeMenuValue(player, boxType, BedrockWinCounterPlugin.MenuValueType.TARGET);
                openTargetMenu(player, boxType);
            }
            return;
        }
        if (isConfiguredSlot(inventory, Screen.BOX_MAIN, "change", 20, slot)) {
            if (ensureWinsAdmin(player, boxType)) {
                openChangeMenu(player, boxType);
            }
            return;
        }
        if (isConfiguredSlot(inventory, Screen.BOX_MAIN, "set", 21, slot)) {
            if (ensureWinsAdmin(player, boxType)) {
                plugin.initializeMenuValue(player, boxType, BedrockWinCounterPlugin.MenuValueType.SET);
                openSetMenu(player, boxType);
            }
            return;
        }
        if (isConfiguredSlot(inventory, Screen.BOX_MAIN, "reset", 22, slot)) {
            if (ensureWinsAdmin(player, boxType)) {
                plugin.setWins(boxType, 0);
                plugin.sendLocalized(
                        player,
                        "messages.counter-reset",
                        "&a%counter% wurden zurueckgesetzt.",
                        "%counter%", plugin.getCounterMessageLabel(boxType)
                );
                reopenMenu(player, Screen.BOX_MAIN, boxType);
            }
            return;
        }
        if (isConfiguredSlot(inventory, Screen.BOX_MAIN, "display-toggle", 23, slot)) {
            if (ensureWinsAdmin(player, boxType)) {
                plugin.toggleDisplay(boxType);
                plugin.sendLocalized(
                        player,
                        "messages.display-toggled",
                        "&aAnzeige umgeschaltet: &6%mode%",
                        "%mode%", plugin.getDisplaySummary()
                );
                reopenMenu(player, Screen.BOX_MAIN, boxType);
            }
            return;
        }
        if (isConfiguredSlot(inventory, Screen.BOX_MAIN, "close", 26, slot)) {
            player.closeInventory();
        }
    }

    private boolean handleBedrockActionClick(Player player, Inventory inventory, int slot) {
        if (isConfiguredSlot(inventory, Screen.BOX_MAIN, "teleport", 10, slot)) {
            runBoxCommand(player, BedrockWinCounterPlugin.BoxType.BEDROCK, "tp");
            return true;
        }
        if (isConfiguredSlot(inventory, Screen.BOX_MAIN, "main-user", 11, slot)) {
            runBoxCommand(player, BedrockWinCounterPlugin.BoxType.BEDROCK, "set_main_user " + player.getName());
            return true;
        }
        if (isConfiguredSlot(inventory, Screen.BOX_MAIN, "action-three", 12, slot)) {
            runBoxCommand(player, BedrockWinCounterPlugin.BoxType.BEDROCK, "toplock");
            return true;
        }
        if (isConfiguredSlot(inventory, Screen.BOX_MAIN, "action-four", 13, slot)) {
            runConsoleCommand("minecraft:attribute " + player.getName() + " minecraft:block_interaction_range base set 20");
            return true;
        }
        if (isConfiguredSlot(inventory, Screen.BOX_MAIN, "fill", 14, slot)) {
            runBoxCommand(player, BedrockWinCounterPlugin.BoxType.BEDROCK, "fill");
            return true;
        }
        if (isConfiguredSlot(inventory, Screen.BOX_MAIN, "clear", 15, slot)) {
            runBoxCommand(player, BedrockWinCounterPlugin.BoxType.BEDROCK, "clear");
            return true;
        }
        return false;
    }

    private boolean handleSandboxActionClick(Player player, Inventory inventory, int slot) {
        if (isConfiguredSlot(inventory, Screen.BOX_MAIN, "teleport", 10, slot)) {
            runBoxCommand(player, BedrockWinCounterPlugin.BoxType.SANDBOX, "tp");
            return true;
        }
        if (isConfiguredSlot(inventory, Screen.BOX_MAIN, "main-user", 11, slot)) {
            runBoxCommand(player, BedrockWinCounterPlugin.BoxType.SANDBOX, "set_main_user " + player.getName());
            return true;
        }
        if (isConfiguredSlot(inventory, Screen.BOX_MAIN, "action-three", 12, slot)) {
            runBoxCommand(player, BedrockWinCounterPlugin.BoxType.SANDBOX, "timer 10");
            return true;
        }
        if (isConfiguredSlot(inventory, Screen.BOX_MAIN, "action-four", 13, slot)) {
            runBoxCommand(player, BedrockWinCounterPlugin.BoxType.SANDBOX, "stop");
            return true;
        }
        if (isConfiguredSlot(inventory, Screen.BOX_MAIN, "fill", 14, slot)) {
            runBoxCommand(player, BedrockWinCounterPlugin.BoxType.SANDBOX, "fill");
            return true;
        }
        if (isConfiguredSlot(inventory, Screen.BOX_MAIN, "clear", 15, slot)) {
            runBoxCommand(player, BedrockWinCounterPlugin.BoxType.SANDBOX, "clear");
            return true;
        }
        return false;
    }

    private void handleTargetMenuClick(
            Player player,
            BedrockWinCounterPlugin.BoxType boxType,
            Inventory inventory,
            int slot
    ) {
        if (!ensureWinsAdmin(player, boxType)) {
            return;
        }

        if (isConfiguredSlot(inventory, Screen.TARGET, "minus-ten", 10, slot)) {
            adjustAndReopen(player, boxType, BedrockWinCounterPlugin.MenuValueType.TARGET, -10, Screen.TARGET);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.TARGET, "minus-five", 11, slot)) {
            adjustAndReopen(player, boxType, BedrockWinCounterPlugin.MenuValueType.TARGET, -5, Screen.TARGET);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.TARGET, "minus-one", 12, slot)) {
            adjustAndReopen(player, boxType, BedrockWinCounterPlugin.MenuValueType.TARGET, -1, Screen.TARGET);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.TARGET, "plus-one", 14, slot)) {
            adjustAndReopen(player, boxType, BedrockWinCounterPlugin.MenuValueType.TARGET, 1, Screen.TARGET);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.TARGET, "plus-five", 15, slot)) {
            adjustAndReopen(player, boxType, BedrockWinCounterPlugin.MenuValueType.TARGET, 5, Screen.TARGET);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.TARGET, "plus-ten", 16, slot)) {
            adjustAndReopen(player, boxType, BedrockWinCounterPlugin.MenuValueType.TARGET, 10, Screen.TARGET);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.TARGET, "reset", 18, slot)) {
            int resetValue = plugin.setMenuValue(player, boxType, BedrockWinCounterPlugin.MenuValueType.TARGET, 0);
            plugin.sendLocalized(
                    player,
                    "messages.menu-value-set",
                    "&7Menuewert &8(%type%&8) &7gesetzt auf: &6%value%",
                    "%type%", "target",
                    "%value%", Integer.toString(resetValue)
            );
            reopenMenu(player, Screen.TARGET, boxType);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.TARGET, "back", 20, slot)) {
            openBoxMenu(player, boxType);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.TARGET, "confirm", 22, slot)) {
            int value = plugin.getMenuValue(player, boxType, BedrockWinCounterPlugin.MenuValueType.TARGET);
            plugin.setTargetWins(boxType, value);
            plugin.sendLocalized(
                    player,
                    "messages.target-set",
                    "&aWin-Ziel fuer %box% gesetzt: &6%value%",
                    "%box%", plugin.getCounterDisplayName(boxType),
                    "%value%", Integer.toString(value)
            );
            openBoxMenu(player, boxType);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.TARGET, "close", 26, slot)) {
            player.closeInventory();
        }
    }

    private void handleChangeMenuClick(
            Player player,
            BedrockWinCounterPlugin.BoxType boxType,
            Inventory inventory,
            int slot
    ) {
        if (!ensureWinsAdmin(player, boxType)) {
            return;
        }

        if (isConfiguredSlot(inventory, Screen.CHANGE, "minus-ten", 10, slot)) {
            applyLiveChange(player, boxType, -10);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.CHANGE, "minus-five", 11, slot)) {
            applyLiveChange(player, boxType, -5);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.CHANGE, "minus-one", 12, slot)) {
            applyLiveChange(player, boxType, -1);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.CHANGE, "plus-one", 14, slot)) {
            applyLiveChange(player, boxType, 1);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.CHANGE, "plus-five", 15, slot)) {
            applyLiveChange(player, boxType, 5);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.CHANGE, "plus-ten", 16, slot)) {
            applyLiveChange(player, boxType, 10);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.CHANGE, "back", 22, slot)) {
            openBoxMenu(player, boxType);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.CHANGE, "close", 26, slot)) {
            player.closeInventory();
        }
    }

    private void handleSetMenuClick(
            Player player,
            BedrockWinCounterPlugin.BoxType boxType,
            Inventory inventory,
            int slot
    ) {
        if (!ensureWinsAdmin(player, boxType)) {
            return;
        }

        if (isConfiguredSlot(inventory, Screen.SET, "minus-ten", 10, slot)) {
            adjustAndReopen(player, boxType, BedrockWinCounterPlugin.MenuValueType.SET, -10, Screen.SET);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.SET, "minus-five", 11, slot)) {
            adjustAndReopen(player, boxType, BedrockWinCounterPlugin.MenuValueType.SET, -5, Screen.SET);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.SET, "minus-one", 12, slot)) {
            adjustAndReopen(player, boxType, BedrockWinCounterPlugin.MenuValueType.SET, -1, Screen.SET);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.SET, "plus-one", 14, slot)) {
            adjustAndReopen(player, boxType, BedrockWinCounterPlugin.MenuValueType.SET, 1, Screen.SET);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.SET, "plus-five", 15, slot)) {
            adjustAndReopen(player, boxType, BedrockWinCounterPlugin.MenuValueType.SET, 5, Screen.SET);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.SET, "plus-ten", 16, slot)) {
            adjustAndReopen(player, boxType, BedrockWinCounterPlugin.MenuValueType.SET, 10, Screen.SET);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.SET, "reset", 18, slot)) {
            int resetValue = plugin.setMenuValue(player, boxType, BedrockWinCounterPlugin.MenuValueType.SET, 0);
            plugin.sendLocalized(
                    player,
                    "messages.menu-value-set",
                    "&7Menuewert &8(%type%&8) &7gesetzt auf: &6%value%",
                    "%type%", "set",
                    "%value%", Integer.toString(resetValue)
            );
            reopenMenu(player, Screen.SET, boxType);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.SET, "back", 20, slot)) {
            openBoxMenu(player, boxType);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.SET, "confirm", 22, slot)) {
            int value = plugin.getMenuValue(player, boxType, BedrockWinCounterPlugin.MenuValueType.SET);
            plugin.setWins(boxType, value);
            plugin.sendLocalized(
                    player,
                    "messages.counter-set",
                    "&a%counter% gesetzt. Neuer Stand: &6%wins%",
                    "%counter%", plugin.getCounterMessageLabel(boxType),
                    "%wins%", Integer.toString(plugin.getWins(boxType))
            );
            openBoxMenu(player, boxType);
            return;
        }
        if (isConfiguredSlot(inventory, Screen.SET, "close", 26, slot)) {
            player.closeInventory();
        }
    }

    private void applyLiveChange(Player player, BedrockWinCounterPlugin.BoxType boxType, int delta) {
        plugin.addWins(boxType, delta);
        if (delta >= 0) {
            plugin.sendLocalized(
                    player,
                    "messages.counter-adjusted",
                    "&a%counter% angepasst. Neuer Stand: &6%wins%",
                    "%counter%", plugin.getCounterMessageLabel(boxType),
                    "%wins%", Integer.toString(plugin.getWins(boxType))
            );
        } else {
            plugin.sendLocalized(
                    player,
                    "messages.counter-reduced",
                    "&a%counter% reduziert. Neuer Stand: &6%wins%",
                    "%counter%", plugin.getCounterMessageLabel(boxType),
                    "%wins%", Integer.toString(plugin.getWins(boxType))
            );
        }
        reopenMenu(player, Screen.CHANGE, boxType);
    }

    private void adjustAndReopen(
            Player player,
            BedrockWinCounterPlugin.BoxType boxType,
            BedrockWinCounterPlugin.MenuValueType type,
            int delta,
            Screen screen
    ) {
        int newValue = plugin.adjustMenuValue(player, boxType, type, delta);
        plugin.sendLocalized(
                player,
                "messages.menu-value-now",
                "&7Menuewert &8(%type%&8) &7jetzt: &6%value%",
                "%type%", type.getKey(),
                "%value%", Integer.toString(newValue)
        );
        reopenMenu(player, screen, boxType);
    }

    private void reopenMenu(Player player, Screen screen, BedrockWinCounterPlugin.BoxType boxType) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            switch (screen) {
                case ROOT -> openMainMenu(player);
                case BOX_MAIN -> openBoxMenu(player, boxType);
                case TARGET -> openTargetMenu(player, boxType);
                case CHANGE -> openChangeMenu(player, boxType);
                case SET -> openSetMenu(player, boxType);
            }
        });
    }

    private boolean ensureMenuAccess(Player player, BedrockWinCounterPlugin.BoxType boxType) {
        if (plugin.hasMenuAccess(player, boxType)) {
            return true;
        }

        plugin.sendLocalized(player, "messages.menu-no-permission", "&cDu darfst dieses Menue nicht oeffnen.");
        return false;
    }

    private boolean ensureWinsAdmin(CommandSender sender, BedrockWinCounterPlugin.BoxType boxType) {
        if (plugin.hasAdminAccess(sender, boxType)) {
            return true;
        }

        plugin.sendLocalized(sender, "messages.no-permission", "&cDafuer fehlen dir die Rechte.");
        return false;
    }

    private void runBoxCommand(Player player, BedrockWinCounterPlugin.BoxType boxType, String subCommand) {
        String command = plugin.getBoxCommand(boxType);
        if (subCommand != null && !subCommand.isBlank()) {
            command += " " + subCommand;
        }
        Bukkit.dispatchCommand(player, command);
    }

    private void runConsoleCommand(String command) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    private Inventory createInventory(Screen screen, BedrockWinCounterPlugin.BoxType boxType, String title, int size) {
        return Bukkit.createInventory(new BedrockMenuHolder(screen, boxType), size, plugin.colorize(title));
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

    private String boxPath(BedrockWinCounterPlugin.BoxType boxType, String suffix) {
        return "menu." + boxType.getKey() + "." + suffix;
    }

    private Material getBoxMaterial(BedrockWinCounterPlugin.BoxType boxType, String suffix, Material fallback) {
        return plugin.getMenuMaterial("materials." + boxType.getKey() + "." + suffix, fallback);
    }

    private ItemStack createLocalizedHead(
            Player player,
            Material material,
            String namePath,
            String fallbackName,
            String lorePath,
            List<String> fallbackLore,
            String... replacements
    ) {
        ItemStack item = createLocalizedItem(
                material,
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
