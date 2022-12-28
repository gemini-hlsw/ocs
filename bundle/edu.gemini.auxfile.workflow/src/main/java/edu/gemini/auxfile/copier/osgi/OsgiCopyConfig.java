package edu.gemini.auxfile.copier.osgi;

import edu.gemini.auxfile.copier.AuxFileType;
import edu.gemini.auxfile.copier.CopyConfig;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.util.ssh.SshConfig$;
import org.osgi.framework.BundleContext;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


final class OsgiCopyConfig extends CopyConfig {
    private static final Logger LOG = Logger.getLogger(OsgiCopyConfig.class.getName());
    private static final String CONFIG_PREFIX = "edu.gemini.auxfile";
    private static final String HOST_KEY      = "host";
    private static final String USER_KEY      = "user";
    private static final String PASSWORD_KEY  = "password";
    private static final String DEST_KEY      = "dest";
    private static final String ENABLED_KEY   = "enabled";

    private final AuxFileType _fileType;
    private final String _destTmpl;


    public static OsgiCopyConfig create(AuxFileType type, BundleContext ctx){
        try {
            return new OsgiCopyConfig(type, ctx);
        } catch (RuntimeException ex) {
            LOG.log(Level.WARNING, "Configuration not found", ex);
        }
        return null;
    }

    private OsgiCopyConfig(AuxFileType type, BundleContext ctx) {
        // Get the values for host, user, and password from the bundle's context.
        // The timeout property comes from the companion object for the Scala SshConfig trait, hence the weird syntax.
	    super(getProperty(type, ctx, HOST_KEY),
	          getProperty(type, ctx, USER_KEY),
	          getProperty(type, ctx, PASSWORD_KEY),
              SshConfig$.MODULE$.DEFAULT_TIMEOUT(),
              ImOption.apply(ctx.getProperty(buildPropertyName(type, ENABLED_KEY))).forall("true"::equals)
              );
        _fileType = type;
        _destTmpl = getProperty(type, ctx, DEST_KEY);
    }

    private static String getProperty(final AuxFileType type, final BundleContext ctx, final String key) {

        final String cfgProp = buildPropertyName(type, key);
        final String defProp = buildPropertyName(AuxFileType.other, key);
        final String cfg = ctx.getProperty(cfgProp);
        final String def = ctx.getProperty(defProp);

        if (cfg != null) {
            // this aux file type has a specific config, use it
            LOG.info("config for " + cfgProp + ": " + cfg);
            return cfg;

        } else if (def != null) {
            // if this aux file type does not have a specific configuration we
            // use the configuration for file type "other" as a default instead
            LOG.info("using default config for " + cfgProp + ": " + def);
            return def;

        } else {
            // no config found!
            throw new RuntimeException("Missing configuration: " + cfgProp);
        }
    }

    private static String buildPropertyName(final AuxFileType type, final String key) {
        final StringBuilder buf = new StringBuilder();
        buf.append(CONFIG_PREFIX).append('.');
        buf.append(type.name()).append('.');
        buf.append(key);
        return buf.toString();
    }

    public AuxFileType getFileType() {
        return _fileType;
    }

    // Patterns to decompose information from the program ID.
    private static final Pattern PROG_PATTERN = Pattern.compile("G[NS]-(\\d\\d\\d\\d[AB]).*");
    private static final Pattern SEMESTER_KEY_PATTERN = Pattern.compile("@SEMESTER@");
    private static final Pattern PROG_ID_KEY_PATTERN  = Pattern.compile("@PROG_ID@");

    public String getDestDir(SPProgramID progId) {
        Matcher m = PROG_PATTERN.matcher(progId.toString());
        if (!m.matches()) return null;

        String semester = m.group(1);

        m = SEMESTER_KEY_PATTERN.matcher(_destTmpl);
        String res = m.replaceAll(semester);

        m = PROG_ID_KEY_PATTERN.matcher(res);
        return m.replaceAll(progId.toString());
    }
}
