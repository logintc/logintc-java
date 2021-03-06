
package com.cyphercor.logintc.resource;

/**
 * A domain object represents a service (e.g. VPN or website) and contains a collection of users and token unlocking policies (e.g. key,
 * passcode, minimum lengths). A domain id consists of a unique 40-character hexadecimal unique identifier.
 */
public class Domain {
    private String id;
    private String name;
    private String type;
    private String keyType;
    private Integer maxAllowedRetries;
    private Integer requestTimeout;
    private Integer activationCodeExpiration;
    private Boolean requestPollingEnabled;
    private Boolean bypassEnabled;

    /**
     * @param id The domain 40-character hexadecimal unique identifier.
     * @param name The domain real name
     * @param type The domain type
     * @param keyType The domain keyType
     * @param maxAllowedRetries Number of invalid retries allowed before token is revoked
     * @param requestTimeout Timeout of a request in seconds
     * @param activationCodeExpiration Activation code expiration in days
     * @param requestPollingEnabled Whether request polling is enabled
     * @param bypassEnabled Whether bypass codes are enabled
     */
    public Domain(String id, String name, String type, String keyType, Integer maxAllowedRetries, Integer requestTimeout,
            Integer activationCodeExpiration, Boolean requestPollingEnabled, Boolean bypassEnabled) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.keyType = keyType;
        this.maxAllowedRetries = maxAllowedRetries;
        this.requestTimeout = requestTimeout;
        this.activationCodeExpiration = activationCodeExpiration;
        this.requestPollingEnabled = requestPollingEnabled;
        this.bypassEnabled = bypassEnabled;
    }

    /**
     * @return The domain's identifier.
     */
    public String getId() {
        return id;
    }

    /**
     * @param id The domain's identifier.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return The domain's name.
     */
    public String getName() {
        return name;
    }

    /**
     * @param name The domain's name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return The domain's type.
     */
    public String getType() {
        return type;
    }

    /**
     * @param type The domain's type.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return The domain's key type.
     */
    public String getKeyType() {
        return keyType;
    }

    /**
     * @param keyType The domain's key type.
     */
    public void setKeyType(String keyType) {
        this.keyType = keyType;
    }

    /**
     * @return The domain's max allowed retries.
     */
    public Integer getMaxAllowedRetries() {
        return maxAllowedRetries;
    }

    /**
     * @return The domain's request timeout.
     */
    public Integer getRequestTimeout() {
        return requestTimeout;
    }

    /**
     * @return The domain's activation code expiration.
     */
    public Integer getActivationCodeExpiration() {
        return activationCodeExpiration;
    }

    /**
     * @return The domain's request polling enabled.
     */
    public Boolean getRequestPollingEnabled() {
        return requestPollingEnabled;
    }

    /**
     * @return The domain's bypass codes enabled.
     */
    public Boolean getBypassEnabled() {
        return bypassEnabled;
    }

}
