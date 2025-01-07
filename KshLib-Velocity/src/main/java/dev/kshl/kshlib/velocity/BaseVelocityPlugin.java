package dev.kshl.kshlib.velocity;

import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.kshl.kshlib.misc.UUIDHelper;
import dev.kshl.kshlib.yaml.YamlConfig;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

public class BaseVelocityPlugin implements IBaseVelocityPlugin {

    public String getID() {
        return id;
    }

    private final String id;
    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;
    private final File configFile;
    private boolean debug = false;

    private YamlConfig config;

    protected BaseVelocityPlugin(String id, ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.id = id;
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.configFile = new File(getDataDirectory().toFile(), "config.yml");
    }

    public void loadConfig() {
        try {
            config = new YamlConfig(configFile, () -> getResourceAsStream("config.yml")).load();
        } catch (IOException e) {
            print("Failed to load config", e);
        }

        setDebug(getConfig().getBoolean("debug").orElse(false));
    }

    public ProxyServer getProxy() {
        return proxy;
    }

    public Logger getLogger() {
        return logger;
    }

    public Path getDataDirectory() {
        return dataDirectory;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public YamlConfig getConfig() {
        return config;
    }

    public static String getUsername(CommandSource commandSource) {
        if (commandSource instanceof Player player) {
            return player.getUsername();
        } else if (commandSource instanceof ConsoleCommandSource) {
            return "CONSOLE";
        }
        throw new IllegalArgumentException();
    }

    public static UUID getUUID(CommandSource commandSource) {
        if (commandSource instanceof Player player) {
            return player.getUniqueId();
        } else if (commandSource instanceof ConsoleCommandSource) {
            return UUIDHelper.ZERO;
        }
        throw new IllegalArgumentException();
    }

    protected CommandMeta getMeta(String command, String... aliases) {
        CommandMeta.Builder builder = getProxy().getCommandManager().metaBuilder(command);
        builder.plugin(this);
        builder.aliases(aliases);
        return builder.build();
    }
}
