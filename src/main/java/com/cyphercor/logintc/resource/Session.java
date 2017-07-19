
package com.cyphercor.logintc.resource;

/**
 * A session is an authentication request sent to a user. Creating a session initiates a push notification to the user's mobile device.
 */
public class Session {

    /**
     * Various states that Sessions can take on.
     */
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

    /**
     * @return The sessions's identifier.
     */
    public String getId() {
        return this.id;
    }

    /**
     * @return The sessions's state.
     */
    public State getState() {
        return this.state;
    }
}
