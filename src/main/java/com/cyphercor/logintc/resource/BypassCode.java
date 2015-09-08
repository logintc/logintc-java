
package com.cyphercor.logintc.resource;

import java.util.Date;

/**
 * A bypass code can be used as an alternative authentication method in the
 * event users misplace their 2nd factor device. A bypass code is a
 * user-specific 9 digit numerical code. Each user can have up to 5 different
 * codes. In order for a user to login using their bypass codes, the domain they
 * are accessing must have bypass code authentication enabled.
 */
public class BypassCode {

    private String id = null;
    private String code = null;
    private Date dtExpiry = null;
    private String user = null;
    private Integer usesAllowed = null;
    private Integer usesRemaining = null;

    /**
     * @param id The bypass code's identifier.
     * @param code The 9 digit bypass code
     * @param dtExpiry The date which the bypass code expires
     * @param user The user's identifier.
     * @param usesAllowed The number of uses originally allowed
     * @param usesRemaining The number of uses remaining
     */
    public BypassCode(String id, String code, Date dtExpiry, String user, Integer usesAllowed, Integer usesRemaining) {
        this.id = id;
        this.code = code;
        this.dtExpiry = dtExpiry;
        this.user = user;
        this.usesAllowed = usesAllowed;
        this.usesRemaining = usesRemaining;
    }

    public String getId() {
        return this.id;
    }

    public String getCode() {
        return this.code;
    }

    public Date getDtExpiry() {
        return this.dtExpiry;
    }

    public String getUser() {
        return this.user;
    }

    public Integer getUsesAllowed() {
        return this.usesAllowed;
    }

    public Integer getUsesRemaining() {
        return this.usesRemaining;
    }
}
