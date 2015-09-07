// Copyright 1997-2001
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: SequenceOutputService.java 46768 2012-07-16 18:58:53Z rnorris $
//
package edu.gemini.spModel.io;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.config.ConfigBridge;
import edu.gemini.spModel.config.ConfigGraph;
import edu.gemini.spModel.config.map.ConfigValMapInstances;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ConfigSequence;
import edu.gemini.spModel.config2.ItemKey;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Write a Science Program sequence to a given OutputStream in  XML format.
 * See the DTD Seq.dtd for the grammar that describes the possible outputs of
 * this class.
 *
 * @author Allan Brighton (Based on class SPOutputXML, by Kim Gillies)
 * Updated to provide an option to run in a functor by Shane.
 */
public enum SequenceOutputService {
    instance;

    private static final Logger LOG = Logger.getLogger(SequenceOutputService.class.getName());

    /** The default public id for the DTD. */
    public static String DEFAULT_PUBLIC_ID = "-//Gemini Observatory//DTD for OT Science Program Sequence//EN";

    public static String DEFAULT_SYSTEM_ID = "Seq.dtd";

    /** The version of the DTD used to write this document.*/
    public static final String SEQ_DTD_VERSION = "0.1";


    // Tags
    public static final String ROOT_ELEMENT_TAG = "sequence";
    public static final String STEP_ELEMENT_TAG = "step";
    public static final String SYSTEM_ELEMENT_TAG = "system";
    public static final String PARAM_ELEMENT_TAG = "param";
    public static final String PARAMSET_ELEMENT_TAG = "paramset";

    public static final String VERSION_ATTRIBUTE_TAG = "version";
    public static final String NAME_ATTRIBUTE_TAG = "name";
    public static final String VALUE_ATTRIBUTE_TAG = "value";

    /**
     *  Add the sequence elements to the given parent element.
     *
     */
    private void _buildSequence(Element parent, ISPObservation obs)  {
        ConfigSequence cs = ConfigBridge.extractSequence(obs, null, ConfigValMapInstances.TO_SEQUENCE_VALUE, true);

        Config[] configs = cs.getCompactView();
        for (int i=0; i<configs.length; ++i) {
            Config c = configs[i];
            renameTargetBase(c);
            parent.add(toStepElement(c, i+1));
        }
    }

    // Another abomination. For reasons that I don't understand,
    // telescope:Base:name has to be called telescope:position(Base):name when
    // sent to the seqexec.
    private static final ItemKey REAL_BASE_KEY = new ItemKey("telescope:Base:name");
    private static final ItemKey SEXQ_BASE_KEY = new ItemKey("telescope:position(Base):name");
    private void renameTargetBase(Config c) {
        Object val = c.remove(REAL_BASE_KEY);
        if (val != null) c.putItem(SEXQ_BASE_KEY, val);
    }

    private static final ConfigGraph.Builder<Element> DOC_BUILDER = new ConfigGraph.Builder<Element>() {
        @Override public Element container(ItemKey path, ImList<Element> children) {
            Element e;
            if (path.size() == 1) {
                e = DocumentHelper.createElement(SYSTEM_ELEMENT_TAG);
            } else {
                e = DocumentHelper.createElement(PARAMSET_ELEMENT_TAG);
            }
            e.addAttribute(NAME_ATTRIBUTE_TAG, path.getName());

            for (Element child : children) e.add(child);
            return e;
        }

        @Override public Element leaf(ItemKey path, Object value) {
            Element e = DocumentHelper.createElement(PARAM_ELEMENT_TAG);
            e.addAttribute(NAME_ATTRIBUTE_TAG, path.getName());
            e.addAttribute(VALUE_ATTRIBUTE_TAG, value.toString());
            return e;
        }
    };

    private Element toStepElement(Config config, int stepCount) {
        final Element e = DocumentHelper.createElement(STEP_ELEMENT_TAG);
        e.addAttribute(NAME_ATTRIBUTE_TAG, "step " + stepCount);

        for (Element child : ConfigGraph.instance.graph(config, DOC_BUILDER)) {
            e.add(child);
        }
        return e;
    }


    /**
     * Write a program sequence document allowing the caller to set whether or
     * not a doc type will be set.
     *
     * @param verify if true, the document will have a DOCTYPE
     *
     * @return true if successful
     */
    public Document toSequenceXml(ISPObservation obs, boolean verify)  {
        Element rootElement = DocumentHelper.createElement(ROOT_ELEMENT_TAG);
        rootElement.addAttribute(VERSION_ATTRIBUTE_TAG, SEQ_DTD_VERSION);

        Document doc = DocumentHelper.createDocument(rootElement);

        _buildSequence(rootElement, obs);

        if (verify) {
            // Add a doctype
            doc.addDocType(ROOT_ELEMENT_TAG, DEFAULT_PUBLIC_ID, DEFAULT_SYSTEM_ID);
        }
        return doc;
    }

    public static boolean printSequence(Writer writer, ISPObservation obs, boolean verify) {
        Document doc = instance.toSequenceXml(obs, verify);

        // Now use the JDom XMLOutputter to output the document to the stream
        OutputFormat format = new OutputFormat("  ", true);
        try {
            XMLWriter outer = new XMLWriter(writer, format);
            outer.write(doc);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            return false;
        }
        return true;

    }
}

