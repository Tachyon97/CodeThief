package org;

import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.NPC;

/**
 * Wrapper class to make stalls interact like NPCs when needed.
 * This allows unified handling of different thieving methods.
 */
public class StallWrapper extends NPC {
    private final GameObject stallObject;

    /**
     * Creates a new StallWrapper for a stall game object
     *
     * @param stallObject The stall game object
     */
    public StallWrapper(GameObject stallObject) {
        super(null); // Pass null entity as we're overriding methods
        this.stallObject = stallObject;
    }

    @Override
    public String getName() {
        return stallObject.getName();
    }

    @Override
    public boolean exists() {
        return stallObject.exists();
    }

    @Override
    public boolean interact(String action) {
        // For stalls, primary action is "Steal-from"
        return stallObject.interact(action);
    }

    @Override
    public int getX() {
        return stallObject.getX();
    }

    @Override
    public int getY() {
        return stallObject.getY();
    }

    @Override
    public int getZ() {
        return stallObject.getZ();
    }

    /**
     * Gets the underlying GameObject
     *
     * @return The stall GameObject
     */
    public GameObject getStallObject() {
        return stallObject;
    }
}