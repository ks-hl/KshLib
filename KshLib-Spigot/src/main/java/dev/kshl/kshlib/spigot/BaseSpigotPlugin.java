package dev.kshl.kshlib.spigot;

import dev.kshl.kshlib.yaml.YamlConfig;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class BaseSpigotPlugin extends JavaPlugin {

    private final YamlConfig debug;

    protected BaseSpigotPlugin() {
        File debugFile = new File(getDataFolder(), "debug.yml");
        try {
            //noinspection ResultOfMethodCallIgnored
            debugFile.getParentFile().mkdirs();
            //noinspection ResultOfMethodCallIgnored
            debugFile.createNewFile();
            this.debug = new YamlConfig(debugFile, null);
            this.debug.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isDebug() {
        return debug.getBoolean("debug").orElse(false);
    }

    public void setDebug(boolean debug) {
        this.debug.set("debug", debug);
        try {
            this.debug.save();
        } catch (IOException e) {
            print("Failed to save debug state", e);
        }
    }

    @Deprecated
    public void print(Throwable t, String message) {
        print(message, t);
    }

    public void print(String message, Throwable t) {
        getLogger().log(Level.WARNING, t.getMessage() + ", " + message, t);
    }

    public void info(String msg) {
        getLogger().info(msg);
    }

    public void warning(String msg) {
        getLogger().warning(msg);
    }

    public void debug(String msg) {
        if (isDebug()) info("[DEBUG] " + msg);
    }

    public void runAsync(Runnable runnable) {
        getServer().getScheduler().runTaskAsynchronously(this, runnable);
    }

    public void runSync(Runnable runnable) {
        getServer().getScheduler().runTask(this, runnable);
    }
}
