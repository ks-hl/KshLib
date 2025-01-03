package dev.kshl.kshlib.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Send a query to a given minecraft server and store any metadata and the
 * player list.
 *
 * @author Ryan Shaw, Jonas Konrad
 * <a href="https://github.com/ryan-shaw/MCJQuery">GitHub Repository</a>
 */
public class MinecraftQuery {
    /**
     * The target address and port
     */
    private final InetSocketAddress address;
    /**
     * <code>null</code> if no successful request has been sent, otherwise a Map
     * containing any metadata received except the player list
     */
    private Map<String, String> values;
    /**
     * <code>null</code> if no successful request has been sent, otherwise an
     * array containing all online player usernames
     */
    private String[] onlineUsernames;
    private InetSocketAddress queryAddress;
    /**
     * Convenience constructor
     *
     * @param host The target host
     * @param port The target port
     * @see MinecraftQuery#MinecraftQuery(InetSocketAddress, InetSocketAddress)
     */
    public MinecraftQuery(String host, int port, int queryport) {
        this(new InetSocketAddress(host, queryport), new InetSocketAddress(host, port));
    }

    /**
     * Create a new instance of this class
     *
     * @param address The servers IP-address
     */
    public MinecraftQuery(InetSocketAddress queryAddress, InetSocketAddress address) {
        this.address = address;
        this.queryAddress = queryAddress;
        if (queryAddress.getPort() == 0) {
            this.queryAddress = new InetSocketAddress(queryAddress.getHostName(), address.getPort());
        }
    }

    public static void main(String[] args) {
        MinecraftQuery query = new MinecraftQuery("play.atownyserver.com", 25565, 25565);
        System.out.println(query.pingServer());
        try {
            query.sendQueryRequest();
            for (Map.Entry<String, String> entry : query.getValues().entrySet()) {
                System.out.println(entry.getKey() + ": " + entry.getValue());
            }
            System.out.println(Arrays.stream(query.getOnlineUsernames()).reduce((a, b) -> a + ", " + b).orElse(""));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper method to send a datagram packet
     *
     * @param socket        The connection the packet should be sent through
     * @param targetAddress The target IP
     * @param data          The byte data to be sent
     * @throws IOException
     */
    private static void sendPacket(DatagramSocket socket, InetSocketAddress targetAddress, byte... data) throws IOException {
        DatagramPacket sendPacket = new DatagramPacket(data, data.length, targetAddress.getAddress(), targetAddress.getPort());
        socket.send(sendPacket);
    }

    /**
     * Helper method to send a datagram packet
     *
     * @param socket        The connection the packet should be sent through
     * @param targetAddress The target IP
     * @param data          The byte data to be sent, will be cast to bytes
     * @throws IOException
     * @see MinecraftQuery#sendPacket(DatagramSocket, InetSocketAddress, byte...)
     */
    private static void sendPacket(DatagramSocket socket, InetSocketAddress targetAddress, int... data) throws IOException {
        final byte[] d = new byte[data.length];
        int i = 0;
        for (int j : data)
            d[i++] = (byte) (j & 0xff);
        sendPacket(socket, targetAddress, d);
    }

    /**
     * Receive a packet from the given socket
     *
     * @param socket the socket
     * @param buffer the buffer for the information to be written into
     * @return the entire packet
     * @throws IOException
     */
    private static DatagramPacket receivePacket(DatagramSocket socket, byte[] buffer) throws IOException {
        final DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
        socket.receive(dp);
        return dp;
    }

    /**
     * Read a String until 0x00
     *
     * @param array  The byte array
     * @param cursor The mutable cursor (will be increased)
     * @return The string
     */
    private static String readString(byte[] array, AtomicInteger cursor) {
        final int startPosition = cursor.incrementAndGet();
        for (; cursor.get() < array.length && array[cursor.get()] != 0; cursor.incrementAndGet())
            ;
        return new String(Arrays.copyOfRange(array, startPosition, cursor.get()));
    }

    /**
     * Try pinging the server and then sending the query
     *
     * @throws IOException If the server cannot be pinged
     * @see MinecraftQuery#pingServer()
     * @see MinecraftQuery#sendQueryRequest()
     */
    public void sendQuery() throws IOException {
        sendQueryRequest();
    }

    /**
     * Try pinging the server
     *
     * @return <code>true</code> if the server can be reached within 1.5 second
     */
    public boolean pingServer() {
        // try pinging the given server
        try {
            final Socket socket = new Socket();
            socket.connect(address, 1500);
            socket.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Request the UDP query
     *
     * @throws IOException if anything goes wrong during the request
     */
    private void sendQueryRequest() throws IOException {
        InetSocketAddress local = queryAddress;
        if (queryAddress.getPort() == 0) {
            local = new InetSocketAddress(queryAddress.getAddress(), address.getPort());
        }
        System.out.println(local);
        final DatagramSocket socket = new DatagramSocket();
        try {
            final byte[] receiveData = new byte[10240];
            socket.setSoTimeout(2000);
            sendPacket(socket, local, 0xFE, 0xFD, 0x09, 0x0A, 0x0E, 0x03, 0x01);
            final int challengeInteger;
            {
                receivePacket(socket, receiveData);
                byte byte1 = -1;
                int i = 0;
                byte[] buffer = new byte[11];
                for (int count = 5; (byte1 = receiveData[count++]) != 0; )
                    buffer[i++] = byte1;
                challengeInteger = Integer.parseInt(new String(buffer).trim());
            }
            sendPacket(socket, local, 0xFE, 0xFD, 0x00, 0x01, 0x01, 0x01, 0x01, challengeInteger >> 24, challengeInteger >> 16, challengeInteger >> 8, challengeInteger, 0x00, 0x00, 0x00, 0x00);

            final int length = receivePacket(socket, receiveData).getLength();
            values = new HashMap<>();
            final AtomicInteger cursor = new AtomicInteger(5);
            while (cursor.get() < length) {
                final String s = readString(receiveData, cursor);
                if (s.isEmpty())
                    break;
                else {
                    final String v = readString(receiveData, cursor);
                    values.put(s, v);
                }
            }
            readString(receiveData, cursor);
            final Set<String> players = new HashSet<>();
            while (cursor.get() < length) {
                final String name = readString(receiveData, cursor);
                if (!name.isEmpty())
                    players.add(name);
            }
            onlineUsernames = players.toArray(new String[0]);
        } finally {
            socket.close();
        }
    }

    /**
     * Get the additional values if the Query has been sent
     *
     * @return The data
     * @throws IllegalStateException if the query has not been sent yet or there has been an error
     */
    public Map<String, String> getValues() {
        if (values == null)
            throw new IllegalStateException("Query has not been sent yet!");
        else
            return values;
    }

    /**
     * Get the online usernames if the Query has been sent
     *
     * @return The username array
     * @throws IllegalStateException if the query has not been sent yet or there has been an error
     */
    public String[] getOnlineUsernames() {
        if (onlineUsernames == null)
            throw new IllegalStateException("Query has not been sent yet!");
        else
            return onlineUsernames;
    }
}
