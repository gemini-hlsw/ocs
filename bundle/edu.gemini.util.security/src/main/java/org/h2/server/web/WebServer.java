/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.server.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.Map.Entry;
import org.h2.constant.SysProperties;
import org.h2.engine.Constants;
import org.h2.message.TraceSystem;
import org.h2.server.Service;
import org.h2.server.ShutdownHandler;
import org.h2.store.fs.FileUtils;
import org.h2.util.StringUtils;
import org.h2.util.Tool;
import org.h2.util.Utils;
import org.h2.util.IOUtils;
import org.h2.util.JdbcUtils;
import org.h2.util.MathUtils;
import org.h2.util.NetUtils;
import org.h2.util.New;
import org.h2.util.SortedProperties;

/**
 * The web server is a simple standalone HTTP server that implements the H2
 * Console application. It is not optimized for performance.
 */
public class WebServer implements Service {

    static final String TRANSFER = "transfer";

    static final String[][] LANGUAGES = {
        { "cs", "\u010ce\u0161tina" },
        { "de", "Deutsch" },
        { "en", "English" },
        { "es", "Espa\u00f1ol" },
        { "fr", "Fran\u00e7ais" },
        { "hu", "Magyar"},
        { "ko", "\ud55c\uad6d\uc5b4"},
        { "in", "Indonesia"},
        { "it", "Italiano"},
        { "ja", "\u65e5\u672c\u8a9e"},
        { "nl", "Nederlands"},
        { "pl", "Polski"},
        { "pt_BR", "Portugu\u00eas (Brasil)"},
        { "pt_PT", "Portugu\u00eas (Europeu)"},
        { "ru", "\u0440\u0443\u0441\u0441\u043a\u0438\u0439"},
        { "sk", "Slovensky"},
        { "tr", "T\u00fcrk\u00e7e"},
        { "uk", "\u0423\u043A\u0440\u0430\u0457\u043D\u0441\u044C\u043A\u0430"},
        { "zh_CN", "\u4e2d\u6587 (\u7b80\u4f53)"},
        { "zh_TW", "\u4e2d\u6587 (\u7e41\u9ad4)"},
    };

    private static final String DEFAULT_LANGUAGE = "en";

    private static final String[] GENERIC = {
        "Generic JNDI Data Source|javax.naming.InitialContext|java:comp/env/jdbc/Test|sa",
        "Generic Firebird Server|org.firebirdsql.jdbc.FBDriver|jdbc:firebirdsql:localhost:c:/temp/firebird/test|sysdba",
        "Generic SQLite|org.sqlite.JDBC|jdbc:sqlite:test|sa",
        "Generic DB2|COM.ibm.db2.jdbc.net.DB2Driver|jdbc:db2://localhost/test|" ,
        "Generic Oracle|oracle.jdbc.driver.OracleDriver|jdbc:oracle:thin:@localhost:1521:XE|sa" ,
        "Generic MS SQL Server 2000|com.microsoft.jdbc.sqlserver.SQLServerDriver|" +
                "jdbc:microsoft:sqlserver://localhost:1433;DatabaseName=sqlexpress|sa",
        "Generic MS SQL Server 2005|com.microsoft.sqlserver.jdbc.SQLServerDriver|" +
                "jdbc:sqlserver://localhost;DatabaseName=test|sa",
        "Generic PostgreSQL|org.postgresql.Driver|jdbc:postgresql:test|" ,
        "Generic MySQL|com.mysql.jdbc.Driver|jdbc:mysql://localhost:3306/test|" ,
        "Generic HSQLDB|org.hsqldb.jdbcDriver|jdbc:hsqldb:test;hsqldb.default_table_type=cached|sa" ,
        "Generic Derby (Server)|org.apache.derby.jdbc.ClientDriver|jdbc:derby://localhost:1527/test;create=true|sa",
        "Generic Derby (Embedded)|org.apache.derby.jdbc.EmbeddedDriver|jdbc:derby:test;create=true|sa",
        "Generic H2 (Server)|org.h2.Driver|jdbc:h2:tcp://localhost/~/test|sa",
        // this will be listed on top for new installations
        "Generic H2 (Embedded)|org.h2.Driver|jdbc:h2:~/test|sa",
    };

    private static int ticker;

    /**
     * The session timeout is 30 min.
     */
    private static final long SESSION_TIMEOUT = 30 * 60 * 1000;

//    public static void main(String... args) throws IOException {
//        String s = IOUtils.readStringAndClose(new java.io.FileReader(
//                // "src/main/org/h2/server/web/res/_text_cs.prop"), -1);
//                "src/main/org/h2/res/_messages_cs.prop"), -1);
//        System.out.println(StringUtils.javaEncode("..."));
//        String[] list = Locale.getISOLanguages();
//        for (int i = 0; i < list.length; i++) {
//            System.out.print(list[i] + " ");
//        }
//        System.out.println();
//        String l = "de";
//        String lang = new java.util.Locale(l).
//            getDisplayLanguage(new java.util.Locale(l));
//        System.out.println(new java.util.Locale(l).getDisplayLanguage());
//        System.out.println(lang);
//        java.util.Locale.CHINESE.getDisplayLanguage(java.util.Locale.CHINESE);
//        for (int i = 0; i < lang.length(); i++) {
//            System.out.println(Integer.toHexString(lang.charAt(i)) + " ");
//        }
//    }

    // private URLClassLoader urlClassLoader;
    private int port;
    private boolean allowOthers;
    private boolean isDaemon;
    private final Set<WebThread> running = Collections.synchronizedSet(new HashSet<WebThread>());
    private boolean ssl;
    private final HashMap<String, ConnectionInfo> connInfoMap = New.hashMap();

    private long lastTimeoutCheck;
    private final HashMap<String, WebSession> sessions = New.hashMap();
    private final HashSet<String> languages = New.hashSet();
    private String startDateTime;
    private ServerSocket serverSocket;
    private String url;
    private ShutdownHandler shutdownHandler;
    private Thread listenerThread;
    private boolean ifExists;
    private boolean trace;
    private TranslateThread translateThread;
    private boolean allowChunked = true;
    private String serverPropertiesDir = Constants.SERVER_PROPERTIES_DIR;

    /**
     * Read the given file from the file system or from the resources.
     *
     * @param file the file name
     * @return the data
     */
    byte[] getFile(String file) throws IOException {
        trace("getFile <" + file + ">");
        if (file.startsWith(TRANSFER + "/") && new File(TRANSFER).exists()) {
            file = file.substring(TRANSFER.length() + 1);
            if (!isSimpleName(file)) {
                return null;
            }
            File f = new File(TRANSFER, file);
            if (!f.exists()) {
                return null;
            }
            return IOUtils.readBytesAndClose(new FileInputStream(f), -1);
        }
        byte[] data = Utils.getResource("/org/h2/server/web/res/" + file);
        if (data == null) {
            trace(" null");
        } else {
            trace(" size=" + data.length);
        }
        return data;
    }

    /**
     * Check if this is a simple name (only contains '.', '-', '_', letters, or
     * digits).
     *
     * @param s the string
     * @return true if it's a simple name
     */
    static boolean isSimpleName(String s) {
        for (char c : s.toCharArray()) {
            if (c != '.' && c != '_' && c != '-' && !Character.isLetterOrDigit(c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Remove this web thread from the set of running threads.
     *
     * @param t the thread to remove
     */
    synchronized void remove(WebThread t) {
        running.remove(t);
    }

    private static String generateSessionId() {
        byte[] buff = MathUtils.secureRandomBytes(16);
        return StringUtils.convertBytesToHex(buff);
    }

    /**
     * Get the web session object for the given session id.
     *
     * @param sessionId the session id
     * @return the web session or null
     */
    WebSession getSession(String sessionId) {
        long now = System.currentTimeMillis();
        if (lastTimeoutCheck + SESSION_TIMEOUT < now) {
            for (String id : New.arrayList(sessions.keySet())) {
                WebSession session = sessions.get(id);
                if (session.lastAccess + SESSION_TIMEOUT < now) {
                    trace("timeout for " + id);
                    sessions.remove(id);
                }
            }
            lastTimeoutCheck = now;
        }
        WebSession session = sessions.get(sessionId);
        if (session != null) {
            session.lastAccess = System.currentTimeMillis();
        }
        return session;
    }

    /**
     * Create a new web session id and object.
     *
     * @param hostAddr the host address
     * @return the web session object
     */
    WebSession createNewSession(String hostAddr) {
        String newId;
        do {
            newId = generateSessionId();
        } while(sessions.get(newId) != null);
        WebSession session = new WebSession(this);
        session.lastAccess = System.currentTimeMillis();
        session.put("sessionId", newId);
        session.put("ip", hostAddr);
        session.put("language", DEFAULT_LANGUAGE);
        session.put("frame-border", "0");
        session.put("frameset-border", "4");
        sessions.put(newId, session);
        // always read the english translation,
        // so that untranslated text appears at least in english
        readTranslations(session, DEFAULT_LANGUAGE);
        return getSession(newId);
    }

    String getStartDateTime() {
        if (startDateTime == null) {
            SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", new Locale("en", ""));
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            startDateTime = format.format(new Date());
        }
        return startDateTime;
    }

    public void init(String... args) {
        // set the serverPropertiesDir, because it's used in loadProperties()
        for (int i = 0; args != null && i < args.length; i++) {
            if ("-properties".equals(args[i])) {
                serverPropertiesDir = args[++i];
            }
        }
        Properties prop = loadProperties();
        port = SortedProperties.getIntProperty(prop, "webPort", Constants.DEFAULT_HTTP_PORT);
        ssl = SortedProperties.getBooleanProperty(prop, "webSSL", false);
        allowOthers = SortedProperties.getBooleanProperty(prop, "webAllowOthers", false);
        for (int i = 0; args != null && i < args.length; i++) {
            String a = args[i];
            if (Tool.isOption(a, "-webPort")) {
                port = Integer.decode(args[++i]);
            } else if (Tool.isOption(a, "-webSSL")) {
                ssl = true;
            } else if (Tool.isOption(a, "-webAllowOthers")) {
                allowOthers = true;
            } else if (Tool.isOption(a, "-webDaemon")) {
                isDaemon = true;
            } else if (Tool.isOption(a, "-baseDir")) {
                String baseDir = args[++i];
                SysProperties.setBaseDir(baseDir);
            } else if (Tool.isOption(a, "-ifExists")) {
                ifExists = true;
            } else if (Tool.isOption(a, "-properties")) {
                // already set
                i++;
            } else if (Tool.isOption(a, "-trace")) {
                trace = true;
            }
        }
//            if(driverList != null) {
//                try {
//                    String[] drivers =
//                        StringUtils.arraySplit(driverList, ',', false);
//                    URL[] urls = new URL[drivers.length];
//                    for(int i=0; i<drivers.length; i++) {
//                        urls[i] = new URL(drivers[i]);
//                    }
//                    urlClassLoader = URLClassLoader.newInstance(urls);
//                } catch (MalformedURLException e) {
//                    TraceSystem.traceThrowable(e);
//                }
//            }
        for (String[] lang : LANGUAGES) {
            languages.add(lang[0]);
        }
        updateURL();
    }

    public String getURL() {
        updateURL();
        return url;
    }

    private void updateURL() {
        try {
            url = (ssl ? "https" : "http") + "://" + NetUtils.getLocalAddress() + ":" + port;
        } catch (NoClassDefFoundError e) {
            // Google App Engine does not allow java.net.InetAddress
        }
    }

    public void start() {
        serverSocket = NetUtils.createServerSocket(port, ssl);
        port = serverSocket.getLocalPort();
        updateURL();
    }

    public void listen() {
        this.listenerThread = Thread.currentThread();
        try {
            while (serverSocket != null) {
                Socket s = serverSocket.accept();
                WebThread c = new WebThread(s, this);
                running.add(c);
                c.start();
            }
        } catch (Exception e) {
            trace(e.toString());
        }
    }

    public boolean isRunning(boolean traceError) {
        if (serverSocket == null) {
            return false;
        }
        try {
            Socket s = NetUtils.createLoopbackSocket(port, ssl);
            s.close();
            return true;
        } catch (Exception e) {
            if (traceError) {
                traceError(e);
            }
            return false;
        }
    }

    public boolean isStopped() {
        return serverSocket == null;
    }

    public void stop() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                traceError(e);
            }
            serverSocket = null;
        }
        if (listenerThread != null) {
            try {
                listenerThread.join(1000);
            } catch (InterruptedException e) {
                TraceSystem.traceThrowable(e);
            }
        }
        // TODO server: using a boolean 'now' argument? a timeout?
        for (WebSession session : New.arrayList(sessions.values())) {
            session.close();
        }
        for (WebThread c : New.arrayList(running)) {
            try {
                c.stopNow();
                c.join(100);
            } catch (Exception e) {
                traceError(e);
            }
        }
    }

    /**
     * Write trace information if trace is enabled.
     *
     * @param s the message to write
     */
    void trace(String s) {
        if (trace) {
            System.out.println(s);
        }
    }

    /**
     * Write the stack trace if trace is enabled.
     *
     * @param e the exception
     */
    void traceError(Throwable e) {
        if (trace) {
            e.printStackTrace();
        }
    }

    /**
     * Check if this language is supported / translated.
     *
     * @param language the language
     * @return true if a translation is available
     */
    boolean supportsLanguage(String language) {
        return languages.contains(language);
    }

    /**
     * Read the translation for this language and save them in the 'text'
     * property of this session.
     *
     * @param session the session
     * @param language the language
     */
    void readTranslations(WebSession session, String language) {
        Properties text = new Properties();
        try {
            trace("translation: "+language);
            byte[] trans = getFile("_text_"+language+".prop");
            trace("  "+new String(trans));
            text = SortedProperties.fromLines(new String(trans, "UTF-8"));
            // remove starting # (if not translated yet)
            for (Entry<Object, Object> entry : text.entrySet()) {
                String value = (String) entry.getValue();
                if (value.startsWith("#")) {
                    entry.setValue(value.substring(1));
                }
            }
        } catch (IOException e) {
            TraceSystem.traceThrowable(e);
        }
        session.put("text", new HashMap<Object, Object>(text));
    }

    ArrayList<HashMap<String, Object>> getSessions() {
        ArrayList<HashMap<String, Object>> list = New.arrayList();
        for (WebSession s : sessions.values()) {
            list.add(s.getInfo());
        }
        return list;
    }

    public String getType() {
        return "Web Console";
    }

    public String getName() {
        return "H2 Console Server";
    }

    void setAllowOthers(boolean b) {
        allowOthers = b;
    }

    public boolean getAllowOthers() {
        return allowOthers;
    }

    void setSSL(boolean b) {
        ssl = b;
    }

    void setPort(int port) {
        this.port = port;
    }

    boolean getSSL() {
        return ssl;
    }

    public int getPort() {
        return port;
    }

    /**
     * Get the connection information for this setting.
     *
     * @param name the setting name
     * @return the connection information
     */
    ConnectionInfo getSetting(String name) {
        return connInfoMap.get(name);
    }

    /**
     * Update a connection information setting.
     *
     * @param info the connection information
     */
    void updateSetting(ConnectionInfo info) {
        connInfoMap.put(info.name, info);
        info.lastAccess = ticker++;
    }

    /**
     * Remove a connection information setting from the list
     *
     * @param name the setting to remove
     */
    void removeSetting(String name) {
        connInfoMap.remove(name);
    }

    private Properties loadProperties() {
        try {
            if ("null".equals(serverPropertiesDir)) {
                return new Properties();
            }
            return SortedProperties.loadProperties(serverPropertiesDir + "/" + Constants.SERVER_PROPERTIES_NAME);
        } catch (Exception e) {
            TraceSystem.traceThrowable(e);
            return new Properties();
        }
    }

    /**
     * Get the list of connection information setting names.
     *
     * @return the connection info names
     */
    String[] getSettingNames() {
        ArrayList<ConnectionInfo> list = getSettings();
        String[] names = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            names[i] = list.get(i).name;
        }
        return names;
    }

    /**
     * Get the list of connection info objects.
     *
     * @return the list
     */
    synchronized ArrayList<ConnectionInfo> getSettings() {
        ArrayList<ConnectionInfo> settings = New.arrayList();
        if (connInfoMap.size() == 0) {
            Properties prop = loadProperties();
            if (prop.size() == 0) {
                for (String gen : GENERIC) {
                    ConnectionInfo info = new ConnectionInfo(gen);
                    settings.add(info);
                    updateSetting(info);
                }
            } else {
                for (int i = 0;; i++) {
                    String data = prop.getProperty(String.valueOf(i));
                    if (data == null) {
                        break;
                    }
                    ConnectionInfo info = new ConnectionInfo(data);
                    settings.add(info);
                    updateSetting(info);
                }
            }
        } else {
            settings.addAll(connInfoMap.values());
        }
        Collections.sort(settings);
        return settings;
    }

    /**
     * Save the settings to the properties file.
     *
     * @param prop null or the properties webPort, webAllowOthers, and webSSL
     */
    synchronized void saveProperties(Properties prop) {
        try {
            if (prop == null) {
                Properties old = loadProperties();
                prop = new SortedProperties();
                prop.setProperty("webPort", "" + SortedProperties.getIntProperty(old, "webPort", port));
                prop.setProperty("webAllowOthers", "" + SortedProperties.getBooleanProperty(old, "webAllowOthers", allowOthers));
                prop.setProperty("webSSL", "" + SortedProperties.getBooleanProperty(old, "webSSL", ssl));
            }
            ArrayList<ConnectionInfo> settings = getSettings();
            int len = settings.size();
            for (int i = 0; i < len; i++) {
                ConnectionInfo info = settings.get(i);
                if (info != null) {
                    prop.setProperty(String.valueOf(len - i - 1), info.getString());
                }
            }
            if (!"null".equals(serverPropertiesDir)) {
                OutputStream out = FileUtils.newOutputStream(serverPropertiesDir + "/" + Constants.SERVER_PROPERTIES_NAME, false);
                prop.store(out, "H2 Server Properties");
                out.close();
            }
        } catch (Exception e) {
            TraceSystem.traceThrowable(e);
        }
    }

    /**
     * Open a database connection.
     *
     * @param driver the driver class name
     * @param databaseUrl the database URL
     * @param user the user name
     * @param password the password
     * @return the database connection
     */
    Connection getConnection(String driver, String databaseUrl, String user, String password) throws SQLException {
        driver = driver.trim();
        databaseUrl = databaseUrl.trim();
        org.h2.Driver.load();
        Properties p = new Properties();
        p.setProperty("user", user.trim());
        // do not trim the password, otherwise an
        // encrypted H2 database with empty user password doesn't work
        p.setProperty("password", password);
        if (databaseUrl.startsWith("jdbc:h2:")) {
            if (ifExists) {
                databaseUrl += ";IFEXISTS=TRUE";
            }
            // PostgreSQL would throw a NullPointerException
            // if it is loaded before the H2 driver
            // because it can't deal with non-String objects in the connection Properties
            return org.h2.Driver.load().connect(databaseUrl, p);
        }
//            try {
//                Driver dr = (Driver) urlClassLoader.
//                        loadClass(driver).newInstance();
//                return dr.connect(url, p);
//            } catch(ClassNotFoundException e2) {
//                throw e2;
//            }
        return JdbcUtils.getConnection(driver, databaseUrl, p);
    }

    /**
     * Shut down the web server.
     */
    void shutdown() {
        if (shutdownHandler != null) {
            shutdownHandler.shutdown();
        }
    }

    public void setShutdownHandler(ShutdownHandler shutdownHandler) {
        this.shutdownHandler = shutdownHandler;
    }

    /**
     * Create a session with a given connection.
     *
     * @param conn the connection
     * @return the URL of the web site to access this connection
     */
    public String addSession(Connection conn) throws SQLException {
        WebSession session = createNewSession("local");
        session.setShutdownServerOnDisconnect();
        session.setConnection(conn);
        session.put("url", conn.getMetaData().getURL());
        String s = (String) session.get("sessionId");
        return url + "/frame.jsp?jsessionid=" + s;
    }

    /**
     * The translate thread reads and writes the file translation.properties
     * once a second.
     */
    private class TranslateThread extends Thread {

        private final File file = new File("translation.properties");
        private final Map<Object, Object> translation;
        private volatile boolean stopNow;

        TranslateThread(Map<Object, Object> translation) {
            this.translation = translation;
        }

        public String getFileName() {
            return file.getAbsolutePath();
        }

        public void stopNow() {
            this.stopNow = true;
            try {
                join();
            } catch (InterruptedException e) {
                // ignore
            }
        }

        public void run() {
            while (!stopNow) {
                try {
                    SortedProperties sp = new SortedProperties();
                    if (file.exists()) {
                        InputStream in = FileUtils.newInputStream(file.getName());
                        sp.load(in);
                        translation.putAll(sp);
                    } else {
                        OutputStream out = FileUtils.newOutputStream(file.getName(), false);
                        sp.putAll(translation);
                        sp.store(out, "Translation");
                    }
                    Thread.sleep(1000);
                } catch (Exception e) {
                    traceError(e);
                }
            }
        }

    }

    /**
     * Start the translation thread that reads the file once a second.
     *
     * @param translation the translation map
     * @return the name of the file to translate
     */
    String startTranslate(Map<Object, Object> translation) {
        if (translateThread != null) {
            translateThread.stopNow();
        }
        translateThread = new TranslateThread(translation);
        translateThread.setDaemon(true);
        translateThread.start();
        return translateThread.getFileName();
    }

    public boolean isDaemon() {
        return isDaemon;
    }

    void setAllowChunked(boolean allowChunked) {
        this.allowChunked = allowChunked;
    }

    boolean getAllowChunked() {
        return allowChunked;
    }

}
