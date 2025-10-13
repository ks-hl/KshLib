package dev.kshl.kshlib.spigot.protocollib.wrapper;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;

import java.lang.reflect.Constructor;

public final class EntityPosSync {
    // Tries a few likely class names across 1.21.x
    private static Class<?> resolveClass(String... names) throws ClassNotFoundException {
        ClassNotFoundException last = null;
        for (String n : names) {
            try {
                return Class.forName(n);
            } catch (ClassNotFoundException e) {
                last = e;
            }
        }
        if (last == null) {
            throw new ClassNotFoundException("No class found");
        }
        throw last;
    }

    public static PacketContainer getTeleportPacket(double x, double y, double z, float yawDeg, float pitchDeg, boolean onGround) throws Exception {

        // NMS classes we need
        Class<?> vec3 = resolveClass("net.minecraft.world.phys.Vec3", "net.minecraft.world.phys.Vec3D" // old fallback, just in case
        );

        // PositionMoveRotation appeared around 1.21.2+; try the common homes
        Class<?> pmr = resolveClass("net.minecraft.world.entity.PositionMoveRotation", "net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket$PositionMoveRotation");

        // Build Vec3 position/delta
        Constructor<?> vecCtor = vec3.getConstructor(double.class, double.class, double.class);
        Object pos = vecCtor.newInstance(x, y, z);
        Object delta = vecCtor.newInstance(0.0, 0.0, 0.0);

        // Build PositionMoveRotation(position, deltaMovement, yRot, xRot)
        Constructor<?> pmrCtor = pmr.getDeclaredConstructor(vec3, vec3, float.class, float.class);
        Object pmrInstance = pmrCtor.newInstance(pos, delta, yawDeg, pitchDeg);

        // Packet
        PacketContainer pkt = new PacketContainer(PacketType.Play.Server.ENTITY_POSITION_SYNC);

        // [1] PositionMoveRotation (write by type, not by index group like getDoubles())
        pkt.getModifier().withType(pmr).write(0, pmrInstance);

        // [2] flags: Set<RelativeMovement> — empty for absolute teleports

        // [3] onGround
        pkt.getBooleans().write(0, onGround);

        return pkt;
    }
}
