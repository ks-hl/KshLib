package dev.kshl.kshlib.bungee;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

public class BungeeCommandBridge extends Command implements TabExecutor {
    public BungeeCommandBridge(String name, String permission, String... aliases) {
        super(name, permission, aliases);
    }

    @Override
    public void execute(CommandSender commandSender, String[] strings) {

    }

    @Override
    public Iterable<String> onTabComplete(CommandSender commandSender, String[] strings) {

        return null;
    }
}
