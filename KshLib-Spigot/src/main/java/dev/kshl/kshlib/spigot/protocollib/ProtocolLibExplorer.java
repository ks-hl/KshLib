package dev.kshl.kshlib.spigot.protocollib;

import com.comphenix.protocol.events.AbstractStructure;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;

import java.lang.reflect.Method;

public class ProtocolLibExplorer {
    public static String explore(PacketContainer packet) {
        StringBuilder out = new StringBuilder();

        int i = 0;
        for (FieldAccessor field : packet.getModifier().getFields()) {
            if (!out.isEmpty()) out.append("\n");
            out.append(i).append(". ");
            out.append(field.getField().getType().getName());
            out.append(" = ");
            out.append(packet.getModifier().read(i));

            i++;
        }

        out.append("\nMETHODS:");
        for (Method method : AbstractStructure.class.getDeclaredMethods()) {
            try {
                out.append("\n").append(method.getName()).append(": ");
                StructureModifier<?> modifier = (StructureModifier<?>) method.invoke(packet);
                out.append(modifier.size());
                for (int i1 = 0; i1 < modifier.size(); i1++) {
                    out.append("\n").append(i1).append(". ").append(modifier.read(i1));
                }
            } catch (ClassCastException ignored) {
            } catch (Throwable t) {
                out.append("\n    ");
                out.append(t.getClass().getName()).append(" (").append(t.getMessage()).append(")");
            }
        }

        return out.toString();
    }
}
