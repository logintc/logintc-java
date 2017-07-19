
package com.cyphercor.logintc;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;

import com.cyphercor.logintc.AdminRestClient.AdminRestClientException;
import com.cyphercor.logintc.AdminRestClient.RestAdminRestClientException;
import com.cyphercor.logintc.LoginTC.LoginTCException;
import com.cyphercor.logintc.LoginTC.NoTokenLoginTCException;
import com.cyphercor.logintc.resource.Domain;
import com.cyphercor.logintc.resource.Organization;
import com.cyphercor.logintc.resource.Session;
import com.cyphercor.logintc.resource.Token;
import com.cyphercor.logintc.resource.User;

/**
 * Tests for LoginTC client.
 */
public class LoginTCTest {
    private LoginTC client = null;
    private AdminRestClient mockedAdminRestClient = null;

    private final String domainId = "9120580e94f134cb7c9f27cd1e43dbc82980e152";
    private final String userId = "12dea96fec20593566ab75692c9949596833adc9";
    private final String sessionId = "fcbdc4c271c889825d8338d2d8f10b6e5e95c171";

    private final String userUsername = "testuser1";
    private final String userEmail = "testuser1@cyphercor.com";
    private final String userName = "Test User";

    private final String domainName = "Cisco ASA";
    private final String domainType = "RADIUS";
    private final String domainKeyType = "PIN";
    private final Integer domainMaxAllowedRetries = 5;
    private final Integer domainRequestTimeout = 120;
    private final Integer domainActivationCodeExpiration = 365;
    private final Boolean domainRequestPollingEnabled = true;
    private final Boolean domainBypassEnabled = true;

    private final String organizationName = "Chrome Stage";

    private final String tokenCode = "89hto1p45";

    private String createJson(String original, Object... args) {
        return String.format(original.replace("'", "\""), args);
    }

    @Before
    public void initialize() {
        this.mockedAdminRestClient = mock(AdminRestClient.class);
        this.client = new LoginTC(null, null, true, mockedAdminRestClient);
    }

    @Test
    public void testAddDomainUser() throws AdminRestClientException, LoginTCException {
        String path = String.format("/api/domains/%s/users/%s", domainId, userId);
        when(mockedAdminRestClient.put(path, null)).thenReturn(null);

        client.addDomainUser(domainId, userId);

        verify(mockedAdminRestClient).put(path, null);
    }

    @Test
    public void testCreateSession() throws AdminRestClientException, LoginTCException {
        String path = String.format("/api/domains/%s/sessions", domainId);
        String body = createJson("{'attributes':[{'value':'Quinoa','key':'Product'},{'value':'42','key':'Price'}],'user':{'id':'%s'}}",
                userId);
        String response = createJson("{'id':'%s','state':'pending'}", sessionId);

        when(mockedAdminRestClient.post(eq(path), JSONObjectStringMatcher.eq(body))).thenReturn(response);

        Map<String, String> attributes = new LinkedHashMap<String, String>();
        attributes.put("Product", "Quinoa");
        attributes.put("Price", "42");

        Session session = client.createSession(domainId, userId, attributes);
        assertEquals(sessionId, session.getId());
        assertEquals(Session.State.PENDING, session.getState());

        verify(mockedAdminRestClient).post(eq(path), JSONObjectStringMatcher.eq(body));
    }

    @Test(expected = NoTokenLoginTCException.class)
    public void testCreateSessionNoTokenLoginTCException() throws AdminRestClientException, LoginTCException {
        String path = String.format("/api/domains/%s/sessions", domainId, userId);
        String body = createJson("{'attributes':[{'value':'Quinoa','key':'Product'},{'value':'42','key':'Price'}],'user':{'id':'%s'}}",
                userId);

        when(mockedAdminRestClient.post(eq(path), JSONObjectStringMatcher.eq(body))).thenThrow(
                new RestAdminRestClientException(404, createJson("{'errors':[{'code':'api.error.notfound.token','message':''}]}")));

        Map<String, String> attributes = new LinkedHashMap<String, String>();
        attributes.put("Product", "Quinoa");
        attributes.put("Price", "42");

        client.createSession(domainId, userId, attributes);
    }

    @Test
    public void testCreateUser() throws AdminRestClientException, LoginTCException, JSONException {
        String path = String.format("/api/users", domainId, userId);
        String body = createJson("{'username':'%s','email':'%s','name':'%s'}", userUsername, userEmail, userName);
        String response = createJson("{'id':'%s','username':'%s','email':'%s','name':'%s','domains':[],'hardware':''}", userId, userUsername, userEmail,
                userName);
        when(mockedAdminRestClient.post(eq(path), JSONObjectStringMatcher.eq(body))).thenReturn(response);

        User user = client.createUser(userUsername, userEmail, userName);
        assertEquals(userId, user.getId());
        assertEquals(userUsername, user.getUsername());
        assertEquals(userEmail, user.getEmail());
        assertEquals(userName, user.getName());

        verify(mockedAdminRestClient).post(eq(path), JSONObjectStringMatcher.eq(body));
    }

    @Test
    public void testCreateUserToken() throws AdminRestClientException, LoginTCException {
        String path = String.format("/api/domains/%s/users/%s/token", domainId, userId);
        String response = createJson("{'state':'pending','code':'%s'}", tokenCode);

        when(mockedAdminRestClient.put(path, null)).thenReturn(response);

        Token token = client.createUserToken(domainId, userId);
        assertEquals(Token.State.PENDING, token.getState());
        assertEquals(tokenCode.toUpperCase(Locale.ENGLISH), token.getCode());

        verify(mockedAdminRestClient).put(path, null);
    }

    @Test
    public void testDeleteSession() throws AdminRestClientException, LoginTCException {
        String path = String.format("/api/domains/%s/sessions/%s", domainId, sessionId);

        when(mockedAdminRestClient.delete(path)).thenReturn(null);

        client.deleteSession(domainId, sessionId);

        verify(mockedAdminRestClient).delete(path);
    }

    @Test
    public void testDeleteUser() throws AdminRestClientException, LoginTCException {
        String path = String.format("/api/users/%s", userId);

        when(mockedAdminRestClient.delete(path)).thenReturn(null);

        client.deleteUser(userId);

        verify(mockedAdminRestClient).delete(path);
    }

    @Test
    public void testDeleteUserToken() throws AdminRestClientException, LoginTCException {
        String path = String.format("/api/domains/%s/users/%s/token", domainId, userId);

        when(mockedAdminRestClient.delete(path)).thenReturn(null);

        client.deleteUserToken(domainId, userId);

        verify(mockedAdminRestClient).delete(path);
    }

    @Test
    public void testGetSession() throws AdminRestClientException, LoginTCException {
        String path = String.format("/api/domains/%s/sessions/%s", domainId, sessionId);
        String response = createJson("{'state':'pending'}", tokenCode);

        when(mockedAdminRestClient.get(path)).thenReturn(response);

        Session session = client.getSession(domainId, sessionId);
        assertEquals(sessionId, session.getId());
        assertEquals(Session.State.PENDING, session.getState());

        verify(mockedAdminRestClient).get(path);
    }

    @Test
    public void testGetUser() throws AdminRestClientException, LoginTCException {
        String path = String.format("/api/users/%s", userId);
        String response = createJson("{'id':'%s','username':'%s','email':'%s','name':'%s','domains':['%s'],'hardware':''}", userId, userUsername,
                userEmail, userName, domainId);

        when(mockedAdminRestClient.get(path)).thenReturn(response);

        User user = client.getUser(userId);
        assertEquals(userUsername, user.getUsername());
        assertEquals(userEmail, user.getEmail());
        assertEquals(userName, user.getName());
        assertEquals(1, user.getDomains().size());
        assertEquals(domainId, user.getDomains().get(0));

        verify(mockedAdminRestClient).get(path);
    }

    @Test
    public void testGetUserToken() throws AdminRestClientException, LoginTCException {
        String path = String.format("/api/domains/%s/users/%s/token", domainId, userId);
        String response = createJson("{'state':'active'}", tokenCode);

        when(mockedAdminRestClient.get(path)).thenReturn(response);

        Token token = client.getUserToken(domainId, userId);
        assertEquals(Token.State.ACTIVE, token.getState());
        assertEquals(null, token.getCode());

        verify(mockedAdminRestClient).get(path);
    }

    @Test
    public void testSetDomainUsers() throws AdminRestClientException, LoginTCException {
        String path = String.format("/api/domains/%s/users", domainId);
        String body = createJson("[{'username':'user1','email':'user1@cyphercor.com','name':'user one'},{'username':'user2','email':'user2@cyphercor.com','name':'user two'}]");

        when(mockedAdminRestClient.put(eq(path), JSONObjectStringMatcher.eq(body))).thenReturn(null);

        List<User> users = new ArrayList<User>();
        users.add(new User("user1", "user1@cyphercor.com", "user one"));
        users.add(new User("user2", "user2@cyphercor.com", "user two"));

        client.setDomainUsers(domainId, users);

        verify(mockedAdminRestClient).put(eq(path), JSONObjectStringMatcher.eq(body));
    }

    @Test
    public void testRemoveDomainUser() throws AdminRestClientException, LoginTCException {
        String path = String.format("/api/domains/%s/users/%s", domainId, userId);

        when(mockedAdminRestClient.delete(path)).thenReturn(null);

        client.removeDomainUser(domainId, userId);

        verify(mockedAdminRestClient).delete(path);
    }

    @Test
    public void testUpdateUser() throws AdminRestClientException, LoginTCException {
        String path = String.format("/api/users/%s", userId);
        String body = createJson("{'email':'new@cyphercor.com','name':'New Name'}");
        String response = createJson("{'id':'%s','username':'%s','email':'%s','name':'%s','domains':[],'hardware':''}", userId, userUsername,
                "new@cyphercor.com", "New Name");

        when(mockedAdminRestClient.put(eq(path), JSONObjectStringMatcher.eq(body))).thenReturn(response);

        User user = client.updateUser(userId, "new@cyphercor.com", "New Name");
        assertEquals("new@cyphercor.com", user.getEmail());
        assertEquals("New Name", user.getName());

        verify(mockedAdminRestClient).put(eq(path), JSONObjectStringMatcher.eq(body));
    }

    @Test
    public void testGetPing() throws AdminRestClientException, LoginTCException {
        String path = "/api/ping";
        String response = createJson("{\"status\": \"OK\"}");

        when(mockedAdminRestClient.get(path)).thenReturn(response);

        boolean status = client.getPing();
        assertEquals(true, status);
        verify(mockedAdminRestClient).get(path);
    }

    @Test
    public void testGetOrganization() throws AdminRestClientException, LoginTCException {
        String path = "/api/organization";
        String response = createJson("{\"name\": \"%s\"}", organizationName);

        when(mockedAdminRestClient.get(path)).thenReturn(response);

        Organization organization = client.getOrganization();
        assertEquals("Chrome Stage", organization.getName());
        verify(mockedAdminRestClient).get(path);
    }

    @Test
    public void testGetDomain() throws AdminRestClientException, LoginTCException {
        String path = String.format("/api/domains/%s", domainId);
        String response = createJson(
                "{'id':'%s','name':'%s','type':'%s','keyType':'%s','maxAllowedRetries':'%d','requestTimeout':'%d','activationCodeExpiration':'%d','requestPollingEnabled':'%s','bypassEnabled':'%s'}",
                domainId, domainName, domainType, domainKeyType, domainMaxAllowedRetries, domainRequestTimeout,
                domainActivationCodeExpiration, domainRequestPollingEnabled,
                domainBypassEnabled);

        when(mockedAdminRestClient.get(path)).thenReturn(response);

        Domain domain = client.getDomain(domainId);
        assertEquals(domainId, domain.getId());
        assertEquals(domainName, domain.getName());
        assertEquals(domainType, domain.getType());
        assertEquals(domainKeyType, domain.getKeyType());
        assertEquals(domainMaxAllowedRetries, domain.getMaxAllowedRetries());
        assertEquals(domainRequestTimeout, domain.getRequestTimeout());
        assertEquals(domainActivationCodeExpiration, domain.getActivationCodeExpiration());
        assertEquals(domainRequestPollingEnabled, domain.getRequestPollingEnabled());
        assertEquals(domainBypassEnabled, domain.getBypassEnabled());
        verify(mockedAdminRestClient).get(path);
    }

    @Test
    public void testGetDomainUser() throws AdminRestClientException, LoginTCException {
        String path = String.format("/api/domains/%s/users/%s", domainId, userId);
        String response = createJson("{'id':'%s','username':'%s','email':'%s','name':'%s','domains':['%s'],'hardware':''}", userId, userUsername,
                userEmail, userName, domainId);

        when(mockedAdminRestClient.get(path)).thenReturn(response);

        User user = client.getDomainUser(domainId, userId);
        assertEquals(userUsername, user.getUsername());
        assertEquals(userEmail, user.getEmail());
        assertEquals(userName, user.getName());
        assertEquals(1, user.getDomains().size());
        assertEquals(domainId, user.getDomains().get(0));
        verify(mockedAdminRestClient).get(path);
    }

    @Test
    public void testGetDomainUsers() throws AdminRestClientException, LoginTCException {
        String path = String.format("/api/domains/%s/users", domainId);
        String query = "page=1";

        String response = createJson(
                "[{'id':'%s','username':'%s','email':'%s','name':'%s','domains':['%s'],'hardware':''},{'id':'%s','username':'%s','email':'%s','name':'%s','domains':['%s'],'hardware':''}]",
                userId, userUsername, userEmail, userName, domainId, userId, userUsername, userEmail, userName, domainId);

        when(mockedAdminRestClient.get(path, query)).thenReturn(response);

        List<User> users = client.getDomainUsers(domainId);

        assertEquals(2, users.size());
        assertEquals(userUsername, users.get(0).getUsername());
        assertEquals(userEmail, users.get(0).getEmail());
        assertEquals(userName, users.get(0).getName());
        assertEquals(1, users.get(0).getDomains().size());
        assertEquals(domainId, users.get(0).getDomains().get(0));

        assertEquals(userUsername, users.get(1).getUsername());
        assertEquals(userEmail, users.get(1).getEmail());
        assertEquals(userName, users.get(1).getName());
        assertEquals(1, users.get(1).getDomains().size());
        assertEquals(domainId, users.get(1).getDomains().get(0));
        verify(mockedAdminRestClient).get(path, query);
    }

    @Test
    public void testGetDomainImage() throws AdminRestClientException, LoginTCException {
        String path = String.format("/api/domains/%s/image", domainId);
        String response = "Hello World!";
        when(mockedAdminRestClient.getBytes(path, "image/png")).thenReturn(response.getBytes());

        byte[] image = client.getDomainImage(domainId);
        String imageText = new String(image);
        assertEquals(response, imageText);
        verify(mockedAdminRestClient).getBytes(path, "image/png");
    }

}
