package edu.gemini.spModel.guide;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A map of all the guiders in the model, indexed by their String key.  They
 * are stored in the file "/resources/conf/guiders.xml", which must be updated
 * when a new guider is added to the model.
 */
public final class GuideProbeMap {
    public static final GuideProbeMap instance = new GuideProbeMap();

    private static final Logger LOG = Logger.getLogger(GuideProbeMap.class.getName());

    private static final String RESOURCE = "/resources/conf/guiders.xml";

    private static final String PROBE_TAG = "probe";

    private final Map<String, GuideProbe> guideProbeMap;

    private GuideProbeMap() {
        guideProbeMap = Collections.unmodifiableMap(parseDocument());
    }

    public GuideProbe get(String key) {
        return guideProbeMap.get(key);
    }

    public boolean containsKey(String key) {
        return guideProbeMap.containsKey(key);
    }

    public boolean containsValue(GuideProbe probe) {
        return guideProbeMap.containsValue(probe);
    }

    public Set<Map.Entry<String, GuideProbe>> entrySet() {
        return guideProbeMap.entrySet();
    }

    public boolean isEmpty() {
        return guideProbeMap.isEmpty();
    }

    public Set<String> keySet() {
        return guideProbeMap.keySet();
    }

    public int size() {
        return guideProbeMap.size();
    }

    public Collection<GuideProbe> values() {
        return guideProbeMap.values();
    }

    private static Map<String, GuideProbe> parseDocument() {
        try {
            URL url = getResourceUrl();
            Document doc = getGuideProbeDoc(url);
            return parseDocument(doc);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Could not read " + RESOURCE + " file.", ex);
            throw new RuntimeException(ex);
        }
    }

    private static URL getResourceUrl() {
        return GuideProbeMap.class.getResource(RESOURCE);
    }

    private static Document getGuideProbeDoc(URL url) {
        Document doc;
        try {
            doc = (new SAXReader()).read(new InputStreamReader(url.openStream(), "UTF-8"));
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Could not read " + RESOURCE + " file.", ex);
            throw new RuntimeException(ex);
        }

        return doc;
    }

    private static Map<String, GuideProbe> parseDocument(Document doc) throws ClassNotFoundException {
        Element root = doc.getRootElement();
        @SuppressWarnings({"unchecked"})
        List<Element> probeElements  = root.elements(PROBE_TAG);
        if (probeElements == null) return Collections.emptyMap();

        Map<String, GuideProbe> res = new HashMap<String, GuideProbe>();

        for (Element probElement : probeElements) {
            for (GuideProbe probe : parseProbe(probElement)) {
                res.put(probe.getKey(), probe);
            }
        }
        return res;
    }

    private static GuideProbe[] parseProbe(Element probeElement) throws ClassNotFoundException {
        String className = probeElement.getTextTrim();
        Class c = Class.forName(className, true, GuideProbeMap.class.getClassLoader());
        return (GuideProbe[]) c.getEnumConstants();
    }
}
