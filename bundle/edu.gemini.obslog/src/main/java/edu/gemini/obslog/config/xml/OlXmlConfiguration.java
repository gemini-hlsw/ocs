package edu.gemini.obslog.config.xml;

import edu.gemini.obslog.config.OlConfigException;
import edu.gemini.obslog.config.model.OlBasicConfiguration;
import edu.gemini.obslog.config.model.OlBasicLogItem;
import edu.gemini.obslog.config.model.OlConfiguration;
import edu.gemini.obslog.config.model.OlModelException;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import java.net.URL;
import java.util.List;
import java.util.logging.Logger;

public final class OlXmlConfiguration {
    public static final Logger LOG = Logger.getLogger(OlXmlConfiguration.class.getName());

    private static String _CONFIG_FILE;

    public OlXmlConfiguration(String configFile) throws NullPointerException {
        if (configFile == null) throw new NullPointerException();
        _CONFIG_FILE = configFile;
    }

    public OlConfiguration getModel() throws OlConfigException, OlModelException {
        URL url = getClass().getResource(_CONFIG_FILE);
        if (url == null) {
            String m = "Configuration file not found: " + _CONFIG_FILE;
            LOG.severe(m);
            throw new OlConfigException(m);
        }

        Document d = _parse(url);
        if (d == null) {
            String message = "Configuration file failed to parse.";
            LOG.severe(message);
            throw new OlConfigException(message);
        }
        return _buildModel(d);
    }

    /**
     * Build a configuration from the XML file.
     */
    @SuppressWarnings("unchecked")
    private OlConfiguration _buildModel(Document d) throws OlModelException {
        Node versionNode = d.selectSingleNode("//obslog/@version");
        if (versionNode == null) throw new NullPointerException("No obslog version attribute.");

        // First get the version
        String version = versionNode.getText();
        OlConfiguration config = new OlBasicConfiguration(version);

        // Get the entryGroups and foreach add the entries
        List<Element> list = d.selectNodes("/configuration/entry");
        if (list == null) throw new NullPointerException();

        for (Element node : list) {
            String entryKey = node.attribute("key").getStringValue();
            LOG.fine("EntryKey: " + entryKey);
            String isVisible = node.attributeValue("visible");

            OlBasicLogItem logItem = (OlBasicLogItem) config.addLogItem(entryKey);
            String heading = node.elementText("heading");
            logItem.setColumnHeading(heading);
            String itemName = node.elementText("property");
            logItem.setProperty(itemName);
            String sequenceName = node.elementText("sequenceName");
            logItem.setSequenceName(sequenceName);
            // Set to true unless present and false
            logItem.setVisible(isVisible == null || Boolean.getBoolean(isVisible));
        }

        // Now add the instruments
        List<Element> logEntries = d.selectNodes("//logEntry");
        for (Element node : logEntries) {
            String logKey = node.attribute("key").getStringValue();
            LOG.fine("logEntry key: " + logKey);


            List<Element> glist = node.selectNodes("entry");
            for (Element gnode: glist) {
                String entryKey = gnode.attribute("key").getStringValue();
                LOG.fine("Entry name: " + entryKey);

                config.addItemToObsLog(logKey, entryKey);

            }
        }
        return config;
    }

    private Document _parse(URL url) {
        SAXReader reader = new SAXReader();
        Document document = null;
        try {
            document = reader.read(url);
        } catch (DocumentException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return document;
    }

}

