package dev.kshl.kshlib.velocity;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.Scheduler;
import dev.kshl.kshlib.kyori.ComponentHelper;
import dev.kshl.kshlib.yaml.YamlConfig;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public interface IBaseVelocityPlugin extends BaseCommand.AsyncRunner {

    String getID();

    default void saveConfig() throws IOException {
        getConfig().save();
    }

    ProxyServer getProxy();

    Logger getLogger();

    Path getDataDirectory();

    YamlConfig getConfig();

    boolean isDebug();

    void setDebug(boolean debug);

    default PluginContainer getPluginContainer() {
        return getProxy().getPluginManager().getPlugin(getID()).orElseThrow();
    }

    default String getVersion() {
        return getPluginContainer().getDescription().getVersion().orElse("???");
    }

    @Override
    default void runAsync(Runnable runnable) {
        schedule(runnable, null, null, null);
    }

    default void runLater(Runnable runnable, long delay, TimeUnit timeUnit) {
        schedule(runnable, delay, null, timeUnit);
    }

    default void runTimer(Runnable runnable, long delay, long interval, TimeUnit timeUnit) {
        schedule(runnable, delay, interval, timeUnit);
    }

    private void schedule(Runnable runnable, Long delay, Long interval, TimeUnit timeUnit) {
        Scheduler.TaskBuilder taskBuilder = getProxy().getScheduler().buildTask(this, () -> {
            try {
                runnable.run();
            } catch (Throwable t) {
                print("Unhandled exception in async task", t);
            }
        });
        if (delay != null && timeUnit != null) {
            taskBuilder.delay(delay, timeUnit);
        }
        if (interval != null && timeUnit != null) {
            taskBuilder.repeat(interval, timeUnit);
        }
        taskBuilder.schedule();
    }

    default InputStream getResourceAsStream(String resource) {
        return getClass().getClassLoader().getResourceAsStream(resource);
    }

    default void print(String explanation, Throwable t) {
        getLogger().warn("{}\n{}", explanation, t.getMessage(), t);
    }

    default void info(String msg) {
        getLogger().info(msg);
    }

    default void warning(String msg) {
        getLogger().warn(msg);
    }

    default void debug(String msg) {
        if (isDebug()) info("[DEBUG] " + msg);
    }

    default void runConsoleCommand(String command) {
        getProxy().getCommandManager().executeAsync(getProxy().getConsoleCommandSource(), command);
    }

    default void broadcast(String msg) {
        broadcast(msg, null);
    }

    default void broadcast(Component msg) {
        broadcast(msg, null);
    }

    default void broadcast(String msg, @Nullable String node) {
        broadcast(ComponentHelper.legacy(msg), node);
    }

    default void broadcast(Component msg, @Nullable String node) {
        getProxy().getAllPlayers().stream().filter(p -> node == null || p.hasPermission(node)).forEach(p -> p.sendMessage(msg));
        getProxy().getConsoleCommandSource().sendMessage(msg);
    }
}
