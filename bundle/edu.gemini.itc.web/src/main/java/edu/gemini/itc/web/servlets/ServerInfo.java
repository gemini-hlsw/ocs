package edu.gemini.itc.web.servlets;


public class ServerInfo {
    public static String _serverName = "localhost";
    public static String _serverPort = "";
    public static String _urlPrefix = "http://";

    public static void setServerName(final String serverName) {
        _serverName = serverName;
    }

    public static String getServerName() {
        return _serverName;
    }

    public static void setServerPort(final int serverPort) {
        _serverPort = new Integer(serverPort).toString();
    }

    public static String getServerPort() {
        return _serverPort;
    }

    public static String getUrlPrefix() {
        return _urlPrefix;
    }

    public static void setUrlPrefix(final String urlPrefix) {
        _urlPrefix = urlPrefix;
    }

    public static String getServerURL() {
        return getUrlPrefix() + getServerName() + ":" + getServerPort() + "/";
    }
}
