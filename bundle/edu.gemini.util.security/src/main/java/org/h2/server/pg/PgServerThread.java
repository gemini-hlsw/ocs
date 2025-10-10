/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.server.pg;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.Socket;
import java.sql.Connection;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import org.h2.command.CommandInterface;
import org.h2.constant.SysProperties;
import org.h2.engine.ConnectionInfo;
import org.h2.jdbc.JdbcConnection;
import org.h2.jdbc.JdbcPreparedStatement;
import org.h2.jdbc.JdbcStatement;
import org.h2.message.DbException;
import org.h2.util.IOUtils;
import org.h2.util.JdbcUtils;
import org.h2.util.ScriptReader;
import org.h2.util.StringUtils;
import org.h2.util.Utils;
import org.h2.value.CaseInsensitiveMap;

/**
 * One server thread is opened for each client.
 */
public class PgServerThread implements Runnable {
    private final PgServer server;
    private Socket socket;
    private Connection conn;
    private boolean stop;
    private DataInputStream dataInRaw;
    private DataInputStream dataIn;
    private OutputStream out;
    private int messageType;
    private ByteArrayOutputStream outBuffer;
    private DataOutputStream dataOut;
    private Thread thread;
    private boolean initDone;
    private String userName;
    private String databaseName;
    private int processId;
    private String clientEncoding = SysProperties.PG_DEFAULT_CLIENT_ENCODING;
    private String dateStyle = "ISO";
    private final HashMap<String, Prepared> prepared = new CaseInsensitiveMap<Prepared>();
    private final HashMap<String, Portal> portals = new CaseInsensitiveMap<Portal>();

    PgServerThread(Socket socket, PgServer server) {
        this.server = server;
        this.socket = socket;
    }

    public void run() {
        try {
            server.trace("Connect");
            InputStream ins = socket.getInputStream();
            out = socket.getOutputStream();
            dataInRaw = new DataInputStream(ins);
            while (!stop) {
                process();
                out.flush();
            }
        } catch (EOFException e) {
            // more or less normal disconnect
        } catch (Exception e) {
            server.traceError(e);
        } finally {
            server.trace("Disconnect");
            close();
        }
    }

    private String readString() throws IOException {
        ByteArrayOutputStream buff = new ByteArrayOutputStream();
        while (true) {
            int x = dataIn.read();
            if (x <= 0) {
                break;
            }
            buff.write(x);
        }
        return new String(buff.toByteArray(), getEncoding());
    }

    private int readInt() throws IOException {
        return dataIn.readInt();
    }

    private int readShort() throws IOException {
        return dataIn.readShort();
    }

    private byte readByte() throws IOException {
        return dataIn.readByte();
    }

    private void readFully(byte[] buff) throws IOException {
        dataIn.readFully(buff);
    }

    private void process() throws IOException {
        int x;
        if (initDone) {
            x = dataInRaw.read();
            if (x < 0) {
                stop = true;
                return;
            }
        } else {
            x = 0;
        }
        int len = dataInRaw.readInt();
        len -= 4;
        byte[] data = Utils.newBytes(len);
        dataInRaw.readFully(data, 0, len);
        dataIn = new DataInputStream(new ByteArrayInputStream(data, 0, len));
        switchBlock: switch (x) {
        case 0:
            server.trace("Init");
            int version = readInt();
            if (version == 80877102) {
                server.trace("CancelRequest (not supported)");
                server.trace(" pid: " + readInt());
                server.trace(" key: " + readInt());
            } else if (version == 80877103) {
                server.trace("SSLRequest");
                out.write('N');
            } else {
                server.trace("StartupMessage");
                server.trace(" version " + version + " (" + (version >> 16) + "." + (version & 0xff) + ")");
                while (true) {
                    String param = readString();
                    if (param.length() == 0) {
                        break;
                    }
                    String value = readString();
                    if ("user".equals(param)) {
                        this.userName = value;
                    } else if ("database".equals(param)) {
                        this.databaseName = value;
                    } else if ("client_encoding".equals(param)) {
                        // UTF8
                        clientEncoding = value;
                    } else if ("DateStyle".equals(param)) {
                        dateStyle = value;
                    }
                    // extra_float_digits 2
                    // geqo on (Genetic Query Optimization)
                    server.trace(" param " + param + "=" + value);
                }
                sendAuthenticationCleartextPassword();
                initDone = true;
            }
            break;
        case 'p': {
            server.trace("PasswordMessage");
            String password = readString();
            try {
                Properties info = new Properties();
                info.put("MODE", "PostgreSQL");
                info.put("USER", userName);
                info.put("PASSWORD", password);
                String url = "jdbc:h2:" + databaseName;
                ConnectionInfo ci = new ConnectionInfo(url, info);
                String baseDir = server.getBaseDir();
                if (baseDir == null) {
                    baseDir = SysProperties.getBaseDir();
                }
                if (baseDir != null) {
                    ci.setBaseDir(baseDir);
                }
                if (server.getIfExists()) {
                    ci.setProperty("IFEXISTS", "TRUE");
                }
                conn = new JdbcConnection(ci, false);
                // can not do this because when called inside
                // DriverManager.getConnection, a deadlock occurs
                // conn = DriverManager.getConnection(url, userName, password);
                initDb();
                sendAuthenticationOk();
            } catch (Exception e) {
                e.printStackTrace();
                stop = true;
            }
            break;
        }
        case 'P': {
            server.trace("Parse");
            Prepared p = new Prepared();
            p.name = readString();
            p.sql = getSQL(readString());
            int count = readShort();
            p.paramType = new int[count];
            for (int i = 0; i < count; i++) {
                int type = readInt();
                server.checkType(type);
                p.paramType[i] = type;
            }
            try {
                p.prep = (JdbcPreparedStatement) conn.prepareStatement(p.sql);
                prepared.put(p.name, p);
                sendParseComplete();
            } catch (Exception e) {
                sendErrorResponse(e);
            }
            break;
        }
        case 'B': {
            server.trace("Bind");
            Portal portal = new Portal();
            portal.name = readString();
            String prepName = readString();
            Prepared prep = prepared.get(prepName);
            if (prep == null) {
                sendErrorResponse("Prepared not found");
                break;
            }
            portal.prep = prep;
            portals.put(portal.name, portal);
            int formatCodeCount = readShort();
            int[] formatCodes = new int[formatCodeCount];
            for (int i = 0; i < formatCodeCount; i++) {
                formatCodes[i] = readShort();
            }
            int paramCount = readShort();
            for (int i = 0; i < paramCount; i++) {
                int paramLen = readInt();
                byte[] d2 = Utils.newBytes(paramLen);
                readFully(d2);
                try {
                    setParameter(prep.prep, i, d2, formatCodes);
                } catch (Exception e) {
                    sendErrorResponse(e);
                    break switchBlock;
                }
            }
            int resultCodeCount = readShort();
            portal.resultColumnFormat = new int[resultCodeCount];
            for (int i = 0; i < resultCodeCount; i++) {
                portal.resultColumnFormat[i] = readShort();
            }
            sendBindComplete();
            break;
        }
        case 'C': {
            char type = (char) readByte();
            String name = readString();
            server.trace("Close");
            if (type == 'S') {
                Prepared p = prepared.remove(name);
                if (p != null) {
                    JdbcUtils.closeSilently(p.prep);
                }
            } else if (type == 'P') {
                portals.remove(name);
            } else {
                server.trace("expected S or P, got " + type);
                sendErrorResponse("expected S or P");
                break;
            }
            sendCloseComplete();
            break;
        }
        case 'D': {
            char type = (char) readByte();
            String name = readString();
            server.trace("Describe");
            if (type == 'S') {
                Prepared p = prepared.get(name);
                if (p == null) {
                    sendErrorResponse("Prepared not found: " + name);
                } else {
                    sendParameterDescription(p);
                }
            } else if (type == 'P') {
                Portal p = portals.get(name);
                if (p == null) {
                    sendErrorResponse("Portal not found: " + name);
                } else {
                    PreparedStatement prep = p.prep.prep;
                    try {
                        ResultSetMetaData meta = prep.getMetaData();
                        sendRowDescription(meta);
                    } catch (Exception e) {
                        sendErrorResponse(e);
                    }
                }
            } else {
                server.trace("expected S or P, got " + type);
                sendErrorResponse("expected S or P");
            }
            break;
        }
        case 'E': {
            String name = readString();
            server.trace("Execute");
            Portal p = portals.get(name);
            if (p == null) {
                sendErrorResponse("Portal not found: " + name);
                break;
            }
            int maxRows = readShort();
            Prepared prepared = p.prep;
            JdbcPreparedStatement prep = prepared.prep;
            server.trace(prepared.sql);
            try {
                prep.setMaxRows(maxRows);
                boolean result = prep.execute();
                if (result) {
                    try {
                        ResultSet rs = prep.getResultSet();
                        ResultSetMetaData meta = rs.getMetaData();
                        sendRowDescription(meta);
                        while (rs.next()) {
                            sendDataRow(rs);
                        }
                        sendCommandComplete(prep, 0);
                    } catch (Exception e) {
                        sendErrorResponse(e);
                    }
                } else {
                    sendCommandComplete(prep, prep.getUpdateCount());
                }
            } catch (Exception e) {
                sendErrorResponse(e);
            }
            break;
        }
        case 'S': {
            server.trace("Sync");
            sendReadyForQuery();
            break;
        }
        case 'Q': {
            server.trace("Query");
            String query = readString();
            ScriptReader reader = new ScriptReader(new StringReader(query));
            while (true) {
                JdbcStatement stat = null;
                try {
                    String s = reader.readStatement();
                    if (s == null) {
                        break;
                    }
                    s = getSQL(s);
                    stat = (JdbcStatement) conn.createStatement();
                    boolean result = stat.execute(s);
                    if (result) {
                        ResultSet rs = stat.getResultSet();
                        ResultSetMetaData meta = rs.getMetaData();
                        try {
                            sendRowDescription(meta);
                            while (rs.next()) {
                                sendDataRow(rs);
                            }
                            sendCommandComplete(stat, 0);
                        } catch (Exception e) {
                            sendErrorResponse(e);
                            break;
                        }
                    } else {
                        sendCommandComplete(stat, stat.getUpdateCount());
                    }
                } catch (SQLException e) {
                    sendErrorResponse(e);
                    break;
                } finally {
                    JdbcUtils.closeSilently(stat);
                }
            }
            sendReadyForQuery();
            break;
        }
        case 'X': {
            server.trace("Terminate");
            close();
            break;
        }
        default:
            server.trace("Unsupported: " + x + " (" + (char) x + ")");
            break;
        }
    }

    private String getSQL(String s) {
        String lower = StringUtils.toLowerEnglish(s);
        if (lower.startsWith("show max_identifier_length")) {
            s = "CALL 63";
        } else if (lower.startsWith("set client_encoding to")) {
            s = "set DATESTYLE ISO";
        }
        // s = StringUtils.replaceAll(s, "i.indkey[ia.attnum-1]", "0");
        if (server.getTrace()) {
            server.trace(s + ";");
        }
        return s;
    }

    private void sendCommandComplete(JdbcStatement stat, int updateCount) throws IOException {
        startMessage('C');
        switch (stat.getLastExecutedCommandType()) {
        case CommandInterface.INSERT:
            writeStringPart("INSERT 0 ");
            writeString(Integer.toString(updateCount));
            break;
        case CommandInterface.UPDATE:
            writeStringPart("UPDATE ");
            writeString(Integer.toString(updateCount));
            break;
        case CommandInterface.DELETE:
            writeStringPart("DELETE ");
            writeString(Integer.toString(updateCount));
            break;
        case CommandInterface.SELECT:
        case CommandInterface.CALL:
            writeString("SELECT");
            break;
        case CommandInterface.BEGIN:
            writeString("BEGIN");
            break;
        default:
            server.trace("check CommandComplete tag for command " + stat);
            writeStringPart("UPDATE ");
            writeString(Integer.toString(updateCount));
        }
        sendMessage();
    }

    private void sendDataRow(ResultSet rs) throws Exception {
        int columns = rs.getMetaData().getColumnCount();
        String[] values = new String[columns];
        for (int i = 0; i < columns; i++) {
            values[i] = rs.getString(i + 1);
        }
        startMessage('D');
        writeShort(columns);
        for (String s : values) {
            if (s == null) {
                writeInt(-1);
            } else {
                // TODO write Binary data
                byte[] d2 = s.getBytes(getEncoding());
                writeInt(d2.length);
                write(d2);
            }
        }
        sendMessage();
    }

    private String getEncoding() {
        if ("UNICODE".equals(clientEncoding)) {
            return "UTF-8";
        }
        return clientEncoding;
    }

    private void setParameter(PreparedStatement prep, int i, byte[] d2, int[] formatCodes) throws SQLException {
        boolean text = (i >= formatCodes.length) || (formatCodes[i] == 0);
        String s;
        try {
            if (text) {
                s = new String(d2, getEncoding());
            } else {
                server.trace("Binary format not supported");
                s = new String(d2, getEncoding());
            }
        } catch (Exception e) {
            server.traceError(e);
            s = null;
        }
        // if(server.getLog()) {
        // server.log(" " + i + ": " + s);
        // }
        prep.setString(i + 1, s);
    }

    private void sendErrorResponse(Exception re) throws IOException {
        SQLException e = DbException.toSQLException(re);
        server.traceError(e);
        startMessage('E');
        write('S');
        writeString("ERROR");
        write('C');
        writeString(e.getSQLState());
        write('M');
        writeString(e.getMessage());
        write('D');
        writeString(e.toString());
        write(0);
        sendMessage();
    }

    private void sendParameterDescription(Prepared p) throws IOException {
        try {
            PreparedStatement prep = p.prep;
            ParameterMetaData meta = prep.getParameterMetaData();
            int count = meta.getParameterCount();
            startMessage('t');
            writeShort(count);
            for (int i = 0; i < count; i++) {
                int type;
                if (p.paramType != null && p.paramType[i] != 0) {
                    type = p.paramType[i];
                } else {
                    type = PgServer.PG_TYPE_VARCHAR;
                }
                server.checkType(type);
                writeInt(type);
            }
            sendMessage();
        } catch (Exception e) {
            sendErrorResponse(e);
        }
    }

    private void sendNoData() throws IOException {
        startMessage('n');
        sendMessage();
    }

    private void sendRowDescription(ResultSetMetaData meta) throws Exception {
        if (meta == null) {
            sendNoData();
        } else {
            int columns = meta.getColumnCount();
            int[] types = new int[columns];
            int[] precision = new int[columns];
            String[] names = new String[columns];
            for (int i = 0; i < columns; i++) {
                String name = meta.getColumnName(i + 1);
                names[i] = name;
                int type = meta.getColumnType(i + 1);
                type = PgServer.convertType(type);
                // the ODBC client needs the column pg_catalog.pg_index
                // to be of type 'int2vector'
                // if (name.equalsIgnoreCase("indkey") &&
                //         "pg_index".equalsIgnoreCase(meta.getTableName(i + 1))) {
                //     type = PgServer.PG_TYPE_INT2VECTOR;
                // }
                precision[i] = meta.getColumnDisplaySize(i + 1);
                server.checkType(type);
                types[i] = type;
            }
            startMessage('T');
            writeShort(columns);
            for (int i = 0; i < columns; i++) {
                writeString(StringUtils.toLowerEnglish(names[i]));
                // object ID
                writeInt(0);
                // attribute number of the column
                writeShort(0);
                // data type
                writeInt(types[i]);
                // pg_type.typlen
                writeShort(getTypeSize(types[i], precision[i]));
                // pg_attribute.atttypmod
                writeInt(-1);
                // text
                writeShort(0);
            }
            sendMessage();
        }
    }

    private static int getTypeSize(int pgType, int precision) {
        switch (pgType) {
        case PgServer.PG_TYPE_VARCHAR:
            return Math.max(255, precision + 10);
        default:
            return precision + 4;
        }
    }

    private void sendErrorResponse(String message) throws IOException {
        server.trace("Exception: " + message);
        startMessage('E');
        write('S');
        writeString("ERROR");
        write('C');
        // PROTOCOL VIOLATION
        writeString("08P01");
        write('M');
        writeString(message);
        sendMessage();
    }

    private void sendParseComplete() throws IOException {
        startMessage('1');
        sendMessage();
    }

    private void sendBindComplete() throws IOException {
        startMessage('2');
        sendMessage();
    }

    private void sendCloseComplete() throws IOException {
        startMessage('3');
        sendMessage();
    }

    private void initDb() throws SQLException {
        Statement stat = null;
        ResultSet rs = null;
        try {
            synchronized (server) {
                // better would be: set the database to exclusive mode
                rs = conn.getMetaData().getTables(null, "PG_CATALOG", "PG_VERSION", null);
                boolean tableFound = rs.next();
                stat = conn.createStatement();
                if (!tableFound) {
                    installPgCatalog(stat);
                }
                rs = stat.executeQuery("SELECT * FROM PG_CATALOG.PG_VERSION");
                if (!rs.next() || rs.getInt(1) < 2) {
                    // installation incomplete, or old version
                    installPgCatalog(stat);
                } else {
                    // version 2 or newer: check the read version
                    int versionRead = rs.getInt(2);
                    if (versionRead > 2) {
                        throw DbException.throwInternalError("Incompatible PG_VERSION");
                    }
                }
            }
            stat.execute("set search_path = PUBLIC, pg_catalog");
            HashSet<Integer> typeSet = server.getTypeSet();
            if (typeSet.size() == 0) {
                rs = stat.executeQuery("SELECT OID FROM PG_CATALOG.PG_TYPE");
                while (rs.next()) {
                    typeSet.add(rs.getInt(1));
                }
            }
        } finally {
            JdbcUtils.closeSilently(stat);
            JdbcUtils.closeSilently(rs);
        }
    }

    private static void installPgCatalog(Statement stat) throws SQLException {
        Reader r = null;
        try {
            r = new InputStreamReader(new ByteArrayInputStream(Utils
                    .getResource("/org/h2/server/pg/pg_catalog.sql")));
            ScriptReader reader = new ScriptReader(r);
            while (true) {
                String sql = reader.readStatement();
                if (sql == null) {
                    break;
                }
                stat.execute(sql);
            }
            reader.close();
        } catch (IOException e) {
            throw DbException.convertIOException(e, "Can not read pg_catalog resource");
        } finally {
            IOUtils.closeSilently(r);
        }
    }

    /**
     * Close this connection.
     */
    void close() {
        try {
            stop = true;
            JdbcUtils.closeSilently(conn);
            if (socket != null) {
                socket.close();
            }
            server.trace("Close");
        } catch (Exception e) {
            server.traceError(e);
        }
        conn = null;
        socket = null;
        server.remove(this);
    }

    private void sendAuthenticationCleartextPassword() throws IOException {
        startMessage('R');
        writeInt(3);
        sendMessage();
    }

    private void sendAuthenticationOk() throws IOException {
        startMessage('R');
        writeInt(0);
        sendMessage();
        sendParameterStatus("client_encoding", clientEncoding);
        sendParameterStatus("DateStyle", dateStyle);
        sendParameterStatus("integer_datetimes", "off");
        sendParameterStatus("is_superuser", "off");
        sendParameterStatus("server_encoding", "SQL_ASCII");
        sendParameterStatus("server_version", "8.1.4");
        sendParameterStatus("session_authorization", userName);
        sendParameterStatus("standard_conforming_strings", "off");
        // TODO PostgreSQL TimeZone
        sendParameterStatus("TimeZone", "CET");
        sendBackendKeyData();
        sendReadyForQuery();
    }

    private void sendReadyForQuery() throws IOException {
        startMessage('Z');
        char c;
        try {
            if (conn.getAutoCommit()) {
                // idle
                c = 'I';
            } else {
                // in a transaction block
                c = 'T';
            }
        } catch (SQLException e) {
            // failed transaction block
            c = 'E';
        }
        write((byte) c);
        sendMessage();
    }

    private void sendBackendKeyData() throws IOException {
        startMessage('K');
        writeInt(processId);
        writeInt(processId);
        sendMessage();
    }

    private void writeString(String s) throws IOException {
        writeStringPart(s);
        write(0);
    }

    private void writeStringPart(String s) throws IOException {
        write(s.getBytes(getEncoding()));
    }

    private void writeInt(int i) throws IOException {
        dataOut.writeInt(i);
    }

    private void writeShort(int i) throws IOException {
        dataOut.writeShort(i);
    }

    private void write(byte[] data) throws IOException {
        dataOut.write(data);
    }

    private void write(int b) throws IOException {
        dataOut.write(b);
    }

    private void startMessage(int newMessageType) {
        this.messageType = newMessageType;
        outBuffer = new ByteArrayOutputStream();
        dataOut = new DataOutputStream(outBuffer);
    }

    private void sendMessage() throws IOException {
        dataOut.flush();
        byte[] buff = outBuffer.toByteArray();
        int len = buff.length;
        dataOut = new DataOutputStream(out);
        dataOut.write(messageType);
        dataOut.writeInt(len + 4);
        dataOut.write(buff);
        dataOut.flush();
    }

    private void sendParameterStatus(String param, String value) throws IOException {
        startMessage('S');
        writeString(param);
        writeString(value);
        sendMessage();
    }

    void setThread(Thread thread) {
        this.thread = thread;
    }

    Thread getThread() {
        return thread;
    }

    void setProcessId(int id) {
        this.processId = id;
    }

    /**
     * Represents a PostgreSQL Prepared object.
     */
    static class Prepared {

        /**
         * The object name.
         */
        String name;

        /**
         * The SQL statement.
         */
        String sql;

        /**
         * The prepared statement.
         */
        JdbcPreparedStatement prep;

        /**
         * The list of parameter types (if set).
         */
        int[] paramType;
    }

    /**
     * Represents a PostgreSQL Portal object.
     */
    static class Portal {

        /**
         * The portal name.
         */
        String name;

        /**
         * The format used in the result set columns (if set).
         */
        int[] resultColumnFormat;

        /**
         * The prepared object.
         */
        Prepared prep;
    }

}
