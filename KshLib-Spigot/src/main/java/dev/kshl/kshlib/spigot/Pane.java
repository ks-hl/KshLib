package dev.kshl.kshlib.spigot;

import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Pane implements InventoryHolder {
    public final PaneType type;
    private final PaneListener listener;
    private final HumanEntity player;
    private final Map<Integer, ButtonAction> buttons = new HashMap<>();
    private Inventory inventory;
    private boolean cancelled;

    public Pane(PaneListener listener, PaneType type, HumanEntity player) {
        assert listener != null && type != null && player != null;
        this.listener = listener;
        this.type = type;
        this.player = player;

        listener.registerNewPane(this);
    }

    public final ItemStack addButton(int slot, Material type, @Nullable ButtonAction buttonAction, @Nullable String name, @Nullable List<String> lore) {
        if (inventory == null) throw new IllegalStateException("Inventory not set before adding button");

        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null) meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return addButton(slot, item, buttonAction);
    }

    public final ItemStack addButton(int slot, ItemStack item, @Nullable ButtonAction buttonAction) {
        if (inventory == null) throw new IllegalStateException("Inventory not set before adding button");
        inventory.setItem(slot, item);
        buttons.put(slot, buttonAction);
        return item;
    }

    public final ItemStack addButton(int slot, Material type, @Nullable Runnable buttonAction, @Nullable String name, @Nullable List<String> lore) {
        return addButton(slot, type, (pane, slot1, item1) -> {
            if (buttonAction != null) buttonAction.run();
        }, name, lore);
    }

    public final ItemStack addButton(int slot, ItemStack item, @Nullable Runnable buttonAction) {
        return addButton(slot, item, (pane, slot1, item1) -> {
            if (buttonAction != null) buttonAction.run();
        });
    }

    public final ItemStack addButton(int slot, Material type, @Nullable String name, @Nullable List<String> lore) {
        return addButton(slot, type, (Runnable) null, name, lore);
    }

    public final ItemStack addButton(int slot, ItemStack item) {
        return addButton(slot, item, (Runnable) null);
    }

    /**
     * Called when a slot in the inventory is clicked. Can be overridden and super does not need to be called.
     *
     * @param slot The slot that was clicked
     */
    public void onClick(int slot) {
    }

    public void onClick(InventoryClickEvent e) {
    }

    /**
     * Called when the inventory is closed. Can be overridden and super does not need to be called.
     */
    public void onClose() {
    }

    public final void close() {
        close(true);
    }

    protected final void close(boolean closeInventory) {
        if (cancelled) return;
        int count = 0;
        for (StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()) {
            if (stackTraceElement.getClassName().equals(getClass().getName()) && stackTraceElement.getMethodName().equals("close_")) {
                count++;
            }
        }
        if (count >= 20) throw new IllegalStateException("Infinite loop?");
        if (closeInventory && player.getOpenInventory().getTopInventory().equals(inventory)) player.closeInventory();
        onClose();
        cancelled = true;
        if (inventory != null) inventory.clear();

        listener.remove(this);
    }

    final void click(int slot) {
        if (cancelled) return;

        onClick(slot);

        ButtonAction button = getButton(slot);
        if (button != null) button.onClick(this, slot, inventory.getItem(slot));
    }

    public ButtonAction getButton(int slot) {
        return buttons.get(slot);
    }

    @FunctionalInterface
    public interface ButtonAction {
        void onClick(Pane pane, int slot, ItemStack item);
    }

    @Override
    public final @Nonnull Inventory getInventory() {
        if (inventory == null) throw new IllegalStateException("Inventory not set before getting inventory");
        return inventory;
    }

    public final void setInventory(Inventory inventory) {
        if (this.inventory != null) throw new IllegalStateException("Attempted to re-set inventory instance");
        this.inventory = inventory;
    }

    public HumanEntity getPlayer() {
        return player;
    }
}