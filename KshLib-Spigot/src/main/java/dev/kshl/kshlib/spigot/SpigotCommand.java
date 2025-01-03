package dev.kshl.kshlib.spigot;

import dev.kshl.kshlib.parsing.Command;
import dev.kshl.kshlib.parsing.CommandUtil;
import dev.kshl.kshlib.parsing.Sender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;
import java.util.Set;

@Deprecated
public class SpigotCommand extends Command implements CommandExecutor, TabCompleter {
    public SpigotCommand(String command, String node, @Nullable Set<Command> subCommands) {
        super(command, node, subCommands);
    }

    public SpigotCommand(String command, String node) {
        super(command, node);
    }

    @Override
    public boolean onCommand(@Nonnull CommandSender commandSender, @Nonnull org.bukkit.command.Command command, @Nonnull String s, @Nonnull String[] args) {
        super.onCommand(new SpigotSender(commandSender), CommandUtil.concat(args));
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@Nonnull CommandSender commandSender, @Nonnull org.bukkit.command.Command command, @Nonnull String s, @Nonnull String[] args) {
        return super.onTabComplete(new SpigotSender(commandSender), CommandUtil.concat(args));
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
            sender.sendMessage(message);
        }
    }
}
