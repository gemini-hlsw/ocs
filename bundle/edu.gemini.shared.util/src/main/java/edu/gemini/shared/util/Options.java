
package edu.gemini.shared.util;

import java.util.*;

/**
 * Representation of command line options which simplifies parsing and
 * processing.
 */
public class Options {

    /**
     * A command line option (immutable).
     */
    public static class OptionDef {

        private String flag;

        private String defaultValue;

        public OptionDef(String flag, String defaultValue) {
            this.flag = flag;
            this.defaultValue = defaultValue;
        }

        public OptionDef(OptionDef that) {
            this.flag = that.flag;
            this.defaultValue = that.defaultValue;
        }

        public String getFlag() {
            return flag;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public String toString() {
            StringBuilder buf = new StringBuilder();
            buf.append(this.getClass().getName());
            buf.append(" [");
            buf.append("flag=").append(flag).append(", ");
            buf.append("defaultValue=").append(defaultValue);
            buf.append("]");
            return buf.toString();
        }
    }

    public static OptionDef[] merge(OptionDef[] defaults, OptionDef[] others) {
        if (defaults == null)
            defaults = new OptionDef[0];
        if (others == null)
            others = new OptionDef[0];
        OptionDef[] res = new OptionDef[defaults.length + others.length];
        System.arraycopy(defaults, 0, res, 0, defaults.length);
        System.arraycopy(others, 0, res, defaults.length, others.length);
        return res;
    }

    public static final String[] EMPTY_ARGS = new String[0];

    private Map<String, OptionDef> optionDefMap = new HashMap<String, OptionDef>();

    private Map<String, String> valueMap = new HashMap<String, String>();

    private String[] commandLineArgs = EMPTY_ARGS;

    public Options() {
        this((OptionDef[]) null);
    }

    public Options(OptionDef[] defs) {
        for (OptionDef def : defs) {
            optionDefMap.put(def.getFlag(), def);
        }
    }

    /**
     * Copy constructor.
     */
    public Options(Options that) {
        optionDefMap.putAll(that.optionDefMap);
        valueMap.putAll(that.valueMap);
        int len = that.commandLineArgs.length;
        commandLineArgs = new String[len];
        System.arraycopy(that.commandLineArgs, 0, commandLineArgs, 0, len);
    }

    /**
     * (Re)defines all the option settings and their values.  Any previoiusly
     * set values are forgotten.
     */
    public void parse(String[] args) {
        commandLineArgs = new String[args.length];
        System.arraycopy(args, 0, commandLineArgs, 0, args.length);
        valueMap.clear();
        for (int i = 0; i < args.length; ++i) {
            String flag = args[i];
            if (!flag.startsWith("-"))
                continue;
            flag = flag.substring(1);
            String value = "true";
            if ((i + 1) < args.length) {
                String tmp = args[i + 1];
                if (!tmp.startsWith("-")) {
                    value = tmp;
                    ++i;
                }
            }
            valueMap.put(flag, value);
        }
    }

    public void addOptionDef(OptionDef def) {
        optionDefMap.put(def.getFlag(), def);
    }

    public void addOptionDefs(OptionDef[] defs) {
        for (OptionDef def : defs) {
            addOptionDef(def);
        }
    }

    public void addOptionDefs(Collection defs) {
        for (Object def : defs) {
            addOptionDef((OptionDef) def);
        }
    }

    public Collection<OptionDef> getOptionDefs() {
        return new ArrayList<OptionDef>(optionDefMap.values());
    }

    public void set(String key, String value) {
        valueMap.put(key, value);
    }

    public String get(String key) {
        String val = valueMap.get(key);
        if (val == null) {
            OptionDef def = optionDefMap.get(key);
            if (def != null)
                val = def.getDefaultValue();
        }
        return val;
    }

    public String get(String key, String def) {
        String val = valueMap.get(key);
        if (val == null)
            val = def;
        return val;
    }

    public int intValue(String key) {
        return intValue(key, 0);
    }

    public int intValue(String key, int n) {
        String str = get(key);
        if (str != null)
            return Integer.parseInt(str);
        return n;
    }

    public long longValue(String key) {
        return longValue(key, 0);
    }

    public long longValue(String key, long n) {
        String str = get(key);
        if (str != null)
            return Long.parseLong(str);
        return n;
    }

    public boolean boolValue(String key) {
        return boolValue(key, false);
    }

    public boolean boolValue(String key, boolean n) {
        String str = get(key);
        if (str != null)
            return Boolean.valueOf(str);
        return n;
    }

    /**
     * Gets the names of the arguments that were set on the command
     * line in a collection.
     */
    public Collection<String> getDefinedFlags() {
        return new ArrayList<String>(valueMap.keySet());
    }

    /**
     * Gets the command line arguments as they were passed to the last call
     * to {@link #parse}.
     */
    public String[] getCommandLineArguments() {
        String[] args = new String[commandLineArgs.length];
        System.arraycopy(commandLineArgs, 0, args, 0, commandLineArgs.length);
        return args;
    }

    private List getSortedEntries() {
        // Collect all the options into a List.
        List<Map.Entry> entries = new ArrayList<Map.Entry>(valueMap.entrySet());

        // Sort the list.
        Collections.sort(entries, new Comparator() {
            public int compare(Object o1, Object o2) {
                Map.Entry entry1 = (Map.Entry) o1;
                Map.Entry entry2 = (Map.Entry) o2;
                String key1 = (String) entry1.getKey();
                String key2 = (String) entry2.getKey();
                return key1.compareTo(key2);
            }
        });
        return entries;
    }

    public String toString() {
        // Collect all the options into a List.
        List entries = getSortedEntries();

        // Write the results.
        StringBuilder buf = new StringBuilder();
        String prefix = "";
        for (Object entry1 : entries) {
            buf.append(prefix);
            Map.Entry entry = (Map.Entry) entry1;
            buf.append(entry.getKey()).append('=').append(entry.getValue());
            prefix = ", ";
        }
        return buf.toString();
    }

    public void dump() {
        // Collect all the options into a List.
        List entries = getSortedEntries();

        // Get the length of the longest key.
        int len = 0;
        for (Object entry1 : entries) {
            Map.Entry entry = (Map.Entry) entry1;
            String key = (String) entry.getKey();
            int keyLen = key.length();
            if (keyLen > len)
                len = keyLen;
        }

        // Write the results.
        StringBuffer buf = new StringBuffer();
        for (Object entry2 : entries) {
            Map.Entry entry = (Map.Entry) entry2;
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            buf.append(key);
            for (int i = key.length(); i < len; ++i)
                buf.append(' ');
            buf.append("= ").append(value).append('\n');
        }
        System.out.print(buf);
    }
}
