package dev.kshl.kshlib.spigot.protocollib;

import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import dev.kshl.kshlib.exceptions.BusyException;
import dev.kshl.kshlib.platform.MojangAPI;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.UUID;

public record Skin(UUID uuid, String skin, String signature) {
    public WrappedSignedProperty wrap() {
        return new WrappedSignedProperty("textures", skin, signature);
    }

    public static Skin getSkin(UUID uuid) throws IOException {
        JSONObject response;
        try {
            response = MojangAPI.sessionServer.getResponse("session/minecraft/profile/" + uuid + "?unsigned=false").getJSON();
        } catch (BusyException e) {
            throw new RuntimeException(e);
        }
        JSONObject props = response.optJSONArray("properties",new JSONArray()).optJSONObject(0, null);
        if (props == null) return null;
        return new Skin(uuid, (String) props.get("value"), (String) props.get("signature"));
    }
}
