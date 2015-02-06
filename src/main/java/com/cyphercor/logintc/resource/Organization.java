
package com.cyphercor.logintc.resource;

/**
 * The Organization is the firm, group, or institution to which the services you
 * wish to protect belong. It is the entity with which your domains and users
 * will be associated.
 */
public class Organization {
    private String name;

    /**
     * @param name Name of the organization
     */
    public Organization(String name)
    {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
