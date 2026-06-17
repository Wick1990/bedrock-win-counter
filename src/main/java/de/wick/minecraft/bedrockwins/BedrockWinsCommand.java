package de.wick.minecraft.bedrockwins;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class BedrockWinsCommand implements CommandExecutor, TabCompleter {

    private final BedrockWinCounterPlugin plugin;
    private final BedrockWinCounterPlugin.BoxType boxType;

    public BedrockWinsCommand(BedrockWinCounterPlugin plugin, BedrockWinCounterPlugin.BoxType boxType) {
        this.plugin = plugin;
        this.boxType = boxType;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!plugin.hasViewAccess(sender, boxType)) {
                plugin.sendLocalized(sender, "messages.no-permission", "&cDafuer fehlen dir die Rechte.");
                return true;
            }

            plugin.sendLocalized(
                    sender,
                    "messages.counter-current",
                    "&fAktuelle %counter%: &6%wins%",
                    "%counter%", plugin.getCounterMessageLabel(boxType),
                    "%wins%", plugin.getWinsDisplay(boxType)
            );
            if (plugin.hasAdminAccess(sender, boxType)) {
                plugin.sendLocalized(
                        sender,
                        "messages.admin-hint",
                        "&7Verfuegbar: /%command% add <zahl>, remove <zahl>, set <zahl>, reset, target <zahl|clear>, reload, display",
                        "%command%", label
                );
            }
            return true;
        }

        if (!plugin.hasAdminAccess(sender, boxType)) {
            plugin.sendLocalized(sender, "messages.no-permission", "&cDafuer fehlen dir die Rechte.");
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        switch (subCommand) {
            case "add" -> handleAdd(sender, label, args);
            case "remove", "minus", "sub" -> handleRemove(sender, label, args);
            case "set" -> handleSet(sender, label, args);
            case "reset" -> handleReset(sender);
            case "target", "goal" -> handleTarget(sender, label, args);
            case "reload" -> handleReload(sender);
            case "display" -> handleDisplay(sender, label, args);
            case "menu" -> handleMenu(sender, label, args);
            default -> plugin.sendLocalized(
                    sender,
                    "messages.unknown-subcommand",
                    "&cUnbekannter Unterbefehl. Nutze: add, remove, set, reset, target, reload, display, menu"
            );
        }

        return true;
    }

    private void handleAdd(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            plugin.sendMessage(sender, "&cNutze: /" + label + " add <zahl>");
            return;
        }

        Integer amount = parseInteger(args[1]);
        if (amount == null) {
            plugin.sendLocalized(sender, "messages.invalid-number", "&cBitte eine gueltige Zahl angeben.");
            return;
        }

        plugin.addWins(boxType, amount);
        plugin.sendLocalized(
                sender,
                "messages.counter-adjusted",
                "&a%counter% angepasst. Neuer Stand: &6%wins%",
                "%counter%", plugin.getCounterMessageLabel(boxType),
                "%wins%", Integer.toString(plugin.getWins(boxType))
        );
    }

    private void handleRemove(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            plugin.sendMessage(sender, "&cNutze: /" + label + " remove <zahl>");
            return;
        }

        Integer amount = parseInteger(args[1]);
        if (amount == null) {
            plugin.sendLocalized(sender, "messages.invalid-number", "&cBitte eine gueltige Zahl angeben.");
            return;
        }

        plugin.addWins(boxType, -amount);
        plugin.sendLocalized(
                sender,
                "messages.counter-reduced",
                "&a%counter% reduziert. Neuer Stand: &6%wins%",
                "%counter%", plugin.getCounterMessageLabel(boxType),
                "%wins%", Integer.toString(plugin.getWins(boxType))
        );
    }

    private void handleSet(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            plugin.sendMessage(sender, "&cNutze: /" + label + " set <zahl>");
            return;
        }

        Integer amount = parseInteger(args[1]);
        if (amount == null) {
            plugin.sendLocalized(sender, "messages.invalid-number", "&cBitte eine gueltige Zahl angeben.");
            return;
        }

        plugin.setWins(boxType, amount);
        plugin.sendLocalized(
                sender,
                "messages.counter-set",
                "&a%counter% gesetzt. Neuer Stand: &6%wins%",
                "%counter%", plugin.getCounterMessageLabel(boxType),
                "%wins%", Integer.toString(plugin.getWins(boxType))
        );
    }

    private void handleReset(CommandSender sender) {
        plugin.setWins(boxType, 0);
        plugin.sendLocalized(
                sender,
                "messages.counter-reset",
                "&a%counter% wurden zurueckgesetzt.",
                "%counter%", plugin.getCounterMessageLabel(boxType)
        );
    }

    private void handleTarget(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            String currentTarget = plugin.getTargetWins(boxType) == null ? "nicht gesetzt" : Integer.toString(plugin.getTargetWins(boxType));
            plugin.sendLocalized(
                    sender,
                    "messages.target-status",
                    "&7Aktuelles Ziel fuer %box%: &6%value%&7. Nutze: /%command% target <zahl|clear>",
                    "%box%", plugin.getCounterDisplayName(boxType),
                    "%value%", currentTarget,
                    "%command%", label
            );
            return;
        }

        String value = args[1].toLowerCase(Locale.ROOT);
        if (value.equals("clear") || value.equals("off") || value.equals("none")) {
            plugin.setTargetWins(boxType, null);
            plugin.sendLocalized(
                    sender,
                    "messages.target-cleared",
                    "&aWin-Ziel fuer %box% entfernt.",
                    "%box%", plugin.getCounterDisplayName(boxType)
            );
            return;
        }

        Integer target = parseInteger(args[1]);
        if (target == null || target < 0) {
            plugin.sendLocalized(sender, "messages.invalid-target", "&cBitte eine gueltige Ziel-Zahl ab 0 angeben.");
            return;
        }

        plugin.setTargetWins(boxType, target);
        plugin.sendLocalized(
                sender,
                "messages.target-set",
                "&aWin-Ziel fuer %box% gesetzt: &6%value%",
                "%box%", plugin.getCounterDisplayName(boxType),
                "%value%", Integer.toString(target)
        );
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadPluginState();
        plugin.sendLocalized(sender, "messages.plugin-reloaded", "&aBedrockWinCounter wurde neu geladen.");
    }

    private void handleDisplay(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            plugin.sendLocalized(
                    sender,
                    "messages.display-status",
                    "&7Anzeige aktuell: &6%mode%&7. Nutze: /%command% display <on|off|toggle|bossbar|sidebar>",
                    "%mode%", plugin.getDisplaySummary(),
                    "%command%", label
            );
            return;
        }

        String value = args[1].toLowerCase(Locale.ROOT);
        switch (value) {
            case "on" -> {
                plugin.setDisplayMode(boxType, BedrockWinCounterPlugin.DisplayMode.BOSSBAR);
                plugin.sendLocalized(
                        sender,
                        "messages.display-set",
                        "&aAnzeige gesetzt: &6%mode%",
                        "%mode%", plugin.getDisplaySummary()
                );
            }
            case "off" -> {
                plugin.setDisplayMode(boxType, BedrockWinCounterPlugin.DisplayMode.NONE);
                plugin.sendLocalized(sender, "messages.display-disabled", "&aAnzeige deaktiviert.");
            }
            case "toggle" -> {
                plugin.toggleDisplay(boxType);
                plugin.sendLocalized(
                        sender,
                        "messages.display-toggled",
                        "&aAnzeige umgeschaltet: &6%mode%",
                        "%mode%", plugin.getDisplaySummary()
                );
            }
            case "bossbar" -> {
                plugin.setDisplayMode(boxType, BedrockWinCounterPlugin.DisplayMode.BOSSBAR);
                plugin.sendLocalized(
                        sender,
                        "messages.display-set",
                        "&aAnzeige gesetzt: &6%mode%",
                        "%mode%", plugin.getDisplaySummary()
                );
            }
            case "sidebar", "scoreboard" -> {
                plugin.setDisplayMode(boxType, BedrockWinCounterPlugin.DisplayMode.SIDEBAR);
                plugin.sendLocalized(
                        sender,
                        "messages.display-set",
                        "&aAnzeige gesetzt: &6%mode%",
                        "%mode%", plugin.getDisplaySummary()
                );
            }
            default -> plugin.sendMessage(sender, "&cNutze: /" + label + " display <on|off|toggle|bossbar|sidebar>");
        }
    }

    private void handleMenu(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.sendLocalized(sender, "messages.only-player", "&cDieses Menue ist nur ingame nutzbar.");
            return;
        }

        if (args.length < 3) {
            plugin.sendMessage(sender, "&cNutze: /" + label + " menu <target|change|set> <init|adjust|reset|apply> [zahl]");
            return;
        }

        BedrockWinCounterPlugin.MenuValueType type = BedrockWinCounterPlugin.MenuValueType.fromKey(args[1]);
        if (type == null) {
            plugin.sendLocalized(
                    sender,
                    "messages.invalid-menu-type",
                    "&cUnbekannter Menue-Typ. Nutze: target, change oder set."
            );
            return;
        }

        String action = args[2].toLowerCase(Locale.ROOT);
        switch (action) {
            case "init" -> {
                int value = args.length >= 4 ? parseIntegerOrDefault(args[3], type.getDefaultValue()) : type.getDefaultValue();
                int initializedValue = plugin.setMenuValue(player, boxType, type, value);
                plugin.sendLocalized(
                        sender,
                        "messages.menu-value-set",
                        "&7Menuewert &8(%type%&8) &7gesetzt auf: &6%value%",
                        "%type%", type.getKey(),
                        "%value%", Integer.toString(initializedValue)
                );
            }
            case "adjust" -> {
                if (args.length < 4) {
                    plugin.sendMessage(sender, "&cNutze: /" + label + " menu " + type.getKey() + " adjust <zahl>");
                    return;
                }

                Integer delta = parseInteger(args[3]);
                if (delta == null) {
                    plugin.sendLocalized(sender, "messages.invalid-number", "&cBitte eine gueltige Zahl angeben.");
                    return;
                }

                int newValue = plugin.adjustMenuValue(player, boxType, type, delta);
                plugin.sendLocalized(
                        sender,
                        "messages.menu-value-now",
                        "&7Menuewert &8(%type%&8) &7jetzt: &6%value%",
                        "%type%", type.getKey(),
                        "%value%", Integer.toString(newValue)
                );
            }
            case "reset" -> {
                int resetValue = plugin.resetMenuValue(player, boxType, type);
                plugin.sendLocalized(
                        sender,
                        "messages.menu-value-set",
                        "&7Menuewert &8(%type%&8) &7gesetzt auf: &6%value%",
                        "%type%", type.getKey(),
                        "%value%", Integer.toString(resetValue)
                );
            }
            case "apply" -> applyMenuValue(player, type);
            default -> plugin.sendMessage(sender, "&cNutze: /" + label + " menu <target|change|set> <init|adjust|reset|apply> [zahl]");
        }
    }

    private void applyMenuValue(Player player, BedrockWinCounterPlugin.MenuValueType type) {
        int value = plugin.getMenuValue(player, boxType, type);
        switch (type) {
            case TARGET -> {
                plugin.setTargetWins(boxType, value);
                plugin.sendLocalized(
                        player,
                        "messages.target-set",
                        "&aWin-Ziel fuer %box% gesetzt: &6%value%",
                        "%box%", plugin.getCounterDisplayName(boxType),
                        "%value%", Integer.toString(value)
                );
            }
            case CHANGE -> {
                plugin.addWins(boxType, value);
                if (value < 0) {
                    plugin.sendLocalized(
                            player,
                            "messages.counter-reduced",
                            "&a%counter% reduziert. Neuer Stand: &6%wins%",
                            "%counter%", plugin.getCounterMessageLabel(boxType),
                            "%wins%", Integer.toString(plugin.getWins(boxType))
                    );
                } else {
                    plugin.sendLocalized(
                            player,
                            "messages.counter-adjusted",
                            "&a%counter% angepasst. Neuer Stand: &6%wins%",
                            "%counter%", plugin.getCounterMessageLabel(boxType),
                            "%wins%", Integer.toString(plugin.getWins(boxType))
                    );
                }
            }
            case SET -> {
                plugin.setWins(boxType, value);
                plugin.sendLocalized(
                        player,
                        "messages.counter-set",
                        "&a%counter% gesetzt. Neuer Stand: &6%wins%",
                        "%counter%", plugin.getCounterMessageLabel(boxType),
                        "%wins%", Integer.toString(plugin.getWins(boxType))
                );
            }
        }
    }

    private Integer parseInteger(String input) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private int parseIntegerOrDefault(String input, int fallback) {
        Integer value = parseInteger(input);
        return value == null ? fallback : value;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (!plugin.hasAdminAccess(sender, boxType)) {
            return suggestions;
        }

        if (args.length == 1) {
            for (String subCommand : List.of("add", "remove", "set", "reset", "target", "reload", "display", "menu")) {
                if (subCommand.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    suggestions.add(subCommand);
                }
            }
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("display")) {
            for (String value : List.of("on", "off", "toggle", "bossbar", "sidebar")) {
                if (value.startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    suggestions.add(value);
                }
            }
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("target")) {
            if ("clear".startsWith(args[1].toLowerCase(Locale.ROOT))) {
                suggestions.add("clear");
            }
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("menu")) {
            for (String value : List.of("target", "change", "set")) {
                if (value.startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    suggestions.add(value);
                }
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("menu")) {
            for (String value : List.of("init", "adjust", "reset", "apply")) {
                if (value.startsWith(args[2].toLowerCase(Locale.ROOT))) {
                    suggestions.add(value);
                }
            }
        }

        return suggestions;
    }
}
