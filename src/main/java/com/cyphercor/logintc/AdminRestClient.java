
package com.cyphercor.logintc;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

/**
 * HTTP REST client for LoginTC Admin.
 */
class AdminRestClient {
    private static final String CONTENT_TYPE = "application/vnd.logintc.v1+json";

    /**
     * Exception thrown out of AdminClient.
     */
    public static class AdminRestClientException extends Exception {
        private static final long serialVersionUID = -3479118087809884898L;

        public AdminRestClientException(String message) {
            super(message);
        }

        public AdminRestClientException(Throwable throwable) {
            super(throwable);
        }
    }

    /**
     * Exception caused by internal non-REST related exception.
     */
    public static class InternalAdminRestClientException extends AdminRestClientException {
        private static final long serialVersionUID = -7945306952585650400L;

        public InternalAdminRestClientException(Throwable throwable) {
            super(throwable);
        }
    }

    /**
     * Exception caused by 4xx or 5xx REST response. Includes details such as
     * the HTTP status code and response body.
     */
    public static class RestAdminRestClientException extends AdminRestClientException {
        private static final long serialVersionUID = 2848640201316730052L;

        private Integer statusCode = null;
        private String body = null;

        public RestAdminRestClientException(Integer statusCode, String body) {
            super("API returned status code " + statusCode.toString());

            this.statusCode = statusCode;
            this.body = body;
        }

        public Integer getStatusCode() {
            return this.statusCode;
        }

        public String getBody() {
            return this.body;
        }
    }

    private String scheme = null;
    private String host = null;
    private Integer port = null;
    private String apiKey = null;
    private String userAgent = null;

    private DefaultHttpClient httpClient = null;

    public AdminRestClient(String scheme, String host, Integer port, String apiKey, String userAgent) {
        this.scheme = scheme;
        this.host = host;
        this.port = port;
        this.apiKey = apiKey;
        this.userAgent = userAgent;

        httpClient = new DefaultHttpClient();
    }

    public void setProxy(String proxyHost, int proxyPort) {
        HttpHost proxy = new HttpHost(proxyHost, proxyPort, "http");
        httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
    }

    public void setProxy(String proxyHost, int proxyPort, String proxyUser, String proxyPassword) {
        CredentialsProvider credsProvider = httpClient.getCredentialsProvider();
        credsProvider.setCredentials(new AuthScope(proxyHost, proxyPort), new UsernamePasswordCredentials(proxyUser, proxyPassword));
        httpClient.setCredentialsProvider(credsProvider);

        HttpHost proxy = new HttpHost(proxyHost, proxyPort, "http");
        httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
    }

    public String get(String path) throws AdminRestClientException {
        HttpGet request = new HttpGet(genUri(path));

        request.setHeader("Accept", CONTENT_TYPE);
        return new String(execute(request));
    }

    public String post(String path, String body) throws AdminRestClientException {
        HttpPost request = new HttpPost(genUri(path));
        request.setHeader("Accept", CONTENT_TYPE);

        try {
            if (body != null) {
                request.setEntity(new StringEntity(body));
                request.setHeader("Content-Type", CONTENT_TYPE);
            }
        } catch (UnsupportedEncodingException e) {
            throw new InternalAdminRestClientException(e);
        }

        return new String(execute(request));
    }

    public String put(String path, String body) throws AdminRestClientException {
        HttpPut request = new HttpPut(genUri(path));
        request.setHeader("Accept", CONTENT_TYPE);

        try {
            if (body != null) {
                request.setEntity(new StringEntity(body));
                request.setHeader("Content-Type", CONTENT_TYPE);
            }
        } catch (UnsupportedEncodingException e) {
            throw new InternalAdminRestClientException(e);
        }

        return new String(execute(request));
    }

    public String delete(String path) throws AdminRestClientException {
        HttpDelete request = new HttpDelete(genUri(path));
        request.setHeader("Accept", CONTENT_TYPE);
        request.setHeader("Content-Length", "0");

        return new String(execute(request));
    }

    private byte[] execute(HttpRequestBase request) throws AdminRestClientException {
        byte[] responseBodyBytes = null;
        Integer statusCode = 0;

        request.setHeader("Host", genHostHeaderValue());
        request.setHeader("User-Agent", this.userAgent);
        request.setHeader("Authorization", String.format("LoginTC key=\"%s\"", apiKey));

        HttpResponse response = null;

        try {
            response = httpClient.execute(request);
        } catch (ClientProtocolException e) {
            throw new InternalAdminRestClientException(e);
        } catch (IOException e) {
            throw new InternalAdminRestClientException(e);
        }

        statusCode = response.getStatusLine().getStatusCode();

        try {
            responseBodyBytes = EntityUtils.toByteArray(response.getEntity());
        } catch (IOException e) {
            throw new InternalAdminRestClientException(e);
        }

        switch (statusCode) {
            case 200: // OK
            case 201: // Created
            case 202: // Accepted
                break;
            case 400: // Bad Request
            case 401: // Unauthorized
            case 403: // Forbidden
            case 404: // Not Found
            case 405: // Method Not Allowed
            case 406: // Not Acceptable
            case 410: // Gone
            case 429: // Too Many Requests
            case 500: // Internal Server Error
            case 501: // Not Implemented
            case 502: // Bad Gateway
            case 503: // Service Unavailable
            case 504: // Gateway Timeout
            default:
                String responseBodyString = new String(responseBodyBytes);
                throw new RestAdminRestClientException(statusCode, responseBodyString);
        }

        return responseBodyBytes;
    }

    private String genHostHeaderValue() {
        String host = this.host;

        if ((scheme.equals("https")) && (port != 443)) {
            host = host + ":" + this.port;
        } else if ((scheme.equals("http")) && (port != 80)) {
            host = host + ":" + this.port;
        }

        return host;
    }

    private URI genUri(String path) throws AdminRestClientException {
        try {
            return new URI(scheme, null, host, port, path, null, null);
        } catch (URISyntaxException e) {
            throw new InternalAdminRestClientException(e);
        }
    }

}
