package de.tarent.telekom.cot.mqtt.util;

/**
 * Contains the 3 statuses for the bootstrapping process:
 *  1. "notStarted" (Bootstrapping process was not yet started, no secret is available)
 *  2. "ongoing" (Bootstrapping process was started, but not finished and no secret is available)
 *  3. "bootstrapped" (Bootstrapping process was completed, and a secret is available)
 */
public enum Bootstrapped {
    NOT_STARTED("notStarted"),
    ONGOING("ongoing"),
    BOOTSTRAPPED("bootstrapped");

    private String status;

    Bootstrapped(final String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
