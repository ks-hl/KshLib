package dev.kshl.kshlib.bungee;

import dev.kshl.kshlib.parsing.Command;
import dev.kshl.kshlib.parsing.Sender;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;

import javax.annotation.Nullable;
import java.util.Set;

@Deprecated
public class BungeeCommand extends Command {
    public BungeeCommand(String command, String node, @Nullable Set<Command> subCommands) {
        super(command, node, subCommands);
    }

    public BungeeCommand(String command, String node) {
        super(command, node);
    }

    public static class SpigotSender implements Sender {
        private final CommandSender sender;

        public SpigotSender(CommandSender sender) {
            this.sender = sender;
        }

        @Override
        public boolean hasPermission(String node) {
            return sender.hasPermission(node);
        }

        @Override
        public void sendMessage(String message) {
            sender.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', message)));

        }
    }
}
