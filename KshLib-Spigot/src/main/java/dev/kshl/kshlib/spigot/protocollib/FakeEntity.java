package dev.kshl.kshlib.spigot.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import dev.kshl.kshlib.concurrent.ConcurrentMap;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public abstract class FakeEntity {
    public final EntityType type;
    protected final int id;
    protected final UUID uuid;
    protected final String name;
    protected final ProtocolManager protocol = ProtocolLibrary.getProtocolManager();
    protected final ConcurrentMap<Map<UUID, Player>, UUID, Player> audience = new ConcurrentMap<>(new HashMap<>());
    protected final boolean hideName;
    protected Location loc;
    protected long lastMoved;

    public FakeEntity(Plugin plugin, EntityType type, String name, Location loc, boolean hideName) {
        this.type = type;
        this.uuid = generateNPCUUID();
        this.id = uuid.hashCode();
        this.hideName = hideName || name == null;
        this.name = hideName ? ("hb." + id) : name;
        this.loc = loc;

        protocol.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Client.USE_ENTITY) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                int entityID = event.getPacket().getIntegers().read(0);
                if (entityID != id || !audience.containsKey(event.getPlayer().getUniqueId())) return;
                EnumWrappers.EntityUseAction action = event.getPacket().getEnumEntityUseActions().read(0).getAction();
                EnumWrappers.Hand hand = event.getPacket().getHands().read(0);
                boolean sneaking = event.getPacket().getBooleans().read(0);

                onInteractEvent(event.getPlayer(), action, hand, sneaking);
            }
        });
    }

    protected void onInteractEvent(Player player, EnumWrappers.EntityUseAction action, EnumWrappers.Hand hand, boolean isSneaking) {
    }

    public static UUID generateNPCUUID() {
        UUID uuid = UUID.randomUUID();
        return UUID.fromString(uuid.toString().substring(0, 15) + '2' + uuid.toString().substring(16));
    }

    @OverridingMethodsMustInvokeSuper
    public void spawn(Player player) {
        if (audience.put(player.getUniqueId(), player) != null) {
            return;
        }

        PacketContainer packet = new PacketContainer(PacketType.Play.Server.SPAWN_ENTITY);
        setIdInPacket(packet);
        packet.getUUIDs().write(0, uuid);
        packet.getEntityTypeModifier().write(0, type);
        packet.getDoubles().write(0, loc.getX());
        packet.getDoubles().write(1, loc.getY());
        packet.getDoubles().write(2, loc.getZ());
        packet.getBytes().write(0, (byte) (loc.getYaw() * 256f / 360f));
        packet.getBytes().write(1, (byte) (loc.getPitch() * 256f / 360f));
        packet.getBytes().write(2, (byte) (loc.getYaw() * 256f / 360f)); // head angle
        protocol.sendServerPacket(player, packet);

//        if (name != null) {
//            packet = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
//            StructureModifier<List<WrappedDataValue>> watchableAccessor = packet.getDataValueCollectionModifier();
//            watchableAccessor.write(0, List.of(
//                    new WrappedDataValue(16, //
//                            WrappedDataWatcher.Registry.getNBTCompoundSerializer(), //
//                            NbtFactory.of("CustomName", "[{\"text\":\"" + name + "\"}]"))) //
//            );
//            protocol.sendServerPacket(player, packet);
//        }
    }

    public static void sendMetadata(Player player, int entityID, List<WrappedDataValue> meta) {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
        packet.getIntegers().write(0, entityID);
        StructureModifier<List<WrappedDataValue>> watchableAccessor = packet.getDataValueCollectionModifier();
        watchableAccessor.write(0, meta);
        ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
    }

    public UpdateRecord update(List<Player> newAudience_) {
        Map<UUID, Player> newAudience = new HashMap<>();
        newAudience_.forEach(p -> newAudience.put(p.getUniqueId(), p));
        Map<UUID, Player> remove = new HashMap<>();
        audience.forEach((uuid, player) -> {
            if (newAudience.remove(uuid) == null) remove.put(uuid, player);
        });
        remove.forEach((u, p) -> remove(p));
        newAudience.forEach((u, p) -> spawn(p));

        return new UpdateRecord(newAudience.values(), remove.values());
    }

    public record UpdateRecord(Collection<Player> added, Collection<Player> removed) {
    }

    protected void setIdInPacket(PacketContainer packet) {
        packet.getIntegers().write(0, id);
    }

    protected void sendToAll(PacketContainer packet) {
        audience.consume(audience -> audience.forEach((uuid, player) -> protocol.sendServerPacket(player, packet)));
    }

    public int getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getLastMoved() {
        return lastMoved;
    }


    public final Collection<Player> removeAll() {
        Set<Player> out = new HashSet<>();
        audience.forEachCopied((u, p) -> {
            remove(p);
            out.add(p);
        });
        audience.clear();
        return out;
    }

    @OverridingMethodsMustInvokeSuper
    public void remove(Player player) {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
        packet.getIntLists().modify(0, list -> {
            list.add(id);
            return list;
        });
        protocol.sendServerPacket(player, packet);
        audience.remove(player.getUniqueId());
    }

    public void move(Location loc, boolean onGround) {
        loc = loc.clone();
        sendToAll(getMoveOrTeleportPacket(loc, onGround));

        lastMoved = System.currentTimeMillis();
        this.loc = loc;
    }

    protected PacketContainer getMoveOrTeleportPacket(Location loc, boolean onGround) {
        loc = loc.clone();
        // Move entity

        double distanceX = loc.getX() - this.loc.getX();
        double distanceY = loc.getY() - this.loc.getY();
        double distanceZ = loc.getZ() - this.loc.getZ();
        boolean teleport = Math.abs(distanceX) > 7.9 || Math.abs(distanceY) > 7.9 || Math.abs(distanceZ) > 7.9;

        PacketContainer packet = new PacketContainer(teleport ? PacketType.Play.Server.ENTITY_TELEPORT : PacketType.Play.Server.REL_ENTITY_MOVE_LOOK);
        setIdInPacket(packet);
        if (teleport) {
            packet.getDoubles().write(0, loc.getX());
            packet.getDoubles().write(1, loc.getY());
            packet.getDoubles().write(2, loc.getZ());
        } else {
            packet.getShorts().write(0, (short) (distanceX * 4096));
            packet.getShorts().write(1, (short) (distanceY * 4096));
            packet.getShorts().write(2, (short) (distanceZ * 4096));
        }
        packet.getBooleans().write(0, onGround);
        packet.getBytes().write(0, (byte) (loc.getYaw() * 256f / 360f));
        packet.getBytes().write(1, (byte) (loc.getPitch() * 256f / 360f));

        return packet;
    }

    public UUID getUUID() {
        return uuid;
    }
}
