package dev.kshl.kshlib.parsing;

public interface Sender {
    boolean hasPermission(String node);

    void sendMessage(String message);
}
