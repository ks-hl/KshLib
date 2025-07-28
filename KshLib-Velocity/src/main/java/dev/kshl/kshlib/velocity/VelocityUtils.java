package dev.kshl.kshlib.velocity;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.kshl.kshlib.kyori.ComponentHelper;
import dev.kshl.kshlib.misc.UUIDHelper;
import net.kyori.adventure.audience.Audience;

import java.util.Objects;
import java.util.UUID;

public class VelocityUtils {
    public static String getUsername(CommandSource commandSource) {
        Objects.requireNonNull(commandSource, "commandSource must be nonnull");

        if (commandSource instanceof Player player) {
            return player.getUsername();
        } else if (commandSource instanceof ConsoleCommandSource) {
            return "CONSOLE";
        }
        throw new IllegalArgumentException("Invalid type: " + commandSource.getClass().getName());
    }

    public static UUID getUUID(CommandSource commandSource) {
        Objects.requireNonNull(commandSource, "commandSource must be nonnull");

        if (commandSource instanceof Player player) {
            return player.getUniqueId();
        } else if (commandSource instanceof ConsoleCommandSource) {
            return UUIDHelper.ZERO;
        }
        throw new IllegalArgumentException("Invalid type: " + commandSource.getClass().getName());
    }

    public static void tell(Audience audience, String message) {
        audience.sendMessage(ComponentHelper.legacy(message));
    }

    public static boolean isConsole(UUID uuid) {
        return UUIDHelper.ZERO.equals(uuid);
    }
}
