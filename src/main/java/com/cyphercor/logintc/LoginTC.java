
package com.cyphercor.logintc;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.cyphercor.logintc.AdminRestClient.AdminRestClientException;
import com.cyphercor.logintc.AdminRestClient.RestAdminRestClientException;
import com.cyphercor.logintc.resource.BypassCode;
import com.cyphercor.logintc.resource.Domain;
import com.cyphercor.logintc.resource.HardwareToken;
import com.cyphercor.logintc.resource.Organization;
import com.cyphercor.logintc.resource.Session;
import com.cyphercor.logintc.resource.Token;
import com.cyphercor.logintc.resource.User;

/**
 * LoginTC Admin client to manage LoginTC users, domains, tokens and sessions.
 */
public class LoginTC {

    private static final String NAME = "LoginTC-Java";
    private static final String VERSION = "1.1.4";

    private static final SimpleDateFormat DATE_FORMAT_ISO8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    /**
     * A generic LoginTC client exception.
     */
    public class LoginTCException extends Exception {
        private static final long serialVersionUID = 3112741136967467568L;

        /**
         * @param message The error message.
         */
        public LoginTCException(String message) {
            super(message);
        }

        /**
         * @param throwable The nested throwable.
         */
        public LoginTCException(Throwable throwable) {
            super(throwable);
        }

        /**
         * Constructor for generic LoginTC client exception.
         */
        public LoginTCException() {
            super();
        }
    }

    /**
     * Exception caused by internal client exception.
     */
    public class InternalLoginTCException extends LoginTCException {
        private static final long serialVersionUID = -7310070273601778377L;

        /**
         * @param throwable The nested throwable.
         */
        public InternalLoginTCException(Throwable throwable) {
            super(throwable);
        }
    }

    /**
     * Exception for failures because of API.
     */
    public class ApiLoginTCException extends LoginTCException {
        private static final long serialVersionUID = 2957085008596633695L;

        private String errorCode = null;
        private String errorMessage = null;

        /**
         * @param errorCode The error code.
         * @param errorMessage The error message.
         */
        public ApiLoginTCException(String errorCode, String errorMessage) {
            super(String.format("%s: %s", errorCode, errorMessage));

            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }

        /**
         * @return The error code.
         */
        public String getErrorCode() {
            return this.errorCode;
        }

        /**
         * @return The error message.
         */
        public String getErrorMessage() {
            return this.errorMessage;
        }
    }

    /**
     * Exception for failure because of no valid token for the specified user and domain. This means the token doesn't exist, it's not yet
     * loaded, or it has been revoked.
     */
    public class NoTokenLoginTCException extends ApiLoginTCException {
        private static final long serialVersionUID = -5878018652693276417L;

        /**
         * @param errorCode The error code.
         * @param errorMessage The error message.
         */
        public NoTokenLoginTCException(String errorCode, String errorMessage) {
            super(errorCode, errorMessage);
        }
    }

    /**
     * Factory for LoginTCException exceptions.
     */
    private class LoginTCExceptionFactory {
        protected LoginTCException createException(RestAdminRestClientException restAdminClientException) {
            LoginTCException exception = new InternalLoginTCException(restAdminClientException);

            try {
                JSONObject jsonObject = getJson(restAdminClientException.getBody());
                JSONArray errorsArray = jsonObject.getJSONArray("errors");

                JSONObject jsonError = errorsArray.getJSONObject(0);
                String errorCode = jsonError.getString("code");
                String errorMessage = jsonError.getString("message");

                if (errorCode.equals("api.error.notfound.token")) {
                    exception = new NoTokenLoginTCException(errorCode, errorMessage);
                } else {
                    exception = new ApiLoginTCException(errorCode, errorMessage);
                }
            } catch (JSONException e) {
                return exception;
            }

            return exception;
        }

        protected LoginTCException createException(AdminRestClientException e) {
            return new InternalLoginTCException(e);
        }

        protected LoginTCException createException(JSONException e) {
            return new InternalLoginTCException(e);
        }

        protected LoginTCException createException(ParseException e) {
            return new InternalLoginTCException(e);
        }
    }

    /**
     * The default LoginTC Admin.
     */
    private static final String DEFAULT_HOST = "cloud.logintc.com";

    /**
     * The LoginTC Admin HTTP client.
     */
    private AdminRestClient adminRestClient = null;

    /**
     * Factory for creating LoginTCException exceptions.
     */
    private LoginTCExceptionFactory exceptionFactory = null;

    /**
     * Serialize a string into JSON.
     * 
     * @param json JSON string.
     * @return The parsed JSON object.
     * @throws JSONException If the input is not valid JSON
     */
    private JSONObject getJson(String json) throws JSONException {
        return (JSONObject) new JSONTokener(json).nextValue();
    }

    /**
     * Serialize a string into JSON.
     * 
     * @param json JSON string.
     * @return The parsed JSON array.
     * @throws JSONException If the input is not valid JSON
     */
    private JSONArray getJsonArray(String json) throws JSONException {
        return (JSONArray) new JSONTokener(json).nextValue();
    }

    /**
     * @param apiKey The LoginTC organization API Key
     */
    public LoginTC(String apiKey) {
        this(apiKey, DEFAULT_HOST, true);
    }

    /**
     * @param apiKey The LoginTC organization API Key
     * @param host The host and optional port
     */
    public LoginTC(String apiKey, String host) {
        this(apiKey, host, true);
    }

    /**
     * @param apiKey The LoginTC organization API Key
     * @param host The host and optional port (e.g. "10.0.10.20:3333")
     * @param secure Specify false to use HTTP instead of HTTPS. Default true.
     */
    public LoginTC(String apiKey, String host, Boolean secure) {
        this(apiKey, host, secure, null);
    }

    /**
     * @param apiKey The LoginTC organization API Key
     * @param host The host and optional port (e.g. "10.0.10.20:3333")
     * @param secure Specify false to use HTTP instead of HTTPS. Default true.
     * @param adminRestClient The LoginTC Admin REST client.
     */
    public LoginTC(String apiKey, String host, Boolean secure, AdminRestClient adminRestClient) {
        this.exceptionFactory = new LoginTCExceptionFactory();

        if (adminRestClient == null) {
            Integer port = null;
            String scheme = secure ? "https" : "http";

            String[] hostParts = host.split(":");

            if (hostParts.length > 1) {
                host = hostParts[0];
                port = Integer.parseInt(hostParts[1]);
            } else {
                if (secure) {
                    port = 443;
                } else {
                    port = 80;
                }
            }

            adminRestClient = new AdminRestClient(scheme, host, port, apiKey, String.format("%s/%s", NAME, VERSION));
        }

        this.adminRestClient = adminRestClient;
    }

    /**
     * @param proxyHost The proxy host.
     * @param proxyPort The proxy port.
     */
    public void setProxy(String proxyHost, int proxyPort) {
        adminRestClient.setProxy(proxyHost, proxyPort);
    }

    /**
     * @param proxyHost The proxy host.
     * @param proxyPort The proxy post.
     * @param proxyUser The proxy username.
     * @param proxyPassword The proxy password for the user.
     */
    public void setProxy(String proxyHost, int proxyPort, String proxyUser, String proxyPassword) {
        adminRestClient.setProxy(proxyHost, proxyPort, proxyUser, proxyPassword);
    }

    /**
     * Get user info.
     * 
     * @param userId The user's identifier.
     * @return The requested user.
     * @throws LoginTCException if the call fails.
     */
    public User getUser(String userId) throws LoginTCException {
        User user = null;

        try {
            JSONObject jsonObject = getJson(adminRestClient.get(String.format("/api/users/%s", userId)));
            JSONArray jsonDomains = jsonObject.getJSONArray("domains");

            List<String> domains = new ArrayList<String>();

            for (int i = 0; i < jsonDomains.length(); i++) {
                domains.add(jsonDomains.getString(i));
            }

            List<String> bypassCodes = new ArrayList<String>();

            if (jsonObject.has("bypasscodes")) {
                JSONArray jsonBypassCodes = jsonObject.getJSONArray("bypasscodes");

                for (int i = 0; i < jsonBypassCodes.length(); i++) {
                    bypassCodes.add(jsonBypassCodes.getString(i));
                }
            }

            String id = jsonObject.getString("id");
            String username = jsonObject.getString("username");
            String email = jsonObject.getString("email");
            String name = jsonObject.getString("name");
            String hardware = jsonObject.getString("hardware");

            user = new User(id, username, email, name, domains, bypassCodes, hardware);
        } catch (JSONException e) {
            throw exceptionFactory.createException(e);
        } catch (RestAdminRestClientException e) {
            throw exceptionFactory.createException(e);
        } catch (AdminRestClientException e) {
            throw exceptionFactory.createException(e);
        }

        return user;
    }

    /**
     * Get user info.
     * 
     * @param username The user's username.
     * @return The requested user.
     * @throws LoginTCException if the call fails.
     */
    public User getUserByUsername(String username) throws LoginTCException {
        User user = null;

        try {
            JSONObject jsonObject = getJson(adminRestClient.get(String.format("/api/users"), String.format("username=%s", username)));
            JSONArray jsonDomains = jsonObject.getJSONArray("domains");

            List<String> domains = new ArrayList<String>();

            for (int i = 0; i < jsonDomains.length(); i++) {
                domains.add(jsonDomains.getString(i));
            }

            List<String> bypassCodes = new ArrayList<String>();

            if (jsonObject.has("bypasscodes")) {
                JSONArray jsonBypassCodes = jsonObject.getJSONArray("bypasscodes");

                for (int i = 0; i < jsonBypassCodes.length(); i++) {
                    bypassCodes.add(jsonBypassCodes.getString(i));
                }
            }

            String id = jsonObject.getString("id");
            String userUsername = jsonObject.getString("username");
            String email = jsonObject.getString("email");
            String name = jsonObject.getString("name");
            String hardware = jsonObject.getString("hardware");

            user = new User(id, userUsername, email, name, domains, bypassCodes, hardware);
        } catch (JSONException e) {
            throw exceptionFactory.createException(e);
        } catch (RestAdminRestClientException e) {
            throw exceptionFactory.createException(e);
        } catch (AdminRestClientException e) {
            throw exceptionFactory.createException(e);
        }

        return user;
    }

    /**
     * Get list of users from organization
     * 
     * @return List of users
     * @throws LoginTCException if the call fails.
     */
    public List<User> getUsers() throws LoginTCException {
        return getUsers(1);
    }

    /**
     * Get list of users from organization
     * 
     * @param page Page number to retrieve
     * @return List of users
     * @throws LoginTCException if the call fails.
     */
    public List<User> getUsers(Integer page) throws LoginTCException {
        List<User> users = new ArrayList<User>();
        try {
            JSONArray jsonArray = getJsonArray(adminRestClient.get(String.format("/api/users"), String.format("page=%d", page)));

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject userObject = jsonArray.getJSONObject(i);
                JSONArray jsonDomains = userObject.getJSONArray("domains");
                List<String> domains = new ArrayList<String>();
                for (int j = 0; j < jsonDomains.length(); j++) {
                    domains.add(jsonDomains.getString(j));
                }
                List<String> bypassCodes = new ArrayList<String>();

                if (userObject.has("bypasscodes")) {
                    JSONArray jsonBypassCodes = userObject.getJSONArray("bypasscodes");

                    for (int k = 0; k < jsonBypassCodes.length(); k++) {
                        bypassCodes.add(jsonBypassCodes.getString(k));
                    }
                }
                String id = userObject.getString("id");
                String username = userObject.getString("username");
                String email = userObject.getString("email");
                String name = userObject.getString("name");
                String hardware = userObject.getString("hardware");

                users.add(new User(id, username, email, name, domains, bypassCodes, hardware));
            }
        } catch (JSONException e) {
            throw exceptionFactory.createException(e);
        } catch (RestAdminRestClientException e) {
            throw exceptionFactory.createException(e);
        } catch (AdminRestClientException e) {
            throw exceptionFactory.createException(e);
        }

        return users;
    }

    /**
     * Create a new user.
     * 
     * @param username The new user's username.
     * @param email The new user's email address.
     * @param name The new user's real name.
     * @return The newly created user.
     * @throws LoginTCException if the call fails.
     */
    public User createUser(String username, String email, String name) throws LoginTCException {
        User user = null;

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("username", username);
            jsonObject.put("email", email);
            jsonObject.put("name", name);

            jsonObject = getJson(adminRestClient.post("/api/users", jsonObject.toString()));
            JSONArray jsonDomains = jsonObject.getJSONArray("domains");

            List<String> domains = new ArrayList<String>();

            for (int i = 0; i < jsonDomains.length(); i++) {
                domains.add(jsonDomains.getString(i));
            }

            List<String> bypassCodes = new ArrayList<String>();

            if (jsonObject.has("bypasscodes")) {
                JSONArray jsonBypassCodes = jsonObject.getJSONArray("bypasscodes");

                for (int i = 0; i < jsonBypassCodes.length(); i++) {
                    bypassCodes.add(jsonBypassCodes.getString(i));
                }
            }

            String id = jsonObject.getString("id");
            username = jsonObject.getString("username");
            email = jsonObject.getString("email");
            name = jsonObject.getString("name");
            String hardware = jsonObject.getString("hardware");

            user = new User(id, username, email, name, domains, bypassCodes, hardware);
        } catch (JSONException e) {
            throw exceptionFactory.createException(e);
        } catch (RestAdminRestClientException e) {
            throw exceptionFactory.createException(e);
        } catch (AdminRestClientException e) {
            throw exceptionFactory.createException(e);
        }

        return user;
    }

    /**
     * Update a user.
     * 
     * @param userId The target user's identifier.
     * @param email The user's new email address. Use null if no change.
     * @param name The user's new name. Use null if no change.
     * @return The updated user.
     * @throws LoginTCException if the call fails.
     */
    public User updateUser(String userId, String email, String name) throws LoginTCException {
        User user = null;

        try {
            JSONObject jsonObject = new JSONObject();

            if (email != null) {
                jsonObject.put("email", email);
            }

            if (name != null) {
                jsonObject.put("name", name);
            }

            jsonObject = getJson(adminRestClient.put(String.format("/api/users/%s", userId), jsonObject.toString()));
            JSONArray jsonDomains = jsonObject.getJSONArray("domains");

            List<String> domains = new ArrayList<String>();

            for (int i = 0; i < jsonDomains.length(); i++) {
                domains.add(jsonDomains.getString(i));
            }

            List<String> bypassCodes = new ArrayList<String>();

            if (jsonObject.has("bypasscodes")) {
                JSONArray jsonBypassCodes = jsonObject.getJSONArray("bypasscodes");

                for (int i = 0; i < jsonBypassCodes.length(); i++) {
                    bypassCodes.add(jsonBypassCodes.getString(i));
                }
            }

            String id = jsonObject.getString("id");
            String username = jsonObject.getString("username");
            email = jsonObject.getString("email");
            name = jsonObject.getString("name");
            String hardware = jsonObject.getString("hardware");

            user = new User(id, username, email, name, domains, bypassCodes, hardware);
        } catch (JSONException e) {
            throw exceptionFactory.createException(e);
        } catch (RestAdminRestClientException e) {
            throw exceptionFactory.createException(e);
        } catch (AdminRestClientException e) {
            throw exceptionFactory.createException(e);
        }

        return user;
    }

    /**
     * Delete a user.
     * 
     * @param userId The target user's identifier.
     * @throws LoginTCException if the call fails.
     */
    public void deleteUser(String userId) throws LoginTCException {
        try {
            adminRestClient.delete(String.format("/api/users/%s", userId));
        } catch (RestAdminRestClientException e) {
            throw exceptionFactory.createException(e);
        } catch (AdminRestClientException e) {
            throw exceptionFactory.createException(e);
        }
    }

    /**
     * Add a user to a domain.
     * 
     * @param domainId The target domain identifier.
     * @param userId The target user identifier.
     * @throws LoginTCException if the call fails.
     */
    public void addDomainUser(String domainId, String userId) throws LoginTCException {
        try {
            adminRestClient.put(String.format("/api/domains/%s/users/%s", domainId, userId), null);
        } catch (RestAdminRestClientException e) {
            throw exceptionFactory.createException(e);
        } catch (AdminRestClientException e) {
            throw exceptionFactory.createException(e);
        }
    }

    /**
     * Set a domain's users. If the provided users do not yet exist then they will be created in the Organization. Existing organization
     * users will be added to the domain. The existing domain users that are not present in the users parameter will be removed from the
     * domain and their tokens will be revoked.
     * 
     * @param domainId The target domain identifier.
     * @param users A list of users that should belong to the domain.
     * @throws LoginTCException if the call fails.
     */
    public void setDomainUsers(String domainId, List<User> users) throws LoginTCException {
        try {
            JSONArray jsonArray = new JSONArray();

            for (User user : users) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("username", user.getUsername());
                jsonObject.put("email", user.getEmail());
                jsonObject.put("name", user.getName());
                jsonArray.put(jsonObject);
            }

            adminRestClient.put(String.format("/api/domains/%s/users", domainId), jsonArray.toString());
        } catch (JSONException e) {
            throw exceptionFactory.createException(e);
        } catch (RestAdminRestClientException e) {
            throw exceptionFactory.createException(e);
        } catch (AdminRestClientException e) {
            throw exceptionFactory.createException(e);
        }
    }

    /**
     * Remove a user from a domain.
     * 
     * @param domainId The target domain identifier.
     * @param userId The targert user identifier.
     * @throws LoginTCException if the call fails.
     */
    public void removeDomainUser(String domainId, String userId) throws LoginTCException {
        try {
            adminRestClient.delete(String.format("/api/domains/%s/users/%s", domainId, userId));
        } catch (RestAdminRestClientException e) {
            throw exceptionFactory.createException(e);
        } catch (AdminRestClientException e) {
            throw exceptionFactory.createException(e);
        }
    }

    /**
     * Create a user token if one does not exist or if it has been revoked. Does nothing if the token is already active or not yet loaded.
     * 
     * @param domainId The target domain identifier.
     * @param userId The target user identifier.
     * @return The newly created token.
     * @throws LoginTCException if the call fails.
     */
    public Token createUserToken(String domainId, String userId) throws LoginTCException {
        Token token = null;

        try {
            JSONObject jsonObject = getJson(adminRestClient.put(String.format("/api/domains/%s/users/%s/token", domainId, userId), null));

            Token.State state = Token.State.valueOf((String) jsonObject.getString("state").toUpperCase(Locale.ENGLISH));
            String code = jsonObject.has("code") ? jsonObject.getString("code") : null;

            token = new Token(state, code);
        } catch (JSONException e) {
            throw exceptionFactory.createException(e);
        } catch (RestAdminRestClientException e) {
            throw exceptionFactory.createException(e);
        } catch (AdminRestClientException e) {
            throw exceptionFactory.createException(e);
        }

        return token;
    }

    /**
     * Gets a user's token information. Throws a LoginTCException if a token does not exist or has been revoked.
     * 
     * @param domainId The target domain identifier.
     * @param userId The target user identifier.
     * @return The requested session.
     * @throws LoginTCException if the call fails.
     */
    public Token getUserToken(String domainId, String userId) throws LoginTCException {
        Token token = null;

        try {
            JSONObject jsonObject = getJson(adminRestClient.get(String.format("/api/domains/%s/users/%s/token", domainId, userId)));

            Token.State state = Token.State.valueOf((String) jsonObject.getString("state").toUpperCase(Locale.ENGLISH));
            String code = jsonObject.has("code") ? jsonObject.getString("code") : null;

            token = new Token(state, code);
        } catch (JSONException e) {
            throw exceptionFactory.createException(e);
        } catch (RestAdminRestClientException e) {
            throw exceptionFactory.createException(e);
        } catch (AdminRestClientException e) {
            throw exceptionFactory.createException(e);
        }

        return token;
    }

    /**
     * Delete (i.e. revoke) a user's token.
     * 
     * @param domainId The target domain identifier.
     * @param userId The target user identifier.
     * @throws LoginTCException if the call fails.
     */
    public void deleteUserToken(String domainId, String userId) throws LoginTCException {
        try {
            adminRestClient.delete(String.format("/api/domains/%s/users/%s/token", domainId, userId));
        } catch (RestAdminRestClientException e) {
            throw exceptionFactory.createException(e);
        } catch (AdminRestClientException e) {
            throw exceptionFactory.createException(e);
        }
    }

    /**
     * Create a LoginTC request.
     * 
     * @param domainId The target domain identifier.
     * @param userId The target user identifier.
     * @param attributes Map of attributes to be included in the LoginTC request. Null is permitted for no attributes.
     * @return Newly created session.
     * @throws NoTokenLoginTCException if the user does not have a token.
     * @throws LoginTCException if the call fails.
     */
    public Session createSession(String domainId, String userId, Map<String, String> attributes) throws NoTokenLoginTCException,
            LoginTCException {
        return createSession(domainId, userId, attributes, null, null, null);
    }

    /**
     * Create a LoginTC request.
     * 
     * @param domainId The target domain identifier.
     * @param userId The target user identifier.
     * @param attributes Map of attributes to be included in the LoginTC request. Null is permitted for no attributes.
     * @param ipAddress The IP Address of the user originating the request (optional)
     * @param bypassCode A 9 digit code to bypass device authentication (optional)
     * @param otp A 6 or 8 digit code instead of device authentication (optional)
     * @return Newly created session.
     * @throws NoTokenLoginTCException if the user does not have a token.
     * @throws LoginTCException if the call fails.
     */
    public Session createSession(String domainId, String userId, Map<String, String> attributes, String ipAddress, String bypassCode,
            String otp)
            throws NoTokenLoginTCException,
            LoginTCException {
        Session session = null;

        try {
            JSONObject jsonObject = new JSONObject();
            JSONArray attributesArray = new JSONArray();

            if (attributes != null) {
                for (Map.Entry<String, String> entry : attributes.entrySet()) {
                    JSONObject attributeObject = new JSONObject();
                    attributeObject.put("key", entry.getKey());
                    attributeObject.put("value", entry.getValue());
                    attributesArray.put(attributeObject);
                }
            }

            JSONObject jsonUserObject = new JSONObject();
            jsonUserObject.put("id", userId);

            jsonObject.put("user", jsonUserObject);
            jsonObject.put("attributes", attributesArray);

            if (ipAddress != null && !ipAddress.isEmpty()) {
                jsonObject.put("ipAddress", ipAddress);
            }

            if (bypassCode != null) {
                jsonObject.put("bypasscode", bypassCode);
            } else if (otp != null) {
                jsonObject.put("otp", otp);
            }

            jsonObject = getJson(adminRestClient.post(String.format("/api/domains/%s/sessions", domainId), jsonObject.toString()));

            String id = jsonObject.getString("id");
            Session.State state = Session.State.valueOf((String) jsonObject.getString("state").toUpperCase(Locale.ENGLISH));

            session = new Session(id, state);
        } catch (JSONException e) {
            throw exceptionFactory.createException(e);
        } catch (RestAdminRestClientException e) {
            throw exceptionFactory.createException(e);
        } catch (AdminRestClientException e) {
            throw exceptionFactory.createException(e);
        }

        return session;
    }

    /**
     * Create a LoginTC request.
     * 
     * @param domainId The target domain identifier.
     * @param username The target user username.
     * @param attributes Map of attributes to be included in the LoginTC request. Null is permitted for no attributes.
     * @return Newly created session.
     * @throws NoTokenLoginTCException if the user does not have a token.
     * @throws LoginTCException if the call fails.
     */
    public Session createSessionWithUsername(String domainId, String username, Map<String, String> attributes)
            throws NoTokenLoginTCException,
            LoginTCException {
        return createSessionWithUsername(domainId, username, attributes, null, null, null);
    }

    /**
     * Create a LoginTC request.
     * 
     * @param domainId The target domain identifier.
     * @param username The target user username.
     * @param attributes Map of attributes to be included in the LoginTC request. Null is permitted for no attributes.
     * @param ipAddress The IP Address of the user originating the request (optional)
     * @param bypassCode A 9 digit code to bypass device authentication (optional)
     * @param otp A 6 or 8 digit code instead of device authentication (optional)
     * @return Newly created session.
     * @throws NoTokenLoginTCException if the user does not have a token.
     * @throws LoginTCException if the call fails.
     */
    public Session createSessionWithUsername(String domainId, String username, Map<String, String> attributes, String ipAddress,
            String bypassCode, String otp)
            throws NoTokenLoginTCException,
            LoginTCException {
        Session session = null;

        try {
            JSONObject jsonObject = new JSONObject();
            JSONArray attributesArray = new JSONArray();

            if (attributes != null) {
                for (Map.Entry<String, String> entry : attributes.entrySet()) {
                    JSONObject attributeObject = new JSONObject();
                    attributeObject.put("key", entry.getKey());
                    attributeObject.put("value", entry.getValue());
                    attributesArray.put(attributeObject);
                }
            }

            JSONObject jsonUserObject = new JSONObject();
            jsonUserObject.put("username", username);

            jsonObject.put("user", jsonUserObject);
            jsonObject.put("attributes", attributesArray);

            if (ipAddress != null && !ipAddress.isEmpty()) {
                jsonObject.put("ipAddress", ipAddress);
            }

            if (bypassCode != null && !bypassCode.isEmpty()) {
                jsonObject.put("bypasscode", bypassCode);
            } else if (otp != null && !otp.isEmpty()) {
                jsonObject.put("otp", otp);
            }

            jsonObject = getJson(adminRestClient.post(String.format("/api/domains/%s/sessions", domainId), jsonObject.toString()));

            String id = jsonObject.getString("id");
            Session.State state = Session.State.valueOf((String) jsonObject.getString("state").toUpperCase(Locale.ENGLISH));

            session = new Session(id, state);
        } catch (JSONException e) {
            throw exceptionFactory.createException(e);
        } catch (RestAdminRestClientException e) {
            throw exceptionFactory.createException(e);
        } catch (AdminRestClientException e) {
            throw exceptionFactory.createException(e);
        }

        return session;
    }

    /**
     * Get a session's information.
     * 
     * @param domainId The target domain identifier.
     * @param sessionId The target session identifier.
     * @return The requested session.
     * @throws NoTokenLoginTCException if the user does not have a token.
     * @throws LoginTCException if the call fails.
     */
    public Session getSession(String domainId, String sessionId) throws NoTokenLoginTCException, LoginTCException {
        Session session = null;

        try {
            JSONObject jsonObject = getJson(adminRestClient.get(String.format("/api/domains/%s/sessions/%s", domainId, sessionId)));

            Session.State state = Session.State.valueOf((String) jsonObject.getString("state").toUpperCase(Locale.ENGLISH));

            session = new Session(sessionId, state);
        } catch (JSONException e) {
            throw exceptionFactory.createException(e);
        } catch (RestAdminRestClientException e) {
            throw exceptionFactory.createException(e);
        } catch (AdminRestClientException e) {
            throw exceptionFactory.createException(e);
        }

        return session;
    }

    /**
     * Delete (i.e. cancel) a session.
     * 
     * @param domainId The target domain identifier.
     * @param sessionId The target session identifier.
     * @throws LoginTCException if the call fails.
     */
    public void deleteSession(String domainId, String sessionId) throws LoginTCException {
        try {
            adminRestClient.delete(String.format("/api/domains/%s/sessions/%s", domainId, sessionId));
        } catch (RestAdminRestClientException e) {
            throw exceptionFactory.createException(e);
        } catch (AdminRestClientException e) {
            throw exceptionFactory.createException(e);
        }
    }

    /**
     * Get Ping status.
     * 
     * @return Status (true if OK).
     * @throws LoginTCException if the call fails.
     */
    public boolean getPing() throws LoginTCException {
        boolean status = false;
        try {
            JSONObject jsonObject = getJson(adminRestClient.get("/api/ping"));
            if (jsonObject.getString("status").equals("OK")) {
                status = true;
            }

        } catch (JSONException e) {
            throw exceptionFactory.createException(e);
        } catch (RestAdminRestClientException e) {
            throw exceptionFactory.createException(e);
        } catch (AdminRestClientException e) {
            throw exceptionFactory.createException(e);
        }

        return status;
    }

    /**
     * @return The requested Organization
     * @throws LoginTCException if the call fails.
     */
    public Organization getOrganization() throws LoginTCException {
        Organization organization = null;
        try {
            JSONObject jsonObject = getJson(adminRestClient.get("/api/organization"));
            String organizationName = jsonObject.getString("name");
            organization = new Organization(organizationName);

        } catch (JSONException e) {
            throw exceptionFactory.createException(e);
        } catch (RestAdminRestClientException e) {
            throw exceptionFactory.createException(e);
        } catch (AdminRestClientException e) {
            throw exceptionFactory.createException(e);
        }

        return organization;
    }

    /**
     * Get domain info.
     * 
     * @param domainId The domain identifier.
     * @return The requested Domain
     * @throws LoginTCException if the call fails.
     */
    public Domain getDomain(String domainId) throws LoginTCException {
        Domain domain = null;
        try {
            JSONObject jsonObject = getJson(adminRestClient.get(String.format("/api/domains/%s", domainId)));
            String id = jsonObject.getString("id");
            String name = jsonObject.getString("name");
            String type = jsonObject.getString("type");
            String keyType = jsonObject.getString("keyType");
            Integer maxAllowedRetries = jsonObject.getInt("maxAllowedRetries");
            Integer requestTimeout = jsonObject.getInt("requestTimeout");
            Integer activationCodeExpiration = jsonObject.getInt("activationCodeExpiration");
            Boolean requestPollingEnabled = jsonObject.getBoolean("requestPollingEnabled");
            Boolean bypassEnabled = jsonObject.getBoolean("bypassEnabled");
            domain = new Domain(id, name, type, keyType, maxAllowedRetries, requestTimeout, activationCodeExpiration,
                    requestPollingEnabled, bypassEnabled);
        } catch (JSONException e) {
            throw exceptionFactory.createException(e);
        } catch (RestAdminRestClientException e) {
            throw exceptionFactory.createException(e);
        } catch (AdminRestClientException e) {
            throw exceptionFactory.createException(e);
        }

        return domain;
    }

    /**
     * Get domain image.
     * 
     * @param domainId The domain identifier.
     * @return The requested image as a byte array
     * @throws LoginTCException if the call fails.
     */
    public byte[] getDomainImage(String domainId) throws LoginTCException {
        byte[] image = null;
        try {
            image = adminRestClient.getBytes(String.format("/api/domains/%s/image", domainId), "image/png");
        } catch (RestAdminRestClientException e) {
            throw exceptionFactory.createException(e);
        } catch (AdminRestClientException e) {
            throw exceptionFactory.createException(e);
        }

        return image;
    }

    /**
     * Get user info.
     * 
     * @param domainId The domain identifier.
     * @param userId The user's identifier.
     * @return The requested user.
     * @throws LoginTCException if the call fails.
     */
    public User getDomainUser(String domainId, String userId) throws LoginTCException {
        User user = null;
        try {
            JSONObject jsonObject = getJson(adminRestClient.get(String.format("/api/domains/%s/users/%s", domainId, userId)));
            JSONArray jsonDomains = jsonObject.getJSONArray("domains");

            List<String> domains = new ArrayList<String>();

            for (int i = 0; i < jsonDomains.length(); i++) {
                domains.add(jsonDomains.getString(i));
            }

            List<String> bypassCodes = new ArrayList<String>();

            if (jsonObject.has("bypasscodes")) {
                JSONArray jsonBypassCodes = jsonObject.getJSONArray("bypasscodes");

                for (int i = 0; i < jsonBypassCodes.length(); i++) {
                    bypassCodes.add(jsonBypassCodes.getString(i));
                }
            }

            String id = jsonObject.getString("id");
            String username = jsonObject.getString("username");
            String email = jsonObject.getString("email");
            String name = jsonObject.getString("name");
            String hardware = jsonObject.getString("hardware");

            user = new User(id, username, email, name, domains, bypassCodes, hardware);
        } catch (JSONException e) {
            throw exceptionFactory.createException(e);
        } catch (RestAdminRestClientException e) {
            throw exceptionFactory.createException(e);
        } catch (AdminRestClientException e) {
            throw exceptionFactory.createException(e);
        }

        return user;
    }

    /**
     * Get list of users from domain
     * 
     * @param domainId The domain identifier
     * @return List of users in the domain
     * @throws LoginTCException if the call fails.
     */
    public List<User> getDomainUsers(String domainId) throws LoginTCException {
        return getDomainUsers(domainId, 1);
    }

    /**
     * Get list of users from domain
     * 
     * @param domainId The domain identifier
     * @param page Page number to retrieve
     * @return List of users in the domain
     * @throws LoginTCException if the call fails.
     */
    public List<User> getDomainUsers(String domainId, Integer page) throws LoginTCException {
        List<User> users = new ArrayList<User>();
        try {
            JSONArray jsonArray = getJsonArray(adminRestClient.get(String.format("/api/domains/%s/users", domainId),
                    String.format("page=%d", page)));

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject userObject = jsonArray.getJSONObject(i);
                JSONArray jsonDomains = userObject.getJSONArray("domains");
                List<String> domains = new ArrayList<String>();
                for (int j = 0; j < jsonDomains.length(); j++) {
                    domains.add(jsonDomains.getString(j));
                }
                List<String> bypassCodes = new ArrayList<String>();

                if (userObject.has("bypasscodes")) {
                    JSONArray jsonBypassCodes = userObject.getJSONArray("bypasscodes");

                    for (int k = 0; k < jsonBypassCodes.length(); k++) {
                        bypassCodes.add(jsonBypassCodes.getString(k));
                    }
                }
                String id = userObject.getString("id");
                String username = userObject.getString("username");
                String email = userObject.getString("email");
                String name = userObject.getString("name");
                String hardware = userObject.getString("hardware");

                users.add(new User(id, username, email, name, domains, bypassCodes, hardware));
            }
        } catch (JSONException e) {
            throw exceptionFactory.createException(e);
        } catch (RestAdminRestClientException e) {
            throw exceptionFactory.createException(e);
        } catch (AdminRestClientException e) {
            throw exceptionFactory.createException(e);
        }

        return users;
    }

    /**
     * Get bypass code info.
     * 
     * @param bypassCodeId The bypass code's identifier.
     * @return The requested bypass code.
     * @throws LoginTCException if the call fails.
     */
    public BypassCode getBypassCode(String bypassCodeId) throws LoginTCException {
        BypassCode bypassCode = null;

        try {
            JSONObject jsonObject = getJson(adminRestClient.get(String.format("/api/bypasscodes/%s", bypassCodeId)));

            String id = jsonObject.getString("id");
            String code = jsonObject.getString("code");
            Date dtExpiry = DATE_FORMAT_ISO8601.parse(jsonObject.getString("dtExpiry"));
            String user = jsonObject.getString("user");
            Integer usesAllowed = jsonObject.getInt("usesAllowed");
            Integer usesRemaining = jsonObject.getInt("usesRemaining");

            bypassCode = new BypassCode(id, code, dtExpiry, user, usesAllowed, usesRemaining);
        } catch (JSONException e) {
            throw exceptionFactory.createException(e);
        } catch (RestAdminRestClientException e) {
            throw exceptionFactory.createException(e);
        } catch (AdminRestClientException e) {
            throw exceptionFactory.createException(e);
        } catch (ParseException e) {
            throw exceptionFactory.createException(e);
        }

        return bypassCode;
    }

    /**
     * Get list of bypass codes from user
     * 
     * @param userId The user identifier
     * @return List of bypass codes for the user
     * @throws LoginTCException if the call fails.
     */
    public List<BypassCode> getBypassCodes(String userId) throws LoginTCException {
        List<BypassCode> bypassCodes = new ArrayList<BypassCode>();
        try {
            JSONArray jsonArray = getJsonArray(adminRestClient.get(String.format("/api/users/%s/bypasscodes", userId)));

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);

                String id = jsonObject.getString("id");
                String code = jsonObject.getString("code");
                Date dtExpiry = DATE_FORMAT_ISO8601.parse(jsonObject.getString("dtExpiry"));
                String user = jsonObject.getString("user");
                Integer usesAllowed = jsonObject.getInt("usesAllowed");
                Integer usesRemaining = jsonObject.getInt("usesRemaining");

                bypassCodes.add(new BypassCode(id, code, dtExpiry, user, usesAllowed, usesRemaining));
            }
        } catch (JSONException e) {
            throw exceptionFactory.createException(e);
        } catch (RestAdminRestClientException e) {
            throw exceptionFactory.createException(e);
        } catch (AdminRestClientException e) {
            throw exceptionFactory.createException(e);
        } catch (ParseException e) {
            throw exceptionFactory.createException(e);
        }

        return bypassCodes;
    }

    /**
     * Create a new bypass code.
     * 
     * @param userId The user's identifier.
     * @return The newly created bypass code.
     * @throws LoginTCException if the call fails.
     */
    public BypassCode createBypassCode(String userId) throws LoginTCException {
        return createBypassCode(userId, null, null);
    }

    /**
     * Create a new bypass code.
     * 
     * @param userId The user's identifier.
     * @param usesAllowed The number of times the bypass code can be used.
     * @param expirationTime The time in minutes the bypass code is valid (0 means never expires).
     * @return The newly created bypass code.
     * @throws LoginTCException if the call fails.
     */
    public BypassCode createBypassCode(String userId, Integer usesAllowed, Integer expirationTime) throws LoginTCException {
        BypassCode bypassCode = null;

        try {
            JSONObject jsonObject = new JSONObject();

            if (usesAllowed == null) {
                usesAllowed = 1;
            }

            if (expirationTime == null) {
                expirationTime = 0;
            }

            jsonObject.put("usesAllowed", usesAllowed);
            jsonObject.put("expirationTime", expirationTime);

            jsonObject = getJson(adminRestClient.post(String.format("/api/users/%s/bypasscodes", userId), jsonObject.toString()));

            String id = jsonObject.getString("id");
            String code = jsonObject.getString("code");
            Date dtExpiry = DATE_FORMAT_ISO8601.parse(jsonObject.getString("dtExpiry"));
            String user = jsonObject.getString("user");
            usesAllowed = jsonObject.getInt("usesAllowed");
            Integer usesRemaining = jsonObject.getInt("usesRemaining");

            bypassCode = new BypassCode(id, code, dtExpiry, user, usesAllowed, usesRemaining);
        } catch (JSONException e) {
            throw exceptionFactory.createException(e);
        } catch (RestAdminRestClientException e) {
            throw exceptionFactory.createException(e);
        } catch (AdminRestClientException e) {
            throw exceptionFactory.createException(e);
        } catch (ParseException e) {
            throw exceptionFactory.createException(e);
        }

        return bypassCode;
    }

    /**
     * Delete a bypass code.
     * 
     * @param bypassCodeId The target bypass code's identifier.
     * @throws LoginTCException if the call fails.
     */
    public void deleteBypassCode(String bypassCodeId) throws LoginTCException {
        try {
            adminRestClient.delete(String.format("/api/bypasscodes/%s", bypassCodeId));
        } catch (RestAdminRestClientException e) {
            throw exceptionFactory.createException(e);
        } catch (AdminRestClientException e) {
            throw exceptionFactory.createException(e);
        }
    }

    /**
     * Delete all of user's bypass codes.
     * 
     * @param userId The target user's identifier.
     * @throws LoginTCException if the call fails.
     */
    public void deleteBypassCodes(String userId) throws LoginTCException {
        try {
            adminRestClient.delete(String.format("/api/users/%s/bypasscodes", userId));
        } catch (RestAdminRestClientException e) {
            throw exceptionFactory.createException(e);
        } catch (AdminRestClientException e) {
            throw exceptionFactory.createException(e);
        }
    }

    /**
     * Get hardware token info.
     * 
     * @param hardwareTokenId The hardware token's identifier.
     * @return The requested hardware token
     * @throws LoginTCException if the call fails.
     */
    public HardwareToken getHardwareToken(String hardwareTokenId) throws LoginTCException {
        HardwareToken hardwareToken = null;

        try {
            JSONObject jsonObject = getJson(adminRestClient.get(String.format("/api/hardware/%s", hardwareTokenId)));

            String id = jsonObject.getString("id");
            String alias = jsonObject.getString("alias");
            String serialNumber = jsonObject.getString("serialNumber");
            String type = jsonObject.getString("type");
            String timeStep = jsonObject.getString("timeStep");
            String syncState = jsonObject.getString("syncState");
            String user = jsonObject.getString("user");

            hardwareToken = new HardwareToken(id, alias, serialNumber, type, timeStep, syncState, user);
        } catch (JSONException e) {
            throw exceptionFactory.createException(e);
        } catch (RestAdminRestClientException e) {
            throw exceptionFactory.createException(e);
        } catch (AdminRestClientException e) {
            throw exceptionFactory.createException(e);
        }

        return hardwareToken;
    }

    /**
     * Get user's hardware token info.
     * 
     * @param userId The user's identifier.
     * @return The requested hardware token
     * @throws LoginTCException if the call fails.
     */
    public HardwareToken getUserHardwareToken(String userId) throws LoginTCException {
        HardwareToken hardwareToken = null;

        try {
            JSONObject jsonObject = getJson(adminRestClient.get(String.format("/api/users/%s/hardware", userId)));

            String id = jsonObject.getString("id");
            String alias = jsonObject.getString("alias");
            String serialNumber = jsonObject.getString("serialNumber");
            String type = jsonObject.getString("type");
            String timeStep = jsonObject.getString("timeStep");
            String syncState = jsonObject.getString("syncState");
            String user = jsonObject.getString("user");

            hardwareToken = new HardwareToken(id, alias, serialNumber, type, timeStep, syncState, user);
        } catch (JSONException e) {
            throw exceptionFactory.createException(e);
        } catch (RestAdminRestClientException e) {
            throw exceptionFactory.createException(e);
        } catch (AdminRestClientException e) {
            throw exceptionFactory.createException(e);
        }

        return hardwareToken;
    }

    /**
     * Get all hardware token info of an organization.
     * 
     * @return The requested hardware token
     * @throws LoginTCException if the call fails.
     */
    public List<HardwareToken> getHardwareTokens() throws LoginTCException {
        return getHardwareTokens(1);
    }

    /**
     * Get all hardware token info of an organization.
     * 
     * @param page Page number to retrieve
     * @return The requested hardware token
     * @throws LoginTCException if the call fails.
     */
    public List<HardwareToken> getHardwareTokens(Integer page) throws LoginTCException {

        List<HardwareToken> hardwareTokens = new ArrayList<HardwareToken>();
        try {
            JSONArray jsonArray = getJsonArray(adminRestClient.get(String.format("/api/hardware"), String.format("page=%d", page)));

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject hardwareTokenObject = jsonArray.getJSONObject(i);

                String id = hardwareTokenObject.getString("id");
                String alias = hardwareTokenObject.getString("alias");
                String serialNumber = hardwareTokenObject.getString("serialNumber");
                String type = hardwareTokenObject.getString("type");
                String timeStep = hardwareTokenObject.getString("timeStep");
                String syncState = hardwareTokenObject.getString("syncState");
                String user = hardwareTokenObject.getString("user");

                hardwareTokens.add(new HardwareToken(id, alias, serialNumber, type, timeStep, syncState, user));
            }
        } catch (JSONException e) {
            throw exceptionFactory.createException(e);
        } catch (RestAdminRestClientException e) {
            throw exceptionFactory.createException(e);
        } catch (AdminRestClientException e) {
            throw exceptionFactory.createException(e);
        }

        return hardwareTokens;
    }

    /**
     * Create a new hardware token.
     * 
     * @param alias A short-hand mutable name
     * @param serialNumber The serial number of the hardware token
     * @param type Can be either TOTP6 or TOTP8
     * @param timeStep The number of seconds for the time step
     * @param seed A hexadecimal representation of the TOTP secret
     * @return The newly created hardware token.
     * @throws LoginTCException if the call fails.
     */
    public HardwareToken createHardwareToken(String alias, String serialNumber, String type, String timeStep, String seed)
            throws LoginTCException {
        HardwareToken hardwareToken = null;

        try {
            JSONObject jsonObject = new JSONObject();

            if (alias != null) {
                jsonObject.put("alias", alias);
            }

            jsonObject.put("serialNumber", serialNumber);
            jsonObject.put("type", type);
            jsonObject.put("timeStep", timeStep);
            jsonObject.put("seed", seed);

            jsonObject = getJson(adminRestClient.post(String.format("/api/hardware"), jsonObject.toString()));

            String id = jsonObject.getString("id");
            alias = jsonObject.getString("alias");
            serialNumber = jsonObject.getString("serialNumber");
            type = jsonObject.getString("type");
            timeStep = jsonObject.getString("timeStep");
            String syncState = jsonObject.getString("syncState");
            String user = jsonObject.getString("user");

            hardwareToken = new HardwareToken(id, alias, serialNumber, type, timeStep, syncState, user);
        } catch (JSONException e) {
            throw exceptionFactory.createException(e);
        } catch (RestAdminRestClientException e) {
            throw exceptionFactory.createException(e);
        } catch (AdminRestClientException e) {
            throw exceptionFactory.createException(e);
        }

        return hardwareToken;
    }

    /**
     * Update a hardware token.
     * 
     * @param hardwareTokenId The hardware token identifier
     * @param alias A short-hand mutable name
     * @return The updated hardware token.
     * @throws LoginTCException if the call fails.
     */
    public HardwareToken updateHardwareToken(String hardwareTokenId, String alias) throws LoginTCException {
        HardwareToken hardwareToken = null;

        try {
            JSONObject jsonObject = new JSONObject();

            if (alias != null) {
                jsonObject.put("alias", alias);
            }

            jsonObject = getJson(adminRestClient.put(String.format("/api/hardware/%s", hardwareTokenId), jsonObject.toString()));

            String id = jsonObject.getString("id");
            alias = jsonObject.getString("alias");
            String serialNumber = jsonObject.getString("serialNumber");
            String type = jsonObject.getString("type");
            String timeStep = jsonObject.getString("timeStep");
            String syncState = jsonObject.getString("syncState");
            String user = jsonObject.getString("user");

            hardwareToken = new HardwareToken(id, alias, serialNumber, type, timeStep, syncState, user);
        } catch (JSONException e) {
            throw exceptionFactory.createException(e);
        } catch (RestAdminRestClientException e) {
            throw exceptionFactory.createException(e);
        } catch (AdminRestClientException e) {
            throw exceptionFactory.createException(e);
        }

        return hardwareToken;
    }

    /**
     * Delete a hardware token.
     * 
     * @param hardwareTokenId The hardware token's identifier.
     * @throws LoginTCException if the call fails.
     */
    public void deleteHardwareToken(String hardwareTokenId) throws LoginTCException {
        try {
            adminRestClient.delete(String.format("/api/hardware/%s", hardwareTokenId));
        } catch (RestAdminRestClientException e) {
            throw exceptionFactory.createException(e);
        } catch (AdminRestClientException e) {
            throw exceptionFactory.createException(e);
        }
    }

    /**
     * Associate a hardware token with a user.
     * 
     * @param userId The user's identifier.
     * @param hardwareTokenId The hardware token's identifier.
     * @throws LoginTCException if the call fails.
     */
    public void associateHardwareToken(String userId, String hardwareTokenId) throws LoginTCException {
        try {
            adminRestClient.put(String.format("/api/users/%s/hardware/%s", userId, hardwareTokenId), null);
        } catch (RestAdminRestClientException e) {
            throw exceptionFactory.createException(e);
        } catch (AdminRestClientException e) {
            throw exceptionFactory.createException(e);
        }

    }

    /**
     * Disassociate a hardware token with a user.
     * 
     * @param userId The user's identifier.
     * @throws LoginTCException if the call fails.
     */
    public void disassociateHardwareToken(String userId) throws LoginTCException {
        try {
            adminRestClient.delete(String.format("/api/users/%s/hardware", userId));
        } catch (RestAdminRestClientException e) {
            throw exceptionFactory.createException(e);
        } catch (AdminRestClientException e) {
            throw exceptionFactory.createException(e);
        }
    }
}
