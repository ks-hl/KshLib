package dev.kshl.kshlib.spigot;

import org.bukkit.event.inventory.InventoryAction;

import java.util.Set;

public enum PaneType {
    /**
     * A buy confirmation GUI
     */
    BUY,
    /**
     * Read only
     */
    READ_ONLY,
    /**
     * The inventory of some entity
     */
    ENTITY_INVENTORY,
    /**
     * Allows items to be taken infinitely
     */
    CREATIVE,

    /**
     * Allows items to be added/removed without changing the quantity the user has. Used for creating item filters.
     */
    FILTER,

    /**
     * Same as {@link PaneType#FILTER}, but ignores the quantity of the item added.
     */
    FILTER_SINGLE,

    /**
     * Does nothing on its own
     */
    PASSIVE;

    private static final Set<InventoryAction> allowedActions = Set.of( //
            InventoryAction.PICKUP_ALL, //
            InventoryAction.PICKUP_SOME, //
            InventoryAction.PICKUP_HALF, //
            InventoryAction.PICKUP_ONE, //
            InventoryAction.PLACE_ALL, //
            InventoryAction.PLACE_SOME, //
            InventoryAction.PLACE_ONE, //
            InventoryAction.SWAP_WITH_CURSOR, //
            InventoryAction.DROP_ALL_CURSOR, //
            InventoryAction.DROP_ONE_CURSOR, //
            InventoryAction.DROP_ALL_SLOT, //
            InventoryAction.DROP_ONE_SLOT //
    );

    private static final Set<InventoryAction> pickup = Set.of( //
            InventoryAction.PICKUP_ALL, //
            InventoryAction.PICKUP_SOME, //
            InventoryAction.PICKUP_HALF, //
            InventoryAction.PICKUP_ONE //
    );

    private static final Set<InventoryAction> drop = Set.of( //
            InventoryAction.DROP_ALL_CURSOR, //
            InventoryAction.DROP_ONE_CURSOR, //
            InventoryAction.DROP_ALL_SLOT, //
            InventoryAction.DROP_ONE_SLOT //
    );


    public boolean isAllowed(InventoryAction inventoryAction, boolean isTopInventory) {
        if (this == CREATIVE) return true;
        if (!allowedActions.contains(inventoryAction)) return false;
        return switch (this) {
            case BUY, READ_ONLY ->
                    isTopInventory && pickup.contains(inventoryAction) && !drop.contains(inventoryAction);
            case FILTER, FILTER_SINGLE -> !drop.contains(inventoryAction);
            case ENTITY_INVENTORY, PASSIVE -> true;

            default -> throw new IllegalStateException("Unexpected value: " + this);
        };
    }
}
