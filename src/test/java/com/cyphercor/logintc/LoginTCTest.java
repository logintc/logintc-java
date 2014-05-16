
package com.cyphercor.logintc;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.cyphercor.logintc.AdminRestClient.AdminRestClientException;
import com.cyphercor.logintc.AdminRestClient.RestAdminRestClientException;
import com.cyphercor.logintc.LoginTC.LoginTCException;
import com.cyphercor.logintc.LoginTC.NoTokenLoginTCException;
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

        when(mockedAdminRestClient.post(path, body)).thenReturn(response);

        Map<String, String> attributes = new LinkedHashMap<String, String>();
        attributes.put("Product", "Quinoa");
        attributes.put("Price", "42");

        Session session = client.createSession(domainId, userId, attributes);
        assertEquals(sessionId, session.getId());
        assertEquals(Session.State.PENDING, session.getState());

        verify(mockedAdminRestClient).post(path, body);
    }

    @Test(expected = NoTokenLoginTCException.class)
    public void testCreateSessionNoTokenLoginTCException() throws AdminRestClientException, LoginTCException {
        String path = String.format("/api/domains/%s/sessions", domainId, userId);
        String body = createJson("{'attributes':[{'value':'Quinoa','key':'Product'},{'value':'42','key':'Price'}],'user':{'id':'%s'}}",
                userId);

        when(mockedAdminRestClient.post(path, body)).thenThrow(
                new RestAdminRestClientException(404, createJson("{'errors':[{'code':'api.error.notfound.token','message':''}]}")));

        Map<String, String> attributes = new LinkedHashMap<String, String>();
        attributes.put("Product", "Quinoa");
        attributes.put("Price", "42");

        client.createSession(domainId, userId, attributes);
    }

    @Test
    public void testCreateUser() throws AdminRestClientException, LoginTCException {
        String path = String.format("/api/users", domainId, userId);
        String body = createJson("{'username':'%s','email':'%s','name':'%s'}", userUsername, userEmail, userName);
        String response = createJson("{'id':'%s','username':'%s','email':'%s','name':'%s','domains':[]}", userId, userUsername, userEmail,
                userName);

        when(mockedAdminRestClient.post(path, body)).thenReturn(response);

        User user = client.createUser(userUsername, userEmail, userName);
        assertEquals(userId, user.getId());
        assertEquals(userUsername, user.getUsername());
        assertEquals(userEmail, user.getEmail());
        assertEquals(userName, user.getName());

        verify(mockedAdminRestClient).post(path, body);
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
        String response = createJson("{'id':'%s','username':'%s','email':'%s','name':'%s','domains':['%s']}", userId, userUsername,
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

        when(mockedAdminRestClient.put(path, body)).thenReturn(null);

        List<User> users = new ArrayList<User>();
        users.add(new User("user1", "user1@cyphercor.com", "user one"));
        users.add(new User("user2", "user2@cyphercor.com", "user two"));

        client.setDomainUsers(domainId, users);

        verify(mockedAdminRestClient).put(path, body);
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
        String response = createJson("{'id':'%s','username':'%s','email':'%s','name':'%s','domains':[]}", userId, userUsername,
                "new@cyphercor.com", "New Name");

        when(mockedAdminRestClient.put(path, body)).thenReturn(response);

        User user = client.updateUser(userId, "new@cyphercor.com", "New Name");
        assertEquals("new@cyphercor.com", user.getEmail());
        assertEquals("New Name", user.getName());

        verify(mockedAdminRestClient).put(path, body);
    }
}
