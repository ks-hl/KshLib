package dev.kshl.kshlib.parsing;

import javax.annotation.Nullable;

import java.util.*;

@Deprecated
public abstract class Command {
    private final String command;
    private final String node;
    private final String[] aliases;
    private final Map<String, Command> subCommands = new HashMap<>();

    public Command(String command, String node, @Nullable Set<Command> subCommands, String... aliases) {
        this.command = command;
        this.node = node;
        this.aliases = aliases;
        if (subCommands != null) subCommands.forEach(this::add);
    }

    public Command(String command, String node, String... aliases) {
        this(command, node, null, aliases);
    }

    public void onCommand(Sender sender, String command) {
        if (!hasPermission(sender)) {
            sendNoPermission(sender);
            return;
        }
        String[] args = command.split(" ");
        Command subCommand = subCommands.get(args[0]);
        if (subCommand == null) {
            sendUnknownSubcommand(sender, args[0]);
            return;
        }
        subCommand.onCommand(sender, CommandUtil.skipFirstArg(command));
    }

    public List<String> onTabComplete(Sender sender, String command) {
        String[] args = command.split(" ");
        if (args.length == 1) {
            List<String> out = new ArrayList<>();
            String start = args[0].toLowerCase();
            for (Command subCommand : subCommands.values()) {
                if (subCommand.hasPermission(sender)) {
                    if (subCommand.command.toLowerCase().startsWith(start)) {
                        out.add(subCommand.command);
                    }
                }
            }
            return out;
        } else {
            Command subCommand = subCommands.get(args[0]);
            if (subCommand != null) return subCommand.onTabComplete(sender, CommandUtil.skipFirstArg(command));
            return List.of();
        }
    }

    protected final Command add(Command subCommand) {
        subCommands.put(subCommand.command, subCommand);
        if (subCommand.aliases != null) for (String alias : subCommand.aliases) {
            subCommands.put(alias, subCommand);
        }
        return this;
    }

    private boolean hasPermission(Sender sender) {
        return node == null || sender.hasPermission(node);
    }

    protected void sendNoPermission(Sender sender) {
        sender.sendMessage("No permission.");
    }

    protected void sendInvalidSyntax(Sender sender) {
        sender.sendMessage("Invalid syntax.");
    }

    protected void sendUnknownSubcommand(Sender sender, String sub) {
        sender.sendMessage("Unknown subcommand: " + sub);
    }

}
