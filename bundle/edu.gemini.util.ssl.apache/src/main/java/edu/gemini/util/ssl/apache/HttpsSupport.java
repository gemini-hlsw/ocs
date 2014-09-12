package edu.gemini.util.ssl.apache;

import edu.gemini.util.ssl.GemSslSocketFactory;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.*;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.IOException;

/**
 * Support for using https with self-signed certificates.
 */
public final class HttpsSupport {
    private static final Logger LOG = Logger.getLogger(HttpsSupport.class.getName());

    private static Protocol _https;

    static {
        // Handle https with a custom protocol socket factory.
        _https = new Protocol("https", new ApacheProtocolSocketFactory(GemSslSocketFactory.get()), 443);
        Protocol.registerProtocol("https", _https);
    }

    public static Protocol getProtocol() {
        return _https;
    }

    private static String _getProxySetting(String proxySetting) {
        // First check for the preferred (https) setting
        String res = System.getProperty("https." + proxySetting);
        if (res != null) {
            res = res.trim();
            if (!"".equals(res)) return res;
        }

        // Okay there wasn't one, so use the fallback (http) setting
        LOG.fine("https." + proxySetting + " not set, checking http." + proxySetting);
        res = System.getProperty("http." + proxySetting);
        if (res != null) {
            res = res.trim();
            if (!"".equals(res)) return res;
        }

        // Neither https nor http proxy settings recorded
        return null;

    }

    /**
     * Gets the https proxy host, if configured.  Defaults to the http.proxyHost
     * if https.proxyHost not set.
     *
     * @return proxy host as configured in the https.proxyHost (or http.proxyHost)
     * system properties or <code>null</code> if there is no proxy host
     */
    public static String getProxyHost() {
        return _getProxySetting("proxyHost");
    }

    /**
     * Gets the https proxy port, if configured.  Defaults to the http proxy
     * port.
     *
     * @return proxy port as configured in the https.proxyPort (or http.proxyPort)
     * system properties or <code>null</code> if there is no proxy port
     */
    public static int getProxyPort() {
        String proxyPortStr = _getProxySetting("proxyPort");
        if (proxyPortStr == null) {
            LOG.fine("https.proxyPort not set");
            return -1;
        }

        int port;
        try {
            port = Integer.parseInt(proxyPortStr);
        } catch (NumberFormatException ex) {
            LOG.log(Level.WARNING, "Not a valid port number: " + proxyPortStr, ex);
            return -1;
        }
        if (port < 0) port = -1;
        return port;
    }

    /**
     * Gets an HttpConnection, taking into account and setting the proxy host
     * and port if configured via the appropriate system properties (see
     * {@link #getProxyHost} and {@link #getProxyPort}).
     */
    public static HttpConnection getConnection(HttpMethod method, int timeoutMs) throws URIException {
        URI uri = method.getURI();
        HttpConnection res;
        res = new HttpConnection(uri.getHost(), uri.getPort(), _https);
        res.setConnectionTimeout(timeoutMs);

        // Configure the proxy, if set.
        String proxyHost = getProxyHost();
        if (proxyHost == null) {
            LOG.fine("no proxy host");
            return res;
        }

        int port = getProxyPort();
        if (port == -1) {
            LOG.fine("no proxy port");
            return res;
        }

        LOG.info("Using proxy " + proxyHost + ":" + port);

        res.setProxyHost(proxyHost);
        res.setProxyPort(port);

        return res;
    }

    /**
     * Gets an HttpConnection and executes the given method.
     */
    public static int execute(HttpMethod method, int timeoutMs) throws IOException {
        HttpConnection httpConnection = getConnection(method, timeoutMs);
        if (httpConnection.isProxied()) {
            ConnectMethod cm = new ConnectMethod(method);
            return cm.execute(new HttpState(), httpConnection);
        } else {
            return method.execute(new HttpState(), httpConnection);
        }
    }
}
