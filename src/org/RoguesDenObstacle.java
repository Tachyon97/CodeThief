package org;

import org.dreambot.api.methods.map.Tile;

/**
 * Represents an obstacle in the Rogues' Den maze.
 * Based on the original Obstacles class from the provided code.
 */
public class RoguesDenObstacle {
    private final Tile tile;
    private final String hint;
    private final int objectId;
    private final int waitTime;

    /**
     * Creates a new obstacle with default wait time
     *
     * @param x        The x-coordinate
     * @param y        The y-coordinate
     * @param objectId The object ID, or -1 if not an object
     * @param hint     The hint for this obstacle
     */
    public RoguesDenObstacle(int x, int y, int objectId, String hint) {
        this(x, y, objectId, hint, 0);
    }

    /**
     * Creates a new obstacle with a specific wait time
     *
     * @param x        The x-coordinate
     * @param y        The y-coordinate
     * @param objectId The object ID, or -1 if not an object
     * @param hint     The hint for this obstacle
     * @param waitTime The time to wait after interacting with this obstacle
     */
    public RoguesDenObstacle(int x, int y, int objectId, String hint, int waitTime) {
        this.tile = new Tile(x, y, 1); // Rogues' Den is on plane 1
        this.objectId = objectId;
        this.hint = hint;
        this.waitTime = waitTime;
    }

    /**
     * Gets the tile for this obstacle
     *
     * @return The tile
     */
    public Tile getTile() {
        return tile;
    }

    /**
     * Gets the hint for this obstacle
     *
     * @return The hint
     */
    public String getHint() {
        return hint;
    }

    /**
     * Gets the object ID for this obstacle
     *
     * @return The object ID, or -1 if not an object
     */
    public int getObjectId() {
        return objectId;
    }

    /**
     * Gets the wait time for this obstacle
     *
     * @return The wait time in milliseconds
     */
    public int getWaitTime() {
        return waitTime;
    }
}