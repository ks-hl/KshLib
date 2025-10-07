package dev.kshl.kshlib.spigot.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
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

public class FakeLivingEntity extends FakeEntity {
    protected final boolean alwaysLookAtTarget;
    private Posture currentPosture = Posture.STANDING;
    private final Map<EnumWrappers.ItemSlot, ItemStack> equipment = new HashMap<>();

    public FakeLivingEntity(Plugin plugin, EntityType type, String name, Location loc, boolean alwaysLookAtTarget, boolean hideName) {
        super(plugin, type, name, loc, hideName);
        this.alwaysLookAtTarget = alwaysLookAtTarget;
    }

    @Override
    public void move(Location loc, boolean onGround) {
        PacketContainer packet = getMoveOrTeleportPacket(loc, onGround);
        if (alwaysLookAtTarget) {
            audience.forEach((u, player) -> {
                Vector lookVector = player.getLocation().toVector().subtract(loc.toVector());
                float pitch = (float) Math.toDegrees(Math.atan2(-lookVector.getY(), Math.sqrt(lookVector.getX() * lookVector.getX() + lookVector.getZ() * lookVector.getZ())));
                float yaw = (float) Math.toDegrees(Math.atan2(-lookVector.getX(), lookVector.getZ()));
                while (yaw < -180.0f) yaw += 360.0f;
                while (yaw > 180.0f) yaw -= 360.0f;

                loc.setPitch(pitch);
                loc.setYaw(yaw);

                packet.getBytes().write(0, (byte) (yaw * 256f / 360f));
                packet.getBytes().write(1, (byte) (pitch * 256f / 360f));
                protocol.sendServerPacket(player, packet);

                updateHead(List.of(player));
            });
        } else {
            sendToAll(packet);

            // Update head

            updateHead(audience.values());
        }

        lastMoved = System.currentTimeMillis();
    }

    public void setEquipment(EnumWrappers.ItemSlot slot, ItemStack item, boolean update) {
        ItemStack stack = (item == null) ? new ItemStack(Material.AIR) : item;
        this.equipment.put(slot, stack);
        if (update) {
            updateEquipment(audience.values());
        }
    }

    private void updateEquipment(Collection<Player> to) {
        final List<Pair<EnumWrappers.ItemSlot, ItemStack>> out = new ArrayList<>();

        for (Map.Entry<EnumWrappers.ItemSlot, ItemStack> entry : equipment.entrySet()) {
            out.add(new Pair<>(entry.getKey(), entry.getValue()));
        }

        if (out.isEmpty()) return;

        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_EQUIPMENT);
        setIdInPacket(packet);

        packet.getSlotStackPairLists().write(0, out);

        for (Player viewer : to) {
            ProtocolLibrary.getProtocolManager().sendServerPacket(viewer, packet);
        }
    }

    public void setPosture(Posture posture) {
        if (currentPosture == posture) return;
        setPosture(posture, audience.values());

        if (posture == Posture.GLIDING) {
            setEquipment(EnumWrappers.ItemSlot.CHEST, new ItemStack(Material.ELYTRA), true);
        } else if (currentPosture == Posture.GLIDING) {
            setEquipment(EnumWrappers.ItemSlot.CHEST, new ItemStack(Material.AIR), true);
        }

        currentPosture = posture;
    }

    @Override
    public void spawn(Player player) {
        super.spawn(player);
        setPosture(currentPosture, List.of(player));
        updateEquipment(List.of(player));
        updateHead(List.of(player));
    }

    private void updateHead(Collection<Player> to) {
        PacketContainer packet1 = new PacketContainer(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
        setIdInPacket(packet1);
        packet1.getBytes().write(0, (byte) (loc.getYaw() * 256f / 360f));
        to.forEach(player -> protocol.sendServerPacket(player, packet1));
    }

    public void setPosture(Posture posture, Collection<Player> to) {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
        setIdInPacket(packet);

        final List<WrappedDataValue> values = new ArrayList<>();

        // 0: entity shared flags (Byte). You probably want to preserve other bits in real code.
        byte flags = switch (posture) {
            case STANDING, SITTING, SLEEPING -> 0;
            case SNEAKING -> 0x02;
            case SWIMMING, CRAWLING -> 0x10;
            case GLIDING -> (byte) 0x80;
        };
        values.add(new WrappedDataValue(0, WrappedDataWatcher.Registry.get(Byte.class), flags));

        // 6: pose. Convert ProtocolLib wrapper enum -> NMS Pose
        var poseSerializer = WrappedDataWatcher.Registry.get(EnumWrappers.getEntityPoseClass());
        Object nmsPose = EnumWrappers.getEntityPoseConverter().getGeneric(
                switch (posture) {
                    case STANDING -> EnumWrappers.EntityPose.STANDING;
                    case SNEAKING -> EnumWrappers.EntityPose.CROUCHING;
                    case SWIMMING, CRAWLING -> EnumWrappers.EntityPose.SWIMMING;
                    case GLIDING -> EnumWrappers.EntityPose.FALL_FLYING;
                    case SITTING -> EnumWrappers.EntityPose.SITTING;
                    case SLEEPING -> EnumWrappers.EntityPose.SLEEPING;
                }
        );
        values.add(new WrappedDataValue(6, poseSerializer, nmsPose));

        packet.getDataValueCollectionModifier().write(0, values);

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