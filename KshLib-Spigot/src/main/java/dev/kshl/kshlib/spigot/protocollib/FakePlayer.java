package dev.kshl.kshlib.spigot.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class FakePlayer extends FakeLivingEntity {
    private final Map<UUID, String> teams = new HashMap<>();
    private final boolean noCollide;
    private Skin skin;

    public FakePlayer(Plugin plugin, String name, Skin skin, Location loc, boolean alwaysLookAtTarget, boolean noCollide) {
        super(plugin, EntityType.PLAYER, name, loc, alwaysLookAtTarget, name == null);
        this.noCollide = noCollide;
        this.skin = skin;
    }

    public void setSkin(Skin skin) {
        this.skin = skin;
    }

    @Override
    public void spawn(Player player) {
        // Sends player info, creates the player

        PacketContainer packet = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
        packet.getPlayerInfoActions().modify(0, set -> {
            set.add(EnumWrappers.PlayerInfoAction.ADD_PLAYER);
            set.add(EnumWrappers.PlayerInfoAction.UPDATE_LISTED);
            return set;
        });
        WrappedGameProfile profile = new WrappedGameProfile(uuid, name);
        if (skin != null) profile.getProperties().put("textures", skin.wrap());
        // TODO fake player still being added to list?
        packet.getPlayerInfoDataLists().write(1, List.of(new PlayerInfoData(uuid, 0, false, EnumWrappers.NativeGameMode.SURVIVAL, profile, WrappedChatComponent.fromLegacyText(name))));
        protocol.sendServerPacket(player, packet);


        // Set initial location

        super.spawn(player);

        if (hideName || noCollide) {
            packet = new PacketContainer(PacketType.Play.Server.SCOREBOARD_TEAM);
            // identifier
            String teamName = "cs:hb." + uuid.toString().replace("-", "") + ":" + UUID.randomUUID().toString().replace("-", "");
            synchronized (teams) {
                teams.put(player.getUniqueId(), teamName);
            }
            packet.getStrings().write(0, teamName);
            // mode
            packet.getIntegers().write(0, 0);

            // name tag visibility
            Object team = ((Optional<?>) packet.getModifier().withType(Optional.class).read(0)).orElseThrow();
            int i = 0;
            for (Field field : team.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                if (!field.getType().equals(String.class)) {
                    continue;
                }
                i++;
                if (i == 1 && !hideName) continue;
                if (i == 2 && !noCollide) continue;
                try {
                    field.set(team, "never");
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
            if (i < 2) {
                throw new IllegalStateException("Not all fields found. i=" + i);
            }

            // uuids
            packet.getModifier().withType(Collection.class).write(0, List.of(name, uuid.toString()));
            protocol.sendServerPacket(player, packet);
        }
    }

    public void swingArm() {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ANIMATION);

        setIdInPacket(packet);
        packet.getIntegers().write(1, 0);

        sendToAll(packet);
    }

    @Override
    public void remove(Player player) {
        // Removes player info

        PacketContainer packet = new PacketContainer(PacketType.Play.Server.PLAYER_INFO_REMOVE);
        packet.getUUIDLists().modify(0, list -> {
            list.add(uuid);
            return list;
        });
        protocol.sendServerPacket(player, packet);

        super.remove(player);

        String teamNameToRemove;
        synchronized (teams) {
            teamNameToRemove = teams.remove(player.getUniqueId());
        }
        if (teamNameToRemove != null) {
            packet = new PacketContainer(PacketType.Play.Server.SCOREBOARD_TEAM);
            // identifier
            packet.getStrings().write(0, teamNameToRemove);
            // mode
            packet.getIntegers().write(0, 1);
            protocol.sendServerPacket(player, packet);
        }
    }

    public static void sendGameModeChange(Player of, EnumWrappers.NativeGameMode gameMode, Collection<Player> to) {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
        packet.getPlayerInfoActions().modify(0, set -> {
            set.add(EnumWrappers.PlayerInfoAction.UPDATE_GAME_MODE);
            return set;
        });
        packet.getPlayerInfoDataLists().write(1, List.of(new PlayerInfoData(of.getUniqueId(), 0, false, gameMode, null, null)));

        AtomicBoolean tellSelf = new AtomicBoolean();
        to.forEach(other -> {
            if (other.getUniqueId().equals(of.getUniqueId())) tellSelf.set(true);
            ProtocolLibrary.getProtocolManager().sendServerPacket(other, packet);
        });

        if (tellSelf.get()) {
            PacketContainer eventPacket = new PacketContainer(PacketType.Play.Server.GAME_STATE_CHANGE);

            eventPacket.getGameStateIDs().write(0, 3);
            eventPacket.getFloat().write(0, (float) switch (gameMode) {
                case SURVIVAL -> 0;
                case CREATIVE -> 1;
                case ADVENTURE -> 2;
                case SPECTATOR -> 3;

                default -> throw new IllegalArgumentException("Invalid gamemode");
            });

            ProtocolLibrary.getProtocolManager().sendServerPacket(of, eventPacket);
        }
    }
}
