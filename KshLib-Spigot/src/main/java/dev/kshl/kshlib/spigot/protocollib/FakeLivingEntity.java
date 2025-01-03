package dev.kshl.kshlib.spigot.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.google.common.collect.Lists;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FakeLivingEntity extends FakeEntity {
    protected final boolean alwaysLookAtTarget;
    private Posture currentPosture = Posture.STANDING;
    private final Map<EnumWrappers.ItemSlot, Pair<EnumWrappers.ItemSlot, ItemStack>> equipment = new HashMap<>();

    public FakeLivingEntity(Plugin plugin, EntityType type, String name, Location loc, boolean alwaysLookAtTarget, boolean hideName) {
        super(plugin, type, name, loc, hideName);
        this.alwaysLookAtTarget = alwaysLookAtTarget;
    }


    @Override
    public void move(Location loc, boolean onGround) {
        loc = loc.clone();
        PacketContainer packet = getMoveOrTeleportPacket(loc, onGround);
        if (alwaysLookAtTarget) {
            Location finalLoc = loc;
            audience.forEach((u, player) -> {
                Vector lookVector = player.getLocation().toVector().subtract(finalLoc.toVector());
                float pitch = (float) Math.toDegrees(Math.atan2(-lookVector.getY(), Math.sqrt(lookVector.getX() * lookVector.getX() + lookVector.getZ() * lookVector.getZ())));

                float yaw = (float) Math.toDegrees(Math.atan2(-lookVector.getX(), lookVector.getZ()));
                while (yaw < -180.0f) yaw += 360.0f;
                while (yaw > 180.0f) yaw -= 360.0f;
                packet.getBytes().write(0, (byte) (yaw * 256f / 360f));
                packet.getBytes().write(1, (byte) (pitch * 256f / 360f));
                protocol.sendServerPacket(player, packet);

                PacketContainer packet1 = new PacketContainer(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
                setIdInPacket(packet1);
                packet1.getBytes().write(0, (byte) (yaw * 256f / 360f));
                protocol.sendServerPacket(player, packet1);
            });
        } else {
            sendToAll(packet);

            // Update head

            PacketContainer headPacket = new PacketContainer(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
            setIdInPacket(headPacket);
            headPacket.getBytes().write(0, (byte) (loc.getYaw() * 256f / 360f));
            sendToAll(headPacket);
        }

        // Update head

//        PacketContainer headPacket = new PacketContainer(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
//        setIdInPacket(headPacket);
//        headPacket.getBytes().write(0, (byte) (loc.getYaw() * 256f / 360f));
//        sendToAll(headPacket);

        lastMoved = System.currentTimeMillis();
        this.loc = loc;
    }

    public void setEquipment(EnumWrappers.ItemSlot slot, ItemStack item) {
        audience.consume(audience -> setEquipment(List.of(new Pair<>(slot, item)), audience.values()));
    }

    public void setEquipment(Collection<Pair<EnumWrappers.ItemSlot, ItemStack>> items, Collection<Player> to) {
        for (Pair<EnumWrappers.ItemSlot, ItemStack> item : items) {
            equipment.put(item.getFirst(), item);
        }
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_EQUIPMENT);

        setIdInPacket(packet);
        packet.getSlotStackPairLists().write(0, new ArrayList<>(items));

        to.forEach(p -> ProtocolLibrary.getProtocolManager().sendServerPacket(p, packet));
    }


    public void setPosture(Posture posture) {
        if (currentPosture == posture) return;
        audience.consume(audience -> setPosture(getID(), loc, posture, audience.values()));

        if (posture == Posture.GLIDING) {
            setEquipment(EnumWrappers.ItemSlot.CHEST, new ItemStack(Material.ELYTRA));
        } else if (currentPosture == Posture.GLIDING) {
            setEquipment(EnumWrappers.ItemSlot.CHEST, new ItemStack(Material.AIR));
        }

        currentPosture = posture;
    }

    @Override
    public void spawn(Player player) {
        super.spawn(player);
        setPosture(getID(), loc, currentPosture, List.of(player));
        setEquipment(equipment.values(), List.of(player));
    }

    public static void setPosture(int id, Location location, Posture posture, Collection<Player> to) {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);

        packet.getIntegers().write(0, id);

        final List<WrappedDataValue> wrappedDataValueList = Lists.newArrayList();
        wrappedDataValueList.add(new WrappedDataValue(0, WrappedDataWatcher.Registry.get(Byte.class), (byte) switch (posture) {
            case STANDING, SITTING, SLEEPING -> 0;
            case SNEAKING -> 0x02;
            case SWIMMING, CRAWLING -> 0x10;
            case GLIDING -> 0x80;
        }));
        wrappedDataValueList.add(new WrappedDataValue(6, WrappedDataWatcher.Registry.get(EnumWrappers.getEntityPoseClass()), switch (posture) {
            case STANDING -> EnumWrappers.EntityPose.STANDING;
            case SNEAKING -> EnumWrappers.EntityPose.CROUCHING;
            case SWIMMING, CRAWLING -> EnumWrappers.EntityPose.SWIMMING;
            case GLIDING -> EnumWrappers.EntityPose.FALL_FLYING;
            case SITTING -> EnumWrappers.EntityPose.SITTING;
            case SLEEPING -> EnumWrappers.EntityPose.SLEEPING;
        }));
        if (posture == Posture.SLEEPING || posture == Posture.STANDING) {
            wrappedDataValueList.add(new WrappedDataValue(14, //
                    WrappedDataWatcher.Registry.getBlockPositionSerializer(true), //
                    posture == Posture.SLEEPING ? //
                            Optional.of(BlockPosition.getConverter().getGeneric(new BlockPosition(location.toVector()))) : //
                            Optional.empty()));
        }
        packet.getDataValueCollectionModifier().write(0, wrappedDataValueList);

        for (Player player : to) {
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
        }
    }

    public enum Posture {
        STANDING(0), SNEAKING(1), SWIMMING(2), GLIDING(3), SITTING(4), CRAWLING(5), SLEEPING(6);
        private final byte id;

        Posture(int id) {
            this.id = (byte) id;
        }

        public byte getID() {
            return id;
        }

        public static Posture fromID(byte id) {
            for (Posture posture : values()) if (posture.id == id) return posture;
            throw new IllegalArgumentException("Unknown posture: " + id);
        }
    }
}