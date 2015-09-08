
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getKeyType() {
        return keyType;
    }

    public void setKeyType(String keyType) {
        this.keyType = keyType;
    }

    public Integer getMaxAllowedRetries() {
        return maxAllowedRetries;
    }

    public Integer getRequestTimeout() {
        return requestTimeout;
    }

    public Integer getActivationCodeExpiration() {
        return activationCodeExpiration;
    }

    public Boolean getRequestPollingEnabled() {
        return requestPollingEnabled;
    }

    public Boolean getBypassEnabled() {
        return bypassEnabled;
    }

}
