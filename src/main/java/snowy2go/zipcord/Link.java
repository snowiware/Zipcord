package snowy2go.zipcord;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Link implements CommandExecutor {
    Zipcord plugin;

    public Link(Zipcord instance) {
        plugin = instance;
    }

    // This method is called, when somebody uses our command
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            String code = args[0];

            // 1. Find tags from code
            String tags = plugin.getTagsFromCode(code);

            // 2. Remove code if self-destructive
            if (plugin.isSelfDestructive(code)) {
                plugin.destroyCode(code);
            }

            // 3. Apply tags from code to sender
            boolean worked = plugin.addTagsToPlayer(String.valueOf(player.getUniqueId()),tags);

            // 4. Clear all expired codes
            plugin.clearDeadCodes();

            if (worked) {
                player.sendMessage(ChatColor.GREEN + "Successfully linked!");
            } else {
                player.sendMessage(ChatColor.GRAY + "Code was not found.");
            }

            return true;
        }
        return false;
    }
}