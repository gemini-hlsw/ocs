package jsky.app.ot.gemini.editor.targetComponent.telescopePosTableModel;

/**
 * The different types of rows in the table and some facilitators to extract
 * data from them.
 */
enum RowType {
    TARGET {
        @Override
        public String raStringExtractor(final Row row) {
            final TargetRow tr = (TargetRow) row;
            return tr.target().getRaString(row.when()).getOrElse("");
        }
        @Override
        public String decStringExtractor(final Row row) {
            final TargetRow tr = (TargetRow) row;
            return tr.target().getDecString(row.when()).getOrElse("");
        }
    },
    COORDINATES {
        @Override
        public String raStringExtractor(final Row row) {
            final CoordinatesRow crow = (CoordinatesRow) row;
            return crow.coordinates().ra().toAngle().formatHMS();
        }
        @Override
        public String decStringExtractor(final Row row) {
            final CoordinatesRow crow = (CoordinatesRow) row;
            return crow.coordinates().dec().toAngle().formatHMS();
        }
    },
    GROUP,
    ;

    public String raStringExtractor(final Row row) {
        return "";
    }
    public String decStringExtractor(final Row row) {
        return "";
    }
}
