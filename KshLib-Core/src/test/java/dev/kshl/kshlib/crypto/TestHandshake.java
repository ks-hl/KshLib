package dev.kshl.kshlib.crypto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;

public class TestHandshake {
    @Test
    @Timeout(value = 10)
    public void testECCRSA() throws ExecutionException, InterruptedException {
        BlockingQueue<byte[]> clientInbound = new LinkedBlockingQueue<>();
        BlockingQueue<byte[]> serverInbound = new LinkedBlockingQueue<>();

        EncryptionRSA.RSAPair clientRSA = EncryptionRSA.generate();
        EncryptionRSA.RSAPair serverRSA = EncryptionRSA.generate();

        HandshakeECRSA.Client client = new HandshakeECRSA.Client(serverInbound::add, clientInbound::take, clientRSA.privateKey(), serverRSA.publicKey());
        HandshakeECRSA.Server server = new HandshakeECRSA.Server(clientInbound::add, serverInbound::take, serverRSA.privateKey(), clientRSA.publicKey());

        testHandshake(client, server);
    }

    public void testHandshake(Handshake client, Handshake server) throws ExecutionException, InterruptedException {

        CompletableFuture<EncryptionAES> clientHandshake = new CompletableFuture<>();
        CompletableFuture<EncryptionAES> serverHandshake = new CompletableFuture<>();

        new Thread(() -> {
            try {
                System.out.println("Client starting handshake");
                clientHandshake.complete(client.handshake());
                System.out.println("Client finished handshake successfully");
            } catch (Throwable t) {
                clientHandshake.completeExceptionally(t);
                t.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            try {
                System.out.println("Server starting handshake");
                serverHandshake.complete(server.handshake());
                System.out.println("Server finished handshake successfully");
            } catch (Throwable t) {
                serverHandshake.completeExceptionally(t);
                t.printStackTrace();
            }
        }).start();

        clientHandshake.get();
        serverHandshake.get();
    }
}
