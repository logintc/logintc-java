
package com.cyphercor.logintc.resource;

import java.util.Locale;

/**
 * A token object is a LoginTC credential tied to a domain and user pair. The
 * LoginTC credential lives on the LoginTC mobile app on the user's mobile
 * device.
 */
public class Token {

    public static enum State {
        /**
         * A code to load the token has been issued to a user but it has not yet
         * been loaded.
         */
        PENDING,

        /**
         * A code has been used to load the token.
         */
        ACTIVE;
    }

    private State state = null;
    private String code = null;

    /**
     * @param state The state that the token is in.
     * @param code The code to load the token.
     */
    public Token(State state, String code) {
        this.state = state;

        if (code != null) {
            this.code = code.toUpperCase(Locale.ENGLISH);
        }
    }

    public State getState() {
        return this.state;
    }

    public String getCode() {
        return this.code;
    }
}
