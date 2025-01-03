package dev.kshl.kshlib.net;

import dev.kshl.kshlib.exceptions.BusyException;
import dev.kshl.kshlib.misc.BiDiMapCache;
import dev.kshl.kshlib.misc.Pair;
import dev.kshl.kshlib.misc.UUIDHelper;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

public class UUIDAPI2 {
    public static final String BEDROCK_PREFIX = ".";
    private final BiDiMapCache<String, UUID> cache = new BiDiMapCache<>(3600000L, 3600000L, true);

    private static final MojangNetSessionAPI instanceMojangNetSessionAPI = new MojangNetSessionAPI();
    private static final GeyserMCAPI instanceGeyserMCAPI = new GeyserMCAPI();
    private static final MojangNetAPI instanceMojangNetAPI = new MojangNetAPI();

    @Nullable
    public UUID getUUIDFromUsername(String username) throws IOException, BusyException {
        return Optional.ofNullable(getUUIDAndNameFromUsername(username)).map(Pair::getValue).orElseThrow();
    }

    @Nullable
    public NameUUID getUUIDAndNameFromUsername(String username) throws IOException, BusyException {
        cache.cleanup();
        UUID out = cache.get(username);
        if (out != null) return new NameUUID(cache.getKey(out), out);
        NameUUID nameUUID;
        if (username.startsWith(BEDROCK_PREFIX)) {
            nameUUID = instanceGeyserMCAPI.getFloodgateUIDFromGamerTag(username);
        } else {
            nameUUID = instanceMojangNetAPI.getJavaUUIDFromUsername(username);
        }
        if (nameUUID == null || nameUUID.getValue() == null) return null;
        cache.put(nameUUID.getKey(), nameUUID.getValue());
        return nameUUID;
    }

    @Nullable
    public String getUsernameFromUUID(UUID uuid) throws IOException, BusyException {
        cache.cleanup();
        String name = cache.getKey(uuid);
        if (name != null) return name;
        if (uuid.getMostSignificantBits() == 0) {
            name = instanceGeyserMCAPI.getGamerTagFromFloodgateUID(uuid);
        } else {
            name = instanceMojangNetSessionAPI.getJavaUsernameFromUUID(uuid);
        }
        if (name == null) return null;
        cache.put(name, uuid);

        return name;
    }

    public static class NameUUID extends Pair<String, UUID> {

        public NameUUID(String key, UUID value) {
            super(key, value);
        }
    }

    private static class MojangNetAPI extends NetUtilInterval {

        private MojangNetAPI() {
            super("https://api.mojang.com/users/profiles/minecraft/", 1000);
        }

        NameUUID getJavaUUIDFromUsername(String username) throws IOException, BusyException {
            JSONObject response = getResponse(username).getJSON();
            String uuidStr = response.optString("id", null);
            return new NameUUID(response.optString("name", null), uuidStr == null ? null : UUIDHelper.fromString(uuidStr));
        }
    }

    private static class MojangNetSessionAPI extends NetUtilInterval {

        private MojangNetSessionAPI() {
            super("https://sessionserver.mojang.com/session/minecraft/profile/", 1000);
        }

        @Nullable
        String getJavaUsernameFromUUID(UUID uuid) throws IOException, BusyException {
            return getResponse(uuid.toString()).getJSON(new JSONObject()).optString("name", null);
        }

    }

    private static class GeyserMCAPI extends NetUtilInterval {
        private GeyserMCAPI() {
            super("https://api.geysermc.org/v2/", 1000);
        }

        NameUUID getFloodgateUIDFromGamerTag(String username) throws IOException, BusyException {
            NetUtil.Response response = getResponse(String.format("utils/uuid/bedrock_or_java/%s?prefix=%s", username, BEDROCK_PREFIX));
            String name = response.getJSON().optString("name", null);
            String id = response.getJSON().optString("id", null);
            if (name == null || id == null) {
                String message = response.getJSON().optString("message", null);
                if (message != null && message.toLowerCase().contains("unable to find")) {
                    return null;
                }
                throw new IOException("Invalid response, code=" + response.getResponseCode() + ", getJSON=" + response.getJSON());
            }
            return new NameUUID(name, UUIDHelper.fromString(id));
        }

        String getGamerTagFromFloodgateUID(UUID uuid) throws IOException, BusyException {
            NetUtil.Response response = getResponse("xbox/gamertag/" + uuid.getLeastSignificantBits());
            String gamertag = response.getJSON().optString("gamertag", null);
            if (gamertag == null) {
                String message = response.getJSON().optString("message", null);
                if (message != null && message.toLowerCase().contains("unable to find")) {
                    return null;
                }
                throw new IOException("Invalid response, code=" + response.getResponseCode() + ", getJSON=" + response.getJSON());
            }
            return BEDROCK_PREFIX + gamertag;
        }
    }
}
