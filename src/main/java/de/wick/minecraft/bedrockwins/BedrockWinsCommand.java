package de.wick.minecraft.bedrockwins;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public final class BedrockWinsCommand implements CommandExecutor, TabCompleter {

    private final BedrockWinCounterPlugin plugin;

    public BedrockWinsCommand(BedrockWinCounterPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!hasViewAccess(sender)) {
                plugin.sendMessage(sender, "&cDafuer fehlen dir die Rechte.");
                return true;
            }

            plugin.sendMessage(sender, "&fAktuelle Bedrock-Wins: &6" + plugin.getWinsDisplay());
            if (sender.hasPermission("bedrockwins.admin")) {
                plugin.sendMessage(sender, "&7Verfuegbar: /" + label + " add <zahl>, remove <zahl>, set <zahl>, reset, target <zahl|clear>, display, reload");
            }
            return true;
        }

        if (!hasAdminAccess(sender)) {
            plugin.sendMessage(sender, "&cDafuer fehlen dir die Rechte.");
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        switch (subCommand) {
            case "add" -> handleAdd(sender, args);
            case "remove", "minus", "sub" -> handleRemove(sender, args);
            case "set" -> handleSet(sender, args);
            case "reset" -> handleReset(sender);
            case "target", "goal" -> handleTarget(sender, args);
            case "reload" -> handleReload(sender);
            case "display" -> handleDisplay(sender, args);
            case "menu" -> handleMenu(sender, args);
            default -> plugin.sendMessage(sender, "&cUnbekannter Unterbefehl. Nutze: add, remove, set, reset, target, reload, display");
        }

        return true;
    }

    private void handleAdd(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.sendMessage(sender, "&cNutze: /bedrockwins add <zahl>");
            return;
        }

        Integer amount = parseInteger(args[1]);
        if (amount == null) {
            plugin.sendMessage(sender, "&cBitte eine gueltige Zahl angeben.");
            return;
        }

        plugin.addWins(amount);
        plugin.sendMessage(sender, "&aBedrock-Wins angepasst. Neuer Stand: &6" + plugin.getWins());
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.sendMessage(sender, "&cNutze: /bedrockwins remove <zahl>");
            return;
        }

        Integer amount = parseInteger(args[1]);
        if (amount == null) {
            plugin.sendMessage(sender, "&cBitte eine gueltige Zahl angeben.");
            return;
        }

        plugin.addWins(-amount);
        plugin.sendMessage(sender, "&aBedrock-Wins reduziert. Neuer Stand: &6" + plugin.getWins());
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.sendMessage(sender, "&cNutze: /bedrockwins set <zahl>");
            return;
        }

        Integer amount = parseInteger(args[1]);
        if (amount == null) {
            plugin.sendMessage(sender, "&cBitte eine gueltige Zahl angeben.");
            return;
        }

        plugin.setWins(amount);
        plugin.sendMessage(sender, "&aBedrock-Wins gesetzt. Neuer Stand: &6" + plugin.getWins());
    }

    private void handleReset(CommandSender sender) {
        plugin.setWins(0);
        plugin.sendMessage(sender, "&aBedrock-Wins wurden zurueckgesetzt.");
    }

    private void handleTarget(CommandSender sender, String[] args) {
        if (args.length < 2) {
            String currentTarget = plugin.getTargetWins() == null ? "nicht gesetzt" : Integer.toString(plugin.getTargetWins());
            plugin.sendMessage(sender, "&7Aktuelles Ziel: &6" + currentTarget + "&7. Nutze: /bedrockwins target <zahl|clear>");
            return;
        }

        String value = args[1].toLowerCase(Locale.ROOT);
        if (value.equals("clear") || value.equals("off") || value.equals("none")) {
            plugin.setTargetWins(null);
            plugin.sendMessage(sender, "&aWin-Ziel entfernt.");
            return;
        }

        Integer target = parseInteger(args[1]);
        if (target == null || target < 0) {
            plugin.sendMessage(sender, "&cBitte eine gueltige Ziel-Zahl ab 0 angeben.");
            return;
        }

        plugin.setTargetWins(target);
        plugin.sendMessage(sender, "&aWin-Ziel gesetzt: &6" + target);
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadPluginState();
        plugin.sendMessage(sender, "&aBedrockWinCounter wurde neu geladen.");
    }

    private void handleDisplay(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.sendMessage(
                    sender,
                    "&7Anzeige aktuell: &6" + plugin.getDisplayModeLabel() + "&7. Nutze: /bedrockwins display <on|off|toggle|bossbar|sidebar>"
            );
            return;
        }

        String value = args[1].toLowerCase(Locale.ROOT);
        switch (value) {
            case "on" -> {
                plugin.setDisplayMode(BedrockWinCounterPlugin.DisplayMode.BOSSBAR);
                plugin.sendMessage(sender, "&aAnzeige aktiviert: &6bossbar");
            }
            case "off" -> {
                plugin.setDisplayMode(BedrockWinCounterPlugin.DisplayMode.NONE);
                plugin.sendMessage(sender, "&aAnzeige deaktiviert.");
            }
            case "toggle" -> {
                plugin.toggleDisplay();
                plugin.sendMessage(sender, "&aAnzeige umgeschaltet: &6" + plugin.getDisplayModeLabel());
            }
            case "bossbar" -> {
                plugin.setDisplayMode(BedrockWinCounterPlugin.DisplayMode.BOSSBAR);
                plugin.sendMessage(sender, "&aAnzeige gesetzt: &6bossbar");
            }
            case "sidebar", "scoreboard" -> {
                plugin.setDisplayMode(BedrockWinCounterPlugin.DisplayMode.SIDEBAR);
                plugin.sendMessage(sender, "&aAnzeige gesetzt: &6sidebar");
            }
            default -> plugin.sendMessage(sender, "&cNutze: /bedrockwins display <on|off|toggle|bossbar|sidebar>");
        }
    }

    private void handleMenu(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.sendMessage(sender, "&cDieser Unterbefehl ist nur ingame nutzbar.");
            return;
        }

        if (args.length < 3) {
            plugin.sendMessage(sender, "&cNutze: /bedrockwins menu <target|change|set> <init|adjust|reset|apply> [zahl]");
            return;
        }

        BedrockWinCounterPlugin.MenuValueType type = BedrockWinCounterPlugin.MenuValueType.fromKey(args[1]);
        if (type == null) {
            plugin.sendMessage(sender, "&cUnbekannter Menü-Typ. Nutze: target, change oder set.");
            return;
        }

        String action = args[2].toLowerCase(Locale.ROOT);
        switch (action) {
            case "init" -> {
                int value = args.length >= 4 ? parseIntegerOrDefault(args[3], type.getDefaultValue()) : type.getDefaultValue();
                int initializedValue = plugin.setMenuValue(player, type, value);
                plugin.sendMessage(sender, "&7Menuewert &8(" + type.getKey() + "&8) &7gesetzt auf: &6" + initializedValue);
            }
            case "adjust" -> {
                if (args.length < 4) {
                    plugin.sendMessage(sender, "&cNutze: /bedrockwins menu " + type.getKey() + " adjust <zahl>");
                    return;
                }

                Integer delta = parseInteger(args[3]);
                if (delta == null) {
                    plugin.sendMessage(sender, "&cBitte eine gueltige Zahl angeben.");
                    return;
                }

                int newValue = plugin.adjustMenuValue(player, type, delta);
                plugin.sendMessage(sender, "&7Menuewert &8(" + type.getKey() + "&8) &7jetzt: &6" + newValue);
            }
            case "reset" -> {
                int resetValue = plugin.resetMenuValue(player, type);
                plugin.sendMessage(sender, "&7Menuewert &8(" + type.getKey() + "&8) &7zurueckgesetzt auf: &6" + resetValue);
            }
            case "apply" -> applyMenuValue(player, type);
            default -> plugin.sendMessage(sender, "&cNutze: /bedrockwins menu <target|change|set> <init|adjust|reset|apply> [zahl]");
        }
    }

    private void applyMenuValue(Player player, BedrockWinCounterPlugin.MenuValueType type) {
        int value = plugin.getMenuValue(player, type);
        switch (type) {
            case TARGET -> {
                plugin.setTargetWins(value);
                plugin.sendMessage(player, "&aWin-Ziel gesetzt: &6" + value);
            }
            case CHANGE -> {
                plugin.addWins(value);
                plugin.sendMessage(player, "&aBedrock-Wins angepasst. Neuer Stand: &6" + plugin.getWins());
            }
            case ADD -> {
                plugin.addWins(value);
                plugin.sendMessage(player, "&aBedrock-Wins angepasst. Neuer Stand: &6" + plugin.getWins());
            }
            case REMOVE -> {
                plugin.addWins(-value);
                plugin.sendMessage(player, "&aBedrock-Wins reduziert. Neuer Stand: &6" + plugin.getWins());
            }
            case SET -> {
                plugin.setWins(value);
                plugin.sendMessage(player, "&aBedrock-Wins gesetzt. Neuer Stand: &6" + plugin.getWins());
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

    private boolean hasViewAccess(CommandSender sender) {
        if (!(sender instanceof Player)) {
            return true;
        }

        return sender.hasPermission("bedrockwins.view");
    }

    private boolean hasAdminAccess(CommandSender sender) {
        if (!(sender instanceof Player)) {
            return true;
        }

        return sender.hasPermission("bedrockwins.admin");
    }

    private int parseIntegerOrDefault(String input, int fallback) {
        Integer value = parseInteger(input);
        return value == null ? fallback : value;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (!sender.hasPermission("bedrockwins.admin")) {
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
            for (String value : List.of("clear")) {
                if (value.startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    suggestions.add(value);
                }
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
