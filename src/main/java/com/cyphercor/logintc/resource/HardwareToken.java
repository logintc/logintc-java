
package com.cyphercor.logintc.resource;

/**
 * A hardware token is a physical authentication method. A hardware token must implement RFC 6238 (TOTP). Each user can have one hardware
 * token. In order for a user to login using their hardware token, the domain they are accessing must have hardware token authentication
 * enabled.
 */
public class HardwareToken {

    private String id = null;
    private String alias = null;
    private String serialNumber = null;
    private String type = null;
    private String timeStep = null;
    private String syncState = null;
    private String user = null;

    /**
     * @param id The hardware token's identifier.
     * @param alias A short-hand mutable name
     * @param serialNumber The serial number of the hardware token
     * @param type Can be either TOTP6 or TOTP8
     * @param timeStep The number of seconds for the time step
     * @param syncState The state of the hardware token
     * @param user The user's identifier.
     */
    public HardwareToken(String id, String alias, String serialNumber, String type, String timeStep, String syncState, String user) {
        this.id = id;
        this.alias = alias;
        this.serialNumber = serialNumber;
        this.type = type;
        this.timeStep = timeStep;
        this.syncState = syncState;
        this.user = user;
    }

    /**
     * @return The hardware token's identifier.
     */
    public String getId() {
        return this.id;
    }

    /**
     * @return The hardware token's alias.
     */
    public String getAlias() {
        return this.alias;
    }

    /**
     * @return The hardware token's serial number.
     */
    public String getSerialNumber() {
        return this.serialNumber;
    }

    /**
     * @return The hardware token's type.
     */
    public String getType() {
        return this.type;
    }

    /**
     * @return The hardware token's time step.
     */
    public String getTimeStep() {
        return this.timeStep;
    }

    /**
     * @return The hardware token's sync state.
     */
    public String getSyncState() {
        return this.syncState;
    }

    /**
     * @return The hardware token's associated user identifier.
     */
    public String getUser() {
        return this.user;
    }
}
