package edu.gemini.pot.sp;

/**
 * Utility code useful for clients and implementors.
 */
public final class SPUtil {

// Don't allow instances
    private SPUtil() {
    }

    private static String getPropName(String prefix, String key) {
        return prefix + ':' + key;
    }

    public static String getClientDataPropertyName(String clientDataKey) {
        return getPropName(ISPNode.CLIENT_DATA_PROP_PREFIX, clientDataKey);
    }

    public static String getTransientClientDataPropertyName(String clientDataKey) {
        return getPropName(ISPNode.TRANSIENT_CLIENT_DATA_PROP_PREFIX, clientDataKey);
    }

    private static final String TRANSIENT_PROP_NAME_PREFIX =
            getPropName(ISPNode.TRANSIENT_CLIENT_DATA_PROP_PREFIX, "");

    public static boolean isTransientClientDataPropertyName(String propertyName) {
        return (propertyName != null) && propertyName.startsWith(TRANSIENT_PROP_NAME_PREFIX);
    }

    public static String getDataObjectPropertyName() {
        return getClientDataPropertyName(ISPNode.DATA_OBJECT_KEY);
    }

}
