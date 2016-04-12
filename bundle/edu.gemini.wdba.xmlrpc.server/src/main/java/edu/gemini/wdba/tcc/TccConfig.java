//
// $Id: TccConfig.java 874 2007-06-04 17:06:28Z gillies $
//
package edu.gemini.wdba.tcc;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.ext.ObservationNode;
import edu.gemini.wdba.glue.api.WdbaGlueException;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Model for the tcc_tcs_config_file XML document.
 * Since each new use creates a new object, and the remote database is synchronized, this
 * should not be a problem.
 * <p/>
 * <p><b>Note that this implementation is not synchronized.</b>
 */
public final class TccConfig {
    private static final Logger LOG = Logger.getLogger(TccConfig.class.getName());

    private final ArrayList<ParamSet> _paramSets;
    private final Site _site;

    /**
     * Default constructor - constructs an empty paramset.
     */
    public TccConfig(Site site) {
        _paramSets = new ArrayList<>();
        _site = site;
    }

    /**
     * build will use the <code>(@link TargetEnv}</code> to construct
     * an XML document.
     */
    public void build(ObservationNode obsNode, Map<SPComponentType, String> supportMap) throws WdbaGlueException {
        // Create the observation environment
        ObservationEnvironment oe = new ObservationEnvironment(obsNode, supportMap, _site);

        TccFieldConfig fieldConfig = new TccFieldConfig(oe);
        if (fieldConfig.build()) {
            _paramSets.add(fieldConfig);
        }

        AOConfig aoConfig = new AOConfig(oe);
        if (aoConfig.build()) {
            // This hack tests to see if its "No AO", because if it is, then TCC has the config and we just send the name.
            if (!aoConfig.getConfigName().equals(TccNames.NO_AO)) {
                _paramSets.add(aoConfig);
            }
        }

        TcsConfig tcsConfig = new TcsConfig(oe);
        if (tcsConfig.build()) {
            _paramSets.add(tcsConfig);
            tcsConfig.putParameter(TccNames.GAOS, aoConfig.getConfigName());
        }

    }

    /**
     * Return the entire document as a <code>String</code>.
     */
    public String configToString() throws WdbaGlueException {
        String result = null;
        try {
            Document doc = DocumentHelper.createDocument();

            // Create the wrapper
            Element e = DocumentHelper.createElement(TccNames.ROOT);
            doc.setRootElement(e);

            // First add the field
            _paramSets.forEach(ps -> e.add(ps));

            OutputFormat format = new OutputFormat("  ", true);

            StringWriter strw = new StringWriter();
            XMLWriter writer = new XMLWriter(strw, format);
            writer.write(doc);
            result = strw.toString();
        } catch (IOException ex) {
            _logAbort("Failed while outputting to XML", ex);
        }
        return result;
    }


    // private method to log and throw and exception
    private void _logAbort(String message, Exception ex) throws WdbaGlueException {
        LOG.severe(message);
        throw new WdbaGlueException(message, (ex != null) ? ex : null);
    }

}

