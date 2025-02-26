package org;

/**
 * Represents all possible states for the thieving script.
 */
public enum ThievingState {
    INITIALIZE,
    WALKING_TO_LOCATION,
    THIEVING,
    HANDLING_INVENTORY,
    WALKING_TO_BANK,
    BANKING,
    HANDLING_HEALTH,
    ROGUES_DEN,
    ERROR
}
