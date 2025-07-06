package dev.kshl.kshlib.spigot;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class PaneListener implements Listener {
    private final Set<Pane> openPanes = new HashSet<>();

    public Pane newPane(PaneType type, HumanEntity player) {
        return new Pane(this, type, player);
    }

    public Pane newPane(PaneType type, HumanEntity player, int size, String title) {
        Pane pane = newPane(type, player);
        Inventory inventory = Bukkit.createInventory(pane, size, title);
        pane.setInventory(inventory);
        return pane;
    }

    protected void registerNewPane(Pane pane) {
        openPanes.add(pane);
    }

    public void shutdown() {
        openPanes.forEach(Pane::close);
    }

    void remove(Pane pane) {
        openPanes.remove(pane);
    }

    @EventHandler
    public void onInventoryClickEvent(InventoryClickEvent e) {
        if (e.getInventory().getHolder() instanceof Pane pane) {
            final boolean isClickTop = Objects.equals(e.getInventory(), e.getClickedInventory());
            if (!pane.type.isAllowed(e.getAction(), isClickTop)) {
                e.setCancelled(true);
                return;
            }
            if (pane.type != PaneType.PASSIVE) e.setCancelled(true);
            if (pane.type == PaneType.CREATIVE) {
                if (e.getClickedInventory() != null) {
                    if (e.getClickedInventory().equals(e.getInventory())) {
                        ItemStack cursor = e.getCursor();

                        if (e.getCurrentItem() != null) {
                            e.getCurrentItem().setAmount(1);
                            boolean isSimilar = cursor != null && cursor.isSimilar(e.getCurrentItem());
                            if (cursor == null || cursor.getType() == Material.AIR || isSimilar) {
                                if (isSimilar && e.getAction() != InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                                    if (cursor.getAmount() >= 64) return;
                                    cursor.setAmount(cursor.getAmount() + 1);
                                    return;
                                }

                                cursor = e.getCurrentItem().clone();

                                if (e.getAction() == InventoryAction.CLONE_STACK || e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                                    cursor.setAmount(cursor.getMaxStackSize());
                                }
                                e.getWhoClicked().setItemOnCursor(cursor);
                                return;
                            }
                        }
                    } else {
                        if (e.getAction() != InventoryAction.MOVE_TO_OTHER_INVENTORY && e.getAction() != InventoryAction.HOTBAR_SWAP) {
                            e.setCancelled(false);
                        }
                    }
                }
                return;
            }
            if (pane.type == PaneType.ENTITY_INVENTORY && (e.getSlot() >= 9 || !Objects.equals(e.getClickedInventory(), e.getInventory()))) {
                e.setCancelled(false);
                return;
            }
            if (pane.type == PaneType.FILTER || pane.type == PaneType.FILTER_SINGLE) {
                if (!isClickTop) {
                    e.setCancelled(false); // Let them click their own inventory
                } else if (pane.getButton(e.getSlot()) == null) {
                    ItemStack set = e.getCursor();
                    if (set == null) set = new ItemStack(Material.AIR);
                    else {
                        set = set.clone();
                        if (pane.type == PaneType.FILTER_SINGLE) set.setAmount(1);
                    }
                    boolean doSet = true;
                    if (set.getType() != Material.AIR) {
                        for (ItemStack item : pane.getInventory()) {
                            if (item == null) continue;
                            if (item.getType() == set.getType()) doSet = false;
                        }
                    }
                    if (doSet) pane.getInventory().setItem(e.getSlot(), set);
                }
            }
            if (isClickTop) {
                pane.onClick(e);
                pane.click(e.getSlot());
            }
        }
    }

    @EventHandler
    public void on(InventoryOpenEvent e) {
        if (e.getInventory().getHolder() != null && e.getInventory().getHolder() instanceof Pane pane) {
            if (e.getPlayer().equals(pane.getPlayer())) return;
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDragEvent(InventoryDragEvent e) {
        if (e.getInventory().getHolder() != null && e.getInventory().getHolder() instanceof Pane) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryCloseEvent(InventoryCloseEvent e) {
        int count = 0;
        for (StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()) {
            if (stackTraceElement.getClassName().equals(getClass().getName()) && stackTraceElement.getMethodName().equals("onInventoryCloseEvent")) {
                count++;
            }
        }
        if (count >= 10) throw new IllegalStateException("Infinite loop?");
        if (e.getInventory().getHolder() != null && e.getInventory().getHolder() instanceof Pane pane) {
            pane.close(false);
        }
    }
}
