package dev.kshl.kshlib.crypto;

import dev.kshl.kshlib.function.ThrowingConsumer;
import dev.kshl.kshlib.function.ThrowingSupplier;
import dev.kshl.kshlib.misc.Bits;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.PublicKey;

public abstract class HandshakeECRSA extends Handshake {

    protected HandshakeECRSA(ThrowingConsumer<byte[], Exception> send, ThrowingSupplier<byte[], Exception> receive, EncryptionRSA privateKey, EncryptionRSA publicKey) {
        super(send, receive, privateKey, publicKey);
    }

    public static class Server extends HandshakeECRSA {

        public Server(ThrowingConsumer<byte[], Exception> send, ThrowingSupplier<byte[], Exception> receive, EncryptionRSA serverKey, EncryptionRSA clientKey) {
            super(send, receive, serverKey, clientKey);
        }

        @Override
        public EncryptionAES handshake() throws IOException, GeneralSecurityException {

            try {
                PublicKey clientPublicECC = receiveKey(publicKey, receive.get());

                EncryptionECC.ECCPair serverECC = EncryptionECC.generate();

                send.accept(encodeKey(serverECC.publicKey().getKey(), privateKey));

                return serverECC.combine(clientPublicECC);
            } catch (IOException | GeneralSecurityException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class Client extends HandshakeECRSA {

        public Client(ThrowingConsumer<byte[], Exception> send, ThrowingSupplier<byte[], Exception> receive, EncryptionRSA clientKey, EncryptionRSA serverKey) {
            super(send, receive, clientKey, serverKey);
        }

        @Override
        public EncryptionAES handshake() throws IOException, GeneralSecurityException {
            try {
                EncryptionECC.ECCPair clientECC = EncryptionECC.generate();

                send.accept(encodeKey(clientECC.publicKey().getKey(), privateKey));

                PublicKey serverPublicECC = receiveKey(publicKey, receive.get());

                return clientECC.combine(serverPublicECC);
            } catch (IOException | GeneralSecurityException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static byte[] encodeKey(PublicKey key, EncryptionRSA privateKey) throws IllegalBlockSizeException, BadPaddingException {
        byte[] keyBytes = key.getEncoded();
        ByteBuffer out = ByteBuffer.allocate(8 + keyBytes.length);

        out.putLong(System.currentTimeMillis());
        out.put(keyBytes);

        return privateKey.encrypt(out.array());
    }

    public static PublicKey receiveKey(EncryptionRSA publicKey, byte[] bytes) throws Exception {
        ByteBuffer data = ByteBuffer.wrap(publicKey.decrypt(bytes));
        long timeStamp = data.getLong();
        if (!isValid(publicKey.getUUID(), timeStamp))
            throw new IllegalArgumentException("Invalid timestamp: " + timeStamp);
        return EncryptionECC.getPublicKey(Bits.slice(data, data.position()).array()).getKey();
    }
}
