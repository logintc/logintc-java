
package com.cyphercor.logintc;

import com.cyphercor.logintc.AdminRestClient.AdminRestClientException;
import com.cyphercor.logintc.AdminRestClient.RestAdminRestClientException;
import com.cyphercor.logintc.resource.Domain;
import com.cyphercor.logintc.resource.Organization;
import com.cyphercor.logintc.resource.Session;
import com.cyphercor.logintc.resource.Token;
import com.cyphercor.logintc.resource.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * LoginTC Admin client to manage LoginTC users, domains, tokens and sessions.
 */
public class LoginTC {

    private static final String NAME = "LoginTC-Java";
    private static final String VERSION = "1.0.2";

    /**
     * A generic LoginTC client exception.
     */
    public class LoginTCException extends Exception {
        private static final long serialVersionUID = 3112741136967467568L;

        public LoginTCException(String message) {
            super(message);
        }

        public LoginTCException(Throwable throwable) {
            super(throwable);
        }

        public LoginTCException() {
            super();
        }
    }

    /**
     * Exception caused by internal client exception.
     */
    public class InternalLoginTCException extends LoginTCException {
        private static final long serialVersionUID = -7310070273601778377L;

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

        public ApiLoginTCException(String errorCode, String errorMessage) {
            super(String.format("%s: %s", errorCode, errorMessage));

            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }

        public String getErrorCode() {
            return this.errorCode;
        }

        public String getErrorMessage() {
            return this.errorMessage;
        }
    }

    /**
     * Exception for failure because of no valid token for the specified user
     * and domain. This means the token doesn't exist, it's not yet loaded, or
     * it has been revoked.
     */
    public class NoTokenLoginTCException extends ApiLoginTCException {
        private static final long serialVersionUID = -5878018652693276417L;

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
     * @throws JSONException
     */
    private JSONObject getJson(String json) throws JSONException {
        return (JSONObject) new JSONTokener(json).nextValue();
    }

    /**
     * Serialize a string into JSON.
     * 
     * @param json JSON string.
     * @return The parsed JSON array.
     * @throws JSONException
     */
    private JSONArray getJsonArray(String json) throws JSONException {
        return (JSONArray) new JSONTokener(json).nextValue();
    }

    /**
     * @param apiKey
     */
    public LoginTC(String apiKey) {
        this(apiKey, DEFAULT_HOST, true);
    }

    /**
     * @param apiKey
     * @param host The host and optional port
     */
    public LoginTC(String apiKey, String host) {
        this(apiKey, host, true);
    }

    /**
     * @param apiKey
     * @param host The host and optional port (e.g. "10.0.10.20:3333")
     * @param secure Specify false to use HTTP instead of HTTPS. Default true.
     */
    public LoginTC(String apiKey, String host, Boolean secure) {
        this(apiKey, host, secure, null);
    }

    /**
     * @param apiKey
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
     * @param proxyHost
     * @param proxyPort
     */
    public void setProxy(String proxyHost, int proxyPort) {
        adminRestClient.setProxy(proxyHost, proxyPort);
    }

    /**
     * @param proxyHost
     * @param proxyPort
     * @param proxyUser
     * @param proxyPassword
     */
    public void setProxy(String proxyHost, int proxyPort, String proxyUser, String proxyPassword) {
        adminRestClient.setProxy(proxyHost, proxyPort, proxyUser, proxyPassword);
    }

    /**
     * Get user info.
     * 
     * @param userId The user's identifier.
     * @return The requested user.
     * @throws LoginTCException
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

            String id = jsonObject.getString("id");
            String username = jsonObject.getString("username");
            String email = jsonObject.getString("email");
            String name = jsonObject.getString("name");

            user = new User(id, username, email, name, domains);
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
     * Create a new user.
     * 
     * @param username The new user's username.
     * @param email The new user's email address.
     * @param name The new user's real name.
     * @return The newly created user.
     * @throws LoginTCException
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

            String id = jsonObject.getString("id");
            username = jsonObject.getString("username");
            email = jsonObject.getString("email");
            name = jsonObject.getString("name");

            user = new User(id, username, email, name, domains);
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
     * @throws LoginTCException
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

            String id = jsonObject.getString("id");
            String username = jsonObject.getString("username");
            email = jsonObject.getString("email");
            name = jsonObject.getString("name");

            user = new User(id, username, email, name, domains);
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
     * @throws LoginTCException
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
     * @throws LoginTCException
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
     * Set a domain's users. If the provided users do not yet exist then they
     * will be created in the Organization. Existing organization users will be
     * added to the domain. The existing domain users that are not present in
     * the users parameter will be removed from the domain and their tokens will
     * be revoked.
     * 
     * @param users A list of users that should belong to the domain.
     * @throws LoginTCException
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
     * @throws LoginTCException
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
     * Create a user token if one does not exist or if it has been revoked. Does
     * nothing if the token is already active or not yet loaded.
     * 
     * @param domainId The target domain identifier.
     * @param userId The target user identifier.
     * @return The newly created token.
     * @throws LoginTCException
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
     * Gets a user's token information. Throws a LoginTCException if a token
     * does not exist or has been revoked.
     * 
     * @param domainId The target domain identifier.
     * @param userId The target user identifier.
     * @return The requested session.
     * @throws LoginTCException
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
     * @throws LoginTCException
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
     * @param attributes Map of attributes to be included in the LoginTC
     *            request. Null is permitted for no attributes.
     * @return Newly created session.
     * @throws NoTokenLoginTCException
     * @throws LoginTCException
     */
    public Session createSession(String domainId, String userId, Map<String, String> attributes) throws NoTokenLoginTCException,
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
     * @param attributes Map of attributes to be included in the LoginTC
     *            request. Null is permitted for no attributes.
     * @return Newly created session.
     * @throws NoTokenLoginTCException
     * @throws LoginTCException
     */
    public Session createSessionWithUsername(String domainId, String username, Map<String, String> attributes)
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
     * @throws LoginTCException
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
     * @throws LoginTCException
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
     * @throws LoginTCException
     */
    public boolean getPing() throws LoginTCException {
        boolean status = false;
        try {
            JSONObject jsonObject = getJson(adminRestClient.get("/api/ping"));
            if (jsonObject.getString("status").equals("OK"))
            {
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
     * @throws LoginTCException
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
     * @throws LoginTCException
     */
    public Domain getDomain(String domainId) throws LoginTCException {
        Domain domain = null;
        try {
            JSONObject jsonObject = getJson(adminRestClient.get(String.format("/api/domains/%s", domainId)));
            String id = jsonObject.getString("id");
            String name = jsonObject.getString("name");
            String type = jsonObject.getString("type");
            String keyType = jsonObject.getString("keyType");
            domain = new Domain(id, name, type, keyType);

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
     * @throws LoginTCException
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
     * @throws LoginTCException
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

            String id = jsonObject.getString("id");
            String username = jsonObject.getString("username");
            String email = jsonObject.getString("email");
            String name = jsonObject.getString("name");

            user = new User(id, username, email, name, domains);
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
     * @throws LoginTCException
     */
    public List<User> getDomainUsers(String domainId) throws LoginTCException {
        List<User> users = new ArrayList<User>();
        try {
            JSONArray jsonArray = getJsonArray(adminRestClient.get(String.format("/api/domains/%s/users", domainId)));

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject userObject = jsonArray.getJSONObject(i);
                JSONArray jsonDomains = userObject.getJSONArray("domains");
                List<String> domains = new ArrayList<String>();
                for (int j = 0; j < jsonDomains.length(); j++) {
                    domains.add(jsonDomains.getString(j));
                }
                String id = userObject.getString("id");
                String username = userObject.getString("username");
                String email = userObject.getString("email");
                String name = userObject.getString("name");
                users.add(new User(id, username, email, name, domains));
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

}
