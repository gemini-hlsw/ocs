package edu.gemini.pot.spdb;

import edu.gemini.pot.sp.*;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.*;

/**
 * Parses an initializer configuration (property) file and provides a
 * convenience routine to apply the initializers to an ISPFactory.  The
 * initializer configuration file describes which ISPNodeInitializer
 * subclasses should be used to initialize specific science program nodes.
 *
 * <p>The file contains one initializer class per line.  The key describes
 * the node and the value is the fully qualified class name of the object
 * that should be used to initialize that node.
 */
public final class SPNodeInitializerConfigReader {
    private static final Logger LOG = Logger.getLogger(SPNodeInitializerConfigReader.class.getName());

    private SPNodeInitializerConfigReader() {}

    /**
     * Separates the parts of the observation component key.  The keys
     * begin with "oc", and are then followed by the broad and narrow
     * types.  For example, "oc.telescope.targetEnv" specifies an
     * observation component with broad type "telescope" and narrow type
     * "targetEnv".
     */
    public static final String PATH_SEPARATOR = ".";

    /**
     * Key that specifies the Science Program root node.
     */
    public static final String PROGRAM_KEY = "sp";

    /**
     * Key that specifies the Nightly Plan root node.
     */
    public static final String NIGHTLY_PLAN_KEY = "np";

    /**
     * Key that specifies the Science Program observation node.
     */
    public static final String OBSERVATION_KEY = "obs";

    /**
     * Key that specifies the Science Program group node.
     */
    public static final String GROUP_KEY = "group";

    public static final String CONFLICT_FOLDER_KEY = "conflictFolder";
    public static final String TEMPLATE_FOLDER_KEY = "templateFolder";
    public static final String TEMPLATE_GROUP_KEY  = "templateGroup";
    public static final String TEMPLATE_PARAMETERS_KEY  = "templateParameters";

    public static final String OBS_QA_LOG_KEY   = String.format("obsLog%sqa",   PATH_SEPARATOR);
    public static final String OBS_EXEC_LOG_KEY = String.format("obsLog%sexec", PATH_SEPARATOR);


    /**
     * Key that specifies the Science Program observation component node.
     * Should be followed by the <code>PATH_SEPARATOR</code> and the
     * specific component type.  For example <code>oc.targetEnv</code>.
     */
    public static final String OBS_COMPONENT_KEY = "oc";

    /**
     * Key that specifies the Science Program sequence component node.
     * Should be followed by the <code>PATH_SEPARATOR</code> and the
     * specific component type.  For example <code>sc.targetEnv</code>.
     */
    public static final String SEQ_COMPONENT_KEY = "sc";

    /**
     * Key that specifies the Nightly Plan plan node.
     * Should be followed by the <code>PATH_SEPARATOR</code> and the
     * specific component type.  For example <code>pl.basicPlan</code>
     */
    public static final String PLAN_KEY = "pl";

    /**
     * Key that specifies the Nightly Plan observing log node.
     * Should be followed by the <code>PATH_SEPARATOR</code> and the
     * specific component type.  For example <code>ol.basicObservingLog</code>
     */
    // Create a SPComponentType for a key
    private static SPComponentType _keyToType(String key) {
        String subtypes = key.substring(3);

        SPComponentType type = null;

        int index = subtypes.indexOf('.');
        if ((index > 0) && (index + 1 < subtypes.length())) {
            String broadType = subtypes.substring(0, index);
            String narrowType = subtypes.substring(index + 1);

            type = SPComponentType.getInstance(SPComponentBroadType.getInstance(broadType), narrowType);
        }
        return type;
    }

    /**
     * Apply the initializers to the given factory.
     * When the factory subsequently creates a node,
     * it will use the matching initializer to initialize it.
     */
    public static void applyConfig(ISPFactory fact, Map<String, ISPNodeInitializer> initializerMap)  {
        //String ocPrefix = OBS_COMPONENT_KEY + PATH_SEPARATOR;
        //String scPrefix = SEQ_COMPONENT_KEY + PATH_SEPARATOR;

        for (Map.Entry<String, ISPNodeInitializer> me : initializerMap.entrySet()) {
            String key = me.getKey();

            // Ugly, but true.
            ISPNodeInitializer ni = me.getValue();
            if (_isComponentKey(key)) {
                SPComponentType type = _keyToType(key);
                if (type != null) {
                    if (_isObsComponentKey(key)) {
                        fact.registerObsComponentInit(type, ni);
                    } else {
                        fact.registerSeqComponentInit(type, ni);
                    }
                }
            } else if (key.equals(OBSERVATION_KEY)) {
                fact.registerObservationInit(ni);
            } else if (key.equals(GROUP_KEY)) {
                fact.registerGroupInit(ni);
            } else if (key.equals(CONFLICT_FOLDER_KEY)) {
                fact.registerConflictFolderInit(ni);
            } else if (key.equals(TEMPLATE_FOLDER_KEY)) {
                fact.registerTemplateFolderInit(ni);
            } else if (key.equals(TEMPLATE_GROUP_KEY)) {
                fact.registerTemplateGroupInit(ni);
            } else if (key.equals(TEMPLATE_PARAMETERS_KEY)) {
                fact.registerTemplateParametersInit(ni);
            } else if (key.equals(OBS_QA_LOG_KEY)) {
                fact.registerObsQaLogInit(ni);
            } else if (key.equals(OBS_EXEC_LOG_KEY)) {
                fact.registerObsExecLogInit(ni);
            } else if (key.equals(PROGRAM_KEY)) {
                fact.registerProgramInit(ni);
            } else if (key.equals(NIGHTLY_PLAN_KEY)) {
                fact.registerNightlyRecordInit(ni);
            }
        }
    }

    /**
     * Given the initializers to the given factory, return the List
     * of <code>{@link SPComponentType}</code> objects for the
     * <code>{@link ISPSeqComponent}</code> objects creatable by this.
     * factory.
     */
    public static List<SPComponentType> getCreatableSeqComponents(Set<String> typeKeys) {
        // List for the result
        List<SPComponentType> l = new ArrayList<>();

        for (String key : typeKeys) {
            if (_isSeqComponentKey(key)) {
                SPComponentType type = _keyToType(key);
                if (type != null) {
                    l.add(type);
                }
            }
        }
        return l;
    }

    /**
     * Given the initializers to the given factory, return the List
     * of <code>{@link SPComponentType}</code> objects for the
     * <code>{@link ISPObsComponent}</code> objects creatable by this.
     * factory.
     */
    public static List<SPComponentType> getCreatableObsComponents(Set<String> typeKeys) {
        // List for the result
        List<SPComponentType> l = new ArrayList<>();

        for (String key : typeKeys) {
            if (_isObsComponentKey(key)) {
                SPComponentType type = _keyToType(key);
                if (type != null) {
                    l.add(type);
                }
            }
        }
        return l;
    }

    // Is this an obs component
    private static boolean _isComponentKey(String key) {
        return _isSeqComponentKey(key) || _isObsComponentKey(key);
    }

    private static boolean _isSeqComponentKey(String key) {
        String scPrefix = SEQ_COMPONENT_KEY + PATH_SEPARATOR;
        return key.startsWith(scPrefix);
    }

    private static boolean _isObsComponentKey(String key) {
        String ocPrefix = OBS_COMPONENT_KEY + PATH_SEPARATOR;
        return key.startsWith(ocPrefix);
    }

    private static boolean _isPlanKey(String key) {
        String plPrefix = PLAN_KEY + PATH_SEPARATOR;
        return key.startsWith(plPrefix);
    }

    /**
     * Iterates over the given <code>Properties</code> instance, creating
     * initializer instances for each property.
     *
     * @return <code>Map</code>, one entry per property, keyed by property name,
     * whose values are <code>{@link ISPNodeInitializer}</code> instances.
     */
    public static Map<String, ISPNodeInitializer> loadInitializers(Map<String, String> props) {
        LOG.fine("SPNodeInitializerConfigReader - loading initializers");
        final Set<Map.Entry<String, String>> entrySet = props.entrySet();
        final Iterator<Map.Entry<String, String>> it = entrySet.iterator();

        final Map<String, ISPNodeInitializer> initMap = new HashMap<>();
        while (it.hasNext()) {
            final Map.Entry<String, String> me = it.next();
            final String key = me.getKey();

            //System.out.println("Loading initializer: " + key
            //                   + ":" + (String)me.getValue());
            final ISPNodeInitializer ni;
            final String className = me.getValue();
            try {
                final Class<?> c = Class.forName(className);
                ni = (ISPNodeInitializer) c.newInstance();
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "Problem loading initializer: " + className, ex);
                throw new RuntimeException("Propblem loading initializer", ex);
            }
            // Should be non-null here.
            initMap.put(key, ni);
        }
        LOG.fine("SPNodeInitializerConfigReader - loaded initializers");
        return Collections.unmodifiableMap(initMap);
    }

}

