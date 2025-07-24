package dev.kshl.kshlib;

import dev.kshl.kshlib.exceptions.BusyException;
import dev.kshl.kshlib.net.HTTPResponseCode;
import dev.kshl.kshlib.net.NetUtilInterval;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class FindUsername {
    private static class Net extends NetUtilInterval {
        public Net(String endpoint) {
            super(endpoint, 1000L);
        }
    }

    private final Net net1 = new Net("https://api.mojang.com/users/profiles/minecraft/");
    private final Net net2 = new Net("https://api.minecraftservices.com/minecraft/profile/lookup/name/");

    private final String alphabet = "abcdefghijklmnopqrstuvwxyz0123456789_";

    private void forEachLetter(Consumer<Character> consumer) {
        ArrayList<Character> chars = new ArrayList<>();
        for (char c : alphabet.toCharArray()) {
            chars.add(c);
        }
        Collections.shuffle(chars);
        chars.forEach(consumer);
    }

    @Test
    public void findUsername() throws BusyException, IOException {
        List<String> usernames = new ArrayList<>();
        for (char a : alphabet.toCharArray()) {
            for (char b : alphabet.toCharArray()) {
                for (char c : alphabet.toCharArray()) {
                    String username = a + "" + b + c;
                    usernames.add(username);
                }
            }
        }
        Collections.shuffle(usernames);

        ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>(usernames);

        new Thread(new RunnableRun(net1, queue)).start();
        new Thread(new RunnableRun(net2, queue)).start();
        long lastPrintAvail = System.currentTimeMillis();
        while (!queue.isEmpty()) {
            Thread.onSpinWait();
            if (System.currentTimeMillis() - lastPrintAvail > 5000 && !available.isEmpty()) {
                lastPrintAvail = System.currentTimeMillis();
                StringBuilder msg = new StringBuilder("Available: ");
                for (String username : available) {
                    msg.append("\n - ").append(username);
                }
                System.out.println(msg);
            }
        }
        System.out.println("Available: ");
        for (String username : available) {
            System.out.println(" - " + username);
        }
    }

    private static final List<String> available = new ArrayList<>();

    private class RunnableRun implements Runnable {
        private final Net net;
        private final ConcurrentLinkedQueue<String> queue;

        long delay = 1000;
        long nextLowerDelay = 0;


        public RunnableRun(Net net, ConcurrentLinkedQueue<String> queue) {
            this.net = net;
            this.queue = queue;
        }

        @Override
        public void run() {
            while (!queue.isEmpty()) {
                String username = queue.poll();
                int free = 0;
                while (true) {
                    try {
                        if (System.currentTimeMillis() > nextLowerDelay) {
                            delay = Math.max(1000, delay - 100);
                            nextLowerDelay = System.currentTimeMillis() + 15000;
                        }
                        Thread.sleep(delay);

                        HTTPResponseCode code = net.getResponse(username).getResponseCode();
                        if (code == HTTPResponseCode.NOT_FOUND) free++;
                        else if (code == HTTPResponseCode.TOO_MANY_REQUESTS) {
                            delay += 100;
                            nextLowerDelay = System.currentTimeMillis() + 30000;
                            System.err.println("Too many requests. Delay is now " + delay + "ms.");
                            continue;
                        } else if (code != HTTPResponseCode.OK) {
                            System.err.println("Bad response: " + code);
                            continue;
                        }
                        String msg = username + " = " + code;
                        if (free == 0) {
                            System.out.println(msg + " (taken)");
                            break;
                        }
                        if (free >= 3) {
                            System.out.println(msg + " (free)");
                            synchronized (available) {
                                available.add(username);
                            }
                            break;
                        }
                        System.out.println(msg + " (count=" + free + ")");
                    } catch (IllegalArgumentException e) {
                        System.out.println(e.getMessage());
                    } catch (BusyException | IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }
}
