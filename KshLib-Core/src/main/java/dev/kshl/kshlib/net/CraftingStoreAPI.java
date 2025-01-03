package dev.kshl.kshlib.net;

import dev.kshl.kshlib.exceptions.BusyException;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.io.IOException;

public class CraftingStoreAPI extends NetUtilInterval {
    private final String token;

    public CraftingStoreAPI(String token) {
        super("https://api.craftingstore.net/v7", 1100L);
        this.token = token;
    }

    @Nullable
    public String makeGiftCard(int amount, String note) throws IOException, BusyException {
        String[] headers = new String[]{ //
                "token", token, //
                "Content-Type", "application/getJSON" //
        };
        JSONObject result = postResponse("gift-cards", new JSONObject().put("amount", String.valueOf(amount)).put("note", note).toString(), headers).getJSON();

        if (!result.has("success")) return null;
        if (!result.getBoolean("success")) return null;
        if (!result.has("data")) return null;
        JSONObject data = result.getJSONObject("data");
        if (!data.has("code")) return null;
        return data.getString("code");
    }
}
