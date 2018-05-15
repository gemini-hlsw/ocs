package jsky.app.ot.gemini.editor.targetComponent.telescopePosTableModel;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * The columns of the table.
 */
enum Column {
    TAG("Type Tag") {
        public String getValue(final Row row) { return row.tag(); }
    },

    NAME("Name") {
        public String getValue(final Row row) { return row.name(); }
    },

    RA("RA") {
        public String getValue(final Row row) {
            return row.type().raStringExtractor(row);
        }
    },

    DEC("Dec") {
        public String getValue(final Row row) {
            return row.type().decStringExtractor(row);
        }
    },

    DIST("Dist") {
        public String getValue(final Row row) {
            nf.setMaximumFractionDigits(2);
            return row.distance().map(nf::format).getOrElse("");
        }
    };

    private final String displayName;
    Column(final String displayName) { this.displayName = displayName; }
    public String displayName() { return displayName; }
    public abstract String getValue(final Row row);

    private final static NumberFormat nf = NumberFormat.getInstance(Locale.US);
}
