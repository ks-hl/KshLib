package dev.kshl.kshlib.platform;

import dev.kshl.kshlib.net.NetUtilInterval;

public class MojangAPI extends NetUtilInterval {
    public static final MojangAPI api = new MojangAPI("https://api.mojang.com/");
    public static final MojangAPI sessionServer = new MojangAPI("https://sessionserver.mojang.com");

    private MojangAPI(String endpoint) {
        super(endpoint, 333);
    }
}
