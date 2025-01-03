package dev.kshl.kshlib.spigot;

import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTCompoundList;
import de.tr7zw.nbtapi.NBTContainer;
import de.tr7zw.nbtapi.NBTItem;
import de.tr7zw.nbtapi.NBTListCompound;
import de.tr7zw.nbtapi.NBTTileEntity;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Base64;
import java.util.UUID;

public class SkullUtil {
    public static ItemStack createHeadItem(String base64) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        setTexture(item, base64);
        return item;
    }

    public static void setTexture(ItemStack item, String base64) {
        validateType(item.getType());
        NBTItem headNBT = new NBTItem(item);
        setTexture(headNBT, base64);
        headNBT.applyNBT(item);
    }

    public static void setTexture(Block block, String base64) {
        validateType(block.getType());
        setTexture(new NBTTileEntity(block.getState()), base64);
    }

    public static String getTexture(ItemStack item) {
        validateType(item.getType());
        return getTexture(new NBTItem(item));
    }

    public static String getTexture(Block block) {
        validateType(block.getType());
        return getTexture(new NBTTileEntity(block.getState()));
    }

    private static void validateType(Material material) {
        if (material == Material.PLAYER_HEAD) return;
        if (material == Material.PLAYER_WALL_HEAD) return;

        throw new IllegalArgumentException("Cannot set skull texture of " + material);
    }

    private static void setTexture(NBTCompound baseNBT, String base64) {
        base64 = filterTexture(base64);

        NBTCompound baseNBTToMerge = new NBTContainer();
        NBTCompound skullOwner = baseNBTToMerge.getOrCreateCompound("SkullOwner");
        skullOwner.setUUID("Id", UUID.randomUUID());
        NBTCompound properties = skullOwner.getOrCreateCompound("Properties");
        NBTCompoundList textures = properties.getCompoundList("textures");
        textures.clear();
        NBTListCompound texture = textures.addCompound();
        texture.setString("Value", base64);
        baseNBT.mergeCompound(baseNBTToMerge);
    }

    private static String getTexture(NBTCompound baseNBT) {
        NBTCompound skullOwner = baseNBT.getCompound("SkullOwner");
        if (skullOwner == null) return null;

        NBTCompound properties = skullOwner.addCompound("Properties");
        if (properties == null) return null;

        NBTCompoundList textures = properties.getCompoundList("textures");
        if (textures == null || textures.isEmpty()) return null;

        NBTCompound texture = textures.get(0);
        if (texture == null) return null;

        return texture.getString("Value");
    }

    /**
     * Strips any extra data from the encoded texture such as signatureRequired, profileName, etc.
     */
    public static String filterTexture(String texture) {
        String jsonTextIn = new String(Base64.getDecoder().decode(texture));
        JSONObject jsonIn;
        try {
            jsonIn = new JSONObject(jsonTextIn);
            JSONObject jsonOut = new JSONObject().put("textures", new JSONObject().put("SKIN", jsonIn.getJSONObject("textures").getJSONObject("SKIN")));
            return Base64.getEncoder().encodeToString(jsonOut.toString().getBytes());
        } catch (JSONException e) {
            throw new JSONException(jsonTextIn, e);
        }
    }

    public static String getTextureFromMojangID(String mojangID) {
        if (!mojangID.matches("[a-z0-f]{63,65}")) {
            throw new IllegalArgumentException("Invalid mojangID. Must be 63-65 character hexadecimal.");
        }

        JSONObject skin = new JSONObject().put("url", "http://textures.minecraft.net/texture/" + mojangID);

        JSONObject textures = new JSONObject().put("SKIN", skin);

        JSONObject json = new JSONObject().put("textures", textures);

        return Base64.getEncoder().encodeToString(json.toString().getBytes());
    }
}
