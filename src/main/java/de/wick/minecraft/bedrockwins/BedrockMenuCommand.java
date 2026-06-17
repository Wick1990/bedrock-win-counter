package de.wick.minecraft.bedrockwins;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class BedrockMenuCommand implements CommandExecutor {

    private final BedrockWinCounterPlugin plugin;
    private final BedrockMenuGui menuGui;

    public BedrockMenuCommand(BedrockWinCounterPlugin plugin, BedrockMenuGui menuGui) {
        this.plugin = plugin;
        this.menuGui = menuGui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.sendLocalized(sender, "messages.only-player", "&cDieses Menue ist nur ingame nutzbar.");
            return true;
        }

        if (!plugin.canOpenAnyMenu(player)) {
            plugin.sendLocalized(player, "messages.menu-no-permission", "&cDu darfst dieses Menue nicht oeffnen.");
            return true;
        }

        menuGui.openMainMenu(player);
        return true;
    }
}
