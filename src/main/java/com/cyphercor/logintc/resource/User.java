
package com.cyphercor.logintc.resource;

import java.util.List;

/**
 * A user object represents a person or an account. A user may belong to many domains and may have many tokens on many devices. A user has a
 * 40-character hexadecimal unique identifier and an optional name and email address. A LoginTC user object generally corresponds one-to-one
 * with your application's user object.
 */
public class User {

    private String id = null;
    private String username = null;
    private String email = null;
    private String name = null;
    private List<String> domains = null;
    private List<String> bypassCodes = null;
    private String hardware = null;

    /**
     * @param username A unique 1-128 character username.
     * @param email The user's email address.
     * @param name The user's real name (or optionally username).
     */
    public User(String username, String email, String name) {
        this.username = username;
        this.email = email;
        this.name = name;
    }

    /**
     * @param id The user's identifier.
     * @param username A unique 1-128 character username.
     * @param email The user's email address.
     * @param name The user's real name (or optionally username).
     * @param domains The user's domain memberships.
     * @param bypassCodes The user's bypass codes.
     * @param hardware The user's hardware token.
     */
    public User(String id, String username, String email, String name, List<String> domains, List<String> bypassCodes, String hardware) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.name = name;
        this.domains = domains;
        this.bypassCodes = bypassCodes;
        this.hardware = hardware;
    }

    /**
     * @param email The user's email.
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * @param name The user's new name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return The user's identifier.
     */
    public String getId() {
        return this.id;
    }

    /**
     * @return The user's username.
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * @return The user's email.
     */
    public String getEmail() {
        return this.email;
    }

    /**
     * @return The user's name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return The user's domain memberships.
     */
    public List<String> getDomains() {
        return this.domains;
    }

    /**
     * @return The user's bypass codes.
     */
    public List<String> getBypassCodes() {
        return this.bypassCodes;
    }

    /**
     * @return The user's hardware token identifier.
     */
    public String getHardware() {
        return this.hardware;
    }
}
