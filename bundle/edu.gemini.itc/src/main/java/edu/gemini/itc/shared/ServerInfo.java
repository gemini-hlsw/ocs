// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
// $Id: ServerInfo.java,v 1.2 2003/11/21 14:31:02 shane Exp $
//
package edu.gemini.itc.shared;


public class ServerInfo {
    public static String _serverName = "localhost";
    public static String _serverPort = "";
    public static String _urlPrefix = "http://";

    public static void setServerName(String serverName) {
        _serverName = serverName;
    }

    public static String getServerName() {
        return _serverName;
    }

    public static void setServerPort(int serverPort) {
        _serverPort = new Integer(serverPort).toString();
    }

    public static String getServerPort() {
        return _serverPort;
    }

    public static String getUrlPrefix() {
        return _urlPrefix;
    }

    public static void setUrlPrefix(String urlPrefix) {
        _urlPrefix = urlPrefix;
    }

    public static String getServerURL() {
        return getUrlPrefix() + getServerName() + ":" + getServerPort() + "/";
    }
}
