
package com.cyphercor.logintc.resource;

/**
 * A session is an authentication request sent to a user. Creating a session
 * initiates a push notification to the user's mobile device.
 */
public class Session {

    public static enum State {
        /**
         * The recipient has not yet responded to the request.
         */
        PENDING,

        /**
         * The recipient has rejected the request.
         */
        APPROVED,

        /**
         * The recipient has denied the request.
         */
        DENIED;
    }

    private String id = null;
    private State state = null;

    /**
     * @param id The session's identifier.
     * @param state The state of the session.
     */
    public Session(String id, State state) {
        this.id = id;
        this.state = state;
    }

    public String getId() {
        return this.id;
    }

    public State getState() {
        return this.state;
    }
}
