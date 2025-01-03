package dev.kshl.kshlib.velocity;

import com.velocitypowered.api.command.CommandSource;

import java.util.List;

interface Executable {
    void execute(CommandSource sender, String[] args, String alias);

    default List<String> tabComplete(CommandSource source, String[] args, String alias) {
        return null;
    }

    default List<String> tabCompleteStartsWith(CommandSource source, String[] args, String alias) {
        return null;
    }
}
