package dev.kshl.kshlib.spigot.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.List;

public class ChatCompletions {
    public enum Action {
        ADD, REMOVE, SET;

        private final Object enumAction;

        Action() {
            Object enumAction1;
            try {
                enumAction1 = EnumReflector.getEnum("net.minecraft.network.protocol.game.ClientboundCustomChatCompletionsPacket$Action", toString());
            } catch (ClassNotFoundException e) {
                enumAction1 = null;
            }
            enumAction = enumAction1;
        }
    }

    public static void sendChatCompletions(Player player, Action action, List<String> completions) throws ClassNotFoundException {
        if (action.enumAction == null) throw new ClassNotFoundException("Enum not found for action " + action);

        ProtocolManager pm = ProtocolLibrary.getProtocolManager();
        PacketContainer container = pm.createPacket(PacketType.Play.Server.CUSTOM_CHAT_COMPLETIONS);

        container.getModifier().write(0, action.enumAction);
        container.getModifier().modify(1, list -> completions);

        pm.sendServerPacket(player, container);
    }

    public static CommandData wrapCommandData(Object node) throws ReflectiveOperationException {
        Object stub = null;
        int flags = 0;
        int[] children = null;
        boolean stubFound = false;
        boolean flagsFound = false;
        boolean childrenFound = false;

        for (Field field_ : node.getClass().getDeclaredFields()) {
            if (field_.getType().toString().contains("ClientboundCommandsPacket$")) {
                field_.setAccessible(true);
                stub = field_.get(node);
                stubFound = true;
            }
            if (!flagsFound && field_.getType().equals(int.class)) {
                flagsFound = true;
                field_.setAccessible(true);
                flags = field_.getInt(node) & 3;
            }
            if (children == null && field_.getType().equals(int[].class)) {
                childrenFound = true;
                field_.setAccessible(true);
                children = (int[]) field_.get(node);
            }
        }

        if (!stubFound) throw new ClassNotFoundException("Stub not found");
        if (!flagsFound) throw new ClassNotFoundException("Flags not found");
        if (!childrenFound) throw new ClassNotFoundException("Children not found");

        String id = null;
        if (stub != null) {
            Field field = stub.getClass().getDeclaredFields()[0];
            field.setAccessible(true);
            id = (String) field.get(stub);
        }

        return new CommandData(new CommandDataStub(id), switch (flags) {
            case 0 -> NodeType.ROOT;
            case 1 -> NodeType.LITERAL;
            case 2 -> NodeType.ARGUMENT;
            default -> throw new IllegalArgumentException();
        }, children);
    }

    public enum NodeType {
        ROOT, LITERAL, ARGUMENT;
    }

    public record CommandData(CommandDataStub stub, NodeType nodeType, int[] children) {

    }

    public record CommandDataStub(String id) {

    }
}
