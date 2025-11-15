package dev.kshl.kshlib.spigot;

import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTCompoundList;
import de.tr7zw.nbtapi.NBTItem;
import de.tr7zw.nbtapi.NBTTileEntity;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBTCompoundList;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.json.JSONObject;

import java.util.Base64;

public class SkullUtil {
    public static ItemStack createHeadItem(String base64) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        setTexture(item, base64);
        return item;
    }

    public static void setTexture(ItemStack item, String base64) {
        validateType(item.getType());
        NBT.modifyComponents(item, nbt -> {
            setTexture(nbt, base64);
        });
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

    private static void setTexture(ReadWriteNBT baseNBT, String base64) {
        ReadWriteNBT profile = baseNBT.resolveOrCreateCompound("profile");
        ReadWriteNBTCompoundList properties = profile.getCompoundList("properties");
        properties.clear();
        ReadWriteNBT texture = properties.addCompound();
        texture.setString("name", "textures");
        texture.setString("value", base64);
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
