package dev.kshl.kshlib.velocity;

import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public abstract class BaseCommand implements SimpleCommand, Executable {
    @Nullable
    protected final String node;
    @Nullable
    private final Consumer<Runnable> runAsync;

    public BaseCommand(@Nullable String node, @Nullable Consumer<Runnable> runAsync) {
        this.node = node;
        this.runAsync = runAsync;
    }

    public BaseCommand(@Nullable String node, @Nullable AsyncRunner runner) {
        this.node = node;
        this.runAsync = runner == null ? null : runner::runAsync;
    }

    @Override
    public final void execute(Invocation invocation) {
        if (!hasPermission(invocation)) {
            invocation.source().sendMessage(getNoPermissionMessage());
            return;
        }
        Runnable run = () -> execute(invocation.source(), invocation.arguments(), invocation.alias());
        if (runAsync != null) runAsync.accept(run);
        else run.run();
    }

    @Override
    public final List<String> suggest(Invocation invocation) {
        if (!hasPermission(invocation)) {
            return List.of();
        }

        List<String> output = tabComplete(invocation.source(), invocation.arguments(), invocation.alias());
        if (output == null) {
            output = tabCompleteStartsWith(invocation.source(), invocation.arguments(), invocation.alias());
            if (output != null && output.size() < 1000) {
                output = new ArrayList<>(output);
            }
            if (output != null) {
                String token = invocation.arguments().length == 0 ? "" : invocation.arguments()[invocation.arguments().length - 1].toLowerCase();
                output.removeIf(word -> !word.toLowerCase().startsWith(token));
            }
        }

        return output;
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        return SimpleCommand.super.suggestAsync(invocation);
    }

    @Override
    public final boolean hasPermission(Invocation invocation) {
        if (node == null) return true;
        return invocation.source().hasPermission(node);
    }


    public interface AsyncRunner {
        void runAsync(Runnable runnable);
    }

    protected Component getNoPermissionMessage() {
        return Component.text("You do not have permission for this command.", NamedTextColor.RED);
    }
}
