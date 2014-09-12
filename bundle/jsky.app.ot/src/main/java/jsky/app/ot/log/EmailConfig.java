//
// $
//

package jsky.app.ot.log;

import edu.gemini.spModel.core.Version;

import java.net.InetAddress;

public enum EmailConfig {
    instance;

    public String smtp() { return "smtp.gemini.edu";    }
    public String from() { return "noreply@gemini.edu"; }
    public String to()   { return "otbugs@gemini.edu"; }

    public boolean shouldInstall() {
        return Version.current.isTest();
    }

    public String getSubject(Throwable t) {
        StringBuilder buf = new StringBuilder();

        buf.append("OT Bug: ");
        buf.append(Version.current);
        if (t != null) {
            buf.append(" ");
            buf.append(t.getMessage());
        }

        return buf.toString();
    }

    private String ctx = null;

    public String context() {
        if (ctx == null) ctx = computeContext();
        return ctx;
    }

    private static String p(String name) {
        String val = System.getProperty(name);
        return val == null ? "" : val;
    }

    private static void appendVals(StringBuilder buf, String key, String... vals) {
        buf.append(key).append(":\t");
        for (String val : vals) {
            if (!"".equals(val)) buf.append(val).append(' ');
        }
        buf.append('\n');
    }

    private static void appendProps(StringBuilder buf, String key, String... props) {
        int i = 0;
        String[] vals = new String[props.length];
        for (String prop : props) vals[i++] = p(prop);
        appendVals(buf, key, vals);
    }

    public static String computeContext() {
        StringBuilder buf = new StringBuilder();

        String hostName = "";
        String hostAddr = "";
        try {
            InetAddress addr = InetAddress.getLocalHost();
            hostName = addr.getHostName();
            hostAddr = addr.getHostAddress();
        } catch (Exception ex) {
            // ignore
        }

        appendVals(buf, "Vers", Version.current.toString());
        appendVals(buf, "Host", hostAddr, hostName);
        appendProps(buf, "OSys", "os.name", "os.arch", "os.version");
        appendProps(buf, "Java", "java.version");
        appendProps(buf, "User", "user.name");
        appendProps(buf, "CWD", "user.dir");

        return buf.toString();
    }
}
