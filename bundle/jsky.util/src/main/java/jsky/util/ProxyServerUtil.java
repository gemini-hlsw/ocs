/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: ProxyServerUtil.java 4818 2004-07-08 19:12:01Z brighton $
 */

package jsky.util;

import java.util.Properties;

/**
 * A utility class for managing access to a proxy server. If set, a proxy server
 * is used to access URLs, usually when a firewall is in place. The proxy server,
 * specified as a host name and port number, does the HTTP GET for us and returns
 * the result.
 *
 * @author Allan Brighton
 * @version $Revision: 4818 $
 */
public class ProxyServerUtil {

    // Keys to use to save proxy settings
    private static final String HTTP_PROXY_HOST = "http.proxyHost";
    private static final String HTTP_PROXY_PORT = "http.proxyPort";
    private static final String HTTPS_PROXY_HOST = "https.proxyHost";
    private static final String HTTPS_PROXY_PORT = "https.proxyPort";
    private static final String HTTP_NON_PROXY_HOSTS = "http.nonProxyHosts";
    private static final String HTTPS_NON_PROXY_HOSTS = "https.nonProxyHosts";


    /**
     * This method should be called once at startup, so that any previous
     * proxy settings are restored.
     */
    public static void init() {
        // Check for saved preferences
        String savedHttpProxyHost = Preferences.get(HTTP_PROXY_HOST);
        String savedHttpProxyPort = Preferences.get(HTTP_PROXY_PORT);
        String savedHttpsProxyHost = Preferences.get(HTTPS_PROXY_HOST);
        String savedHttpsProxyPort = Preferences.get(HTTPS_PROXY_PORT);
        String savedHttpNonProxyHosts = Preferences.get(HTTP_NON_PROXY_HOSTS);
        String savedHttpsNonProxyHosts = Preferences.get(HTTPS_NON_PROXY_HOSTS);

        // Java properties (-D options) override saved preferences
        String httpProxyHostOption = System.getProperty(HTTP_PROXY_HOST);
        String httpProxyPortOption = System.getProperty(HTTP_PROXY_PORT);
        String httpsProxyHostOption = System.getProperty(HTTPS_PROXY_HOST);
        String httpsProxyPortOption = System.getProperty(HTTPS_PROXY_PORT);
        String httpNonProxyHostsOption = System.getProperty(HTTP_NON_PROXY_HOSTS);
        String httpsNonProxyHostsOption = System.getProperty(HTTPS_NON_PROXY_HOSTS);

        String host = httpProxyHostOption;
        if (host == null) {
            host = savedHttpProxyHost;
        }
        if (host != null && host != httpProxyHostOption && host.length() != 0) {
            System.setProperty(HTTP_PROXY_HOST, host);
        }

        String port = httpProxyPortOption;
        if (port == null) {
            port = savedHttpProxyPort;
        }
        if (port != null && port != httpProxyPortOption) {
            System.setProperty(HTTP_PROXY_PORT, port);
        }

        host = httpsProxyHostOption;
        if (host == null) {
            host = savedHttpsProxyHost;
        }
        if (host != null && host != httpsProxyHostOption && host.length() != 0) {
            System.setProperty(HTTPS_PROXY_HOST, host);
        }

        port = httpsProxyPortOption;
        if (port == null) {
            port = savedHttpsProxyPort;
        }
        if (port != null && port != httpsProxyPortOption) {
            System.setProperty(HTTPS_PROXY_PORT, port);
        }

        String httpNonProxyHosts = httpNonProxyHostsOption;
        if (httpNonProxyHosts == null) {
            httpNonProxyHosts = savedHttpNonProxyHosts;
        }
        if (httpNonProxyHosts != null
                && httpNonProxyHosts != httpNonProxyHostsOption
                && httpNonProxyHosts.length() != 0) {
            System.setProperty(HTTP_NON_PROXY_HOSTS, httpNonProxyHosts);
        }

        String httpsNonProxyHosts = httpsNonProxyHostsOption;
        if (httpsNonProxyHosts == null) {
            httpsNonProxyHosts = savedHttpsNonProxyHosts;
        }
        if (httpsNonProxyHosts != null
                && httpsNonProxyHosts != httpsNonProxyHostsOption
                && httpsNonProxyHosts.length() != 0) {
            System.setProperty(HTTPS_NON_PROXY_HOSTS, httpsNonProxyHosts);
        }
    }

    /**
     * Set the proxy server information.
     *
     * @param httpProxyHost  HTTP proxy server host
     * @param httpProxyPort  HTTP proxy server port number
     * @param httpsProxyHost HTTPS proxy server host
     * @param httpsProxyPort HTTPS proxy server port number
     * @param httpNonProxyHosts  a list of domains not requiring a proxy server (separated by spaces)
     */
    public static void setProxy(String httpProxyHost, int httpProxyPort,
                                String httpsProxyHost, int httpsProxyPort,
                                String httpNonProxyHosts, String httpsNonProxyHosts) {

        String httpProxyPortStr = String.valueOf(httpProxyPort);
        String httpsProxyPortStr = String.valueOf(httpsProxyPort);

        Preferences.set(HTTP_PROXY_HOST, httpProxyHost);
        Preferences.set(HTTP_PROXY_PORT, httpProxyPortStr);
        Preferences.set(HTTPS_PROXY_HOST, httpsProxyHost);
        Preferences.set(HTTPS_PROXY_PORT, httpsProxyPortStr);
        Preferences.set(HTTP_NON_PROXY_HOSTS, httpNonProxyHosts);
        Preferences.set(HTTPS_NON_PROXY_HOSTS, httpsNonProxyHosts);

        System.setProperty(HTTP_PROXY_HOST, httpProxyHost);
        System.setProperty(HTTP_PROXY_PORT, httpProxyPortStr);
        System.setProperty(HTTPS_PROXY_HOST, httpsProxyHost);
        System.setProperty(HTTPS_PROXY_PORT, httpsProxyPortStr);
        System.setProperty(HTTP_NON_PROXY_HOSTS, httpNonProxyHosts);
        System.setProperty(HTTPS_NON_PROXY_HOSTS, httpsNonProxyHosts);
    }

    /**
     * Removes any current proxy settings
     */
    public static void clearProxySettings() {
        Preferences.unset(HTTP_PROXY_HOST);
        Preferences.unset(HTTP_PROXY_PORT);
        Preferences.unset(HTTPS_PROXY_HOST);
        Preferences.unset(HTTPS_PROXY_PORT);
        Preferences.unset(HTTP_NON_PROXY_HOSTS);
        Preferences.unset(HTTPS_NON_PROXY_HOSTS);

        Properties props = System.getProperties();
        props.remove(HTTP_PROXY_HOST);
        props.remove(HTTP_PROXY_PORT);
        props.remove(HTTPS_PROXY_HOST);
        props.remove(HTTPS_PROXY_PORT);
        props.remove(HTTP_NON_PROXY_HOSTS);
        props.remove(HTTPS_NON_PROXY_HOSTS);
    }

    /**
     * Return the HTTP proxy server host name.
     */
    public static String getHttpProxyHost() {
        return System.getProperty(HTTP_PROXY_HOST);
    }

    /**
     * Return the HTTP proxy server port.
     */
    public static int getHttpProxyPort() {
        String s = System.getProperty(HTTP_PROXY_PORT);
        if (s == null) {
            return 80;
        }
        return Integer.parseInt(s);
    }

    /**
     * Return the HTTPS proxy server host name.
     */
    public static String getHttpsProxyHost() {
        return System.getProperty(HTTPS_PROXY_HOST);
    }

    /**
     * Return the HTTPS proxy server port.
     */
    public static int getHttpsProxyPort() {
        String s = System.getProperty(HTTPS_PROXY_PORT);
        if (s == null) {
            return 443;
        }
        return Integer.parseInt(s);
    }

    /**
     * Return the space separated list of domains not requiring a proxy for HTTP.
     */
    public static String getHttpNonProxyHosts() {
        return System.getProperty(HTTP_NON_PROXY_HOSTS);
    }

    /**
     * Return the space separated list of domains not requiring a proxy for HTTPS.
     */
    public static String getHttpsNonProxyHosts() {
        return System.getProperty(HTTPS_NON_PROXY_HOSTS);
    }
}

