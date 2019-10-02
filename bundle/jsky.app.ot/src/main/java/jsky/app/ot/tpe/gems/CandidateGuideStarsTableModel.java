package jsky.app.ot.tpe.gems;

import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.core.Angle;
import edu.gemini.spModel.core.MagnitudeBand;
import edu.gemini.spModel.core.SiderealTarget;
import edu.gemini.catalog.api.CatalogName.UCAC4$;
import jsky.catalog.FieldDesc;
import jsky.catalog.FieldDescAdapter;
import jsky.catalog.TableQueryResult;
import jsky.catalog.skycat.SkycatConfigEntry;
import jsky.catalog.skycat.SkycatTable;

import javax.swing.table.DefaultTableModel;
import java.util.*;
import java.util.function.Predicate;

/**
 * OT-111: Model for {@link CandidateGuideStarsTable}
 */
class CandidateGuideStarsTableModel extends DefaultTableModel {

    // The NIR band is selected in the UI, the others are listed afterwards (UNUSED_BAND*).
    // Always including them in the table makes the SkyObjectFactory code easier later on.
    private enum Cols {
        CHECK, ID, _r, R, UC, RA, DEC, J, H, K
    }

    private static final Set<Cols> BANDS_COLUMNS;

    static {
        final Set<Cols> tmp = new HashSet<>();
        Collections.addAll(tmp, Cols._r, Cols.R, Cols.UC, Cols.J, Cols.H, Cols.K);
        BANDS_COLUMNS = Collections.unmodifiableSet(tmp);
    }

    private static final String RA_COL = "ra_col";
    private static final String DEC_COL = "dec_col";
    private static final String SERV_TYPE = "serv_type";
    private static final String LONG_NAME = "long_name";

    private static final String RA_TITLE = "RA";
    private static final String DEC_TITLE = "Dec";

    // User interface model
    private final GemsGuideStarSearchModel _model;
    private final boolean _isUCAC4;

    // The unselected bands (displayed at end)
    private static final ImList<MagnitudeBand> JHK =
        DefaultImList.create(
            MagnitudeBand.unsafeFromString("J"), // can't reference directly
            MagnitudeBand.unsafeFromString("H"), // from Java so going through
            MagnitudeBand.unsafeFromString("K")  // the lookup by string route
        );

    // Table column names
    private final Vector<String> _columnNames;

    // SideralTargets corresponding to the table rows
    private List<SiderealTarget> _siderealTargets;

    CandidateGuideStarsTableModel(final GemsGuideStarSearchModel model) {
        _model           = model;
        _siderealTargets = model.getCwfsCandidates();
        _isUCAC4         = model.getCatalog().catalog() == UCAC4$.MODULE$;
        _columnNames     = makeColumnNames(_isUCAC4);
        setDataVector(makeDataVector(_siderealTargets, _isUCAC4), _columnNames);
    }

    private static Vector<String> makeColumnNames(final boolean isUCAC4) {
        final Vector<String> columnNames = new Vector<>();
        columnNames.add(""); // checkbox column
        columnNames.add("Id");
        if (isUCAC4) {
            columnNames.add("r'");
            columnNames.add("UC");
        } else {
            columnNames.add("R");
        }
        columnNames.add(RA_TITLE);
        columnNames.add(DEC_TITLE);
        JHK.foreach(b -> columnNames.add(b.name()));
        return columnNames;
    }

    private static Vector<Vector<Object>> makeDataVector(
      final List<SiderealTarget> siderealTargets,
      final boolean              isUCAC4
    ) {
        final Vector<Vector<Object>> rows = new Vector<>();
        for (SiderealTarget siderealTarget : siderealTargets) {
            rows.add(new Vector<Object>(isUCAC4                     ?
                CatalogUtils4Java.makeUCAC4Row(siderealTarget, JHK) :
                CatalogUtils4Java.makeRow(siderealTarget, JHK)
            ));
        }
        return rows;
    }

    @Override
    public boolean isCellEditable(final int row, final int column) {
        return column == Cols.CHECK.ordinal();
    }

    @Override
    public Class<?> getColumnClass(final int columnIndex) {
        if (columnIndex == Cols.CHECK.ordinal())
            return Boolean.class;
        if (columnIndex == Cols.ID.ordinal())
            return Object.class;
        if (BANDS_COLUMNS.stream().map(c -> c.ordinal()).anyMatch(i -> i == columnIndex))
            return Double.class;
        return String.class;
    }

    public FieldDesc[] getFields() {
        final List<FieldDescAdapter> fields = new ArrayList<>();
        for(String columnName: _columnNames) {
            FieldDescAdapter desc = new FieldDescAdapter(columnName);
            if (columnName.equals(RA_TITLE)) {
                desc.setIsRA(true);
            }
            if (columnName.equals(DEC_TITLE)) {
                desc.setIsDec(true);
            }
            if (columnName.equals("")) {
                desc.setIsId(true);
            }
            fields.add(desc);
        }
        FieldDescAdapter[] fd = new FieldDescAdapter[_columnNames.size()];
        return fields.toArray(fd);
    }

    @SuppressWarnings("unchecked")
    TableQueryResult getTableQueryResult() {
        final String raPosition = String.valueOf(_columnNames.indexOf(RA_TITLE));
        final String decPosition = String.valueOf(_columnNames.indexOf(DEC_TITLE));
        final Properties props = new Properties();
        props.setProperty(RA_COL, raPosition);
        props.setProperty(DEC_COL, decPosition);
        props.setProperty(SERV_TYPE, "catalog");
        props.setProperty(LONG_NAME, "ucac4");
        final SkycatConfigEntry entry = new SkycatConfigEntry(props);
        return new SkycatTable(entry, CandidateGuideStarsTableModel.this.getDataVector(), getFields()) {
            @Override
            public Option<SiderealTarget> getSiderealTarget(int i) {
                return _model.cwfsTargetAt(i);
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return columnIndex == Cols.CHECK.ordinal();
            }

            @Override
            public String getId() {
                return "GEMS"; // see entry in ot.skycat.cfg - needed for GemsSkyObjectFactory
            }
        };
    }

    private List<SiderealTarget> getFilteredCandidates(Predicate<Integer> f) {
        final List<SiderealTarget> result = new ArrayList<>();
        final int numRows = getRowCount();

        for(int row = 0; row < numRows; row++) {
            if (f.test(row)) result.add(_siderealTargets.get(row));
        }
        return result;
    }

    private final Predicate<Integer> checked =
            r -> (Boolean) getValueAt(r, Cols.CHECK.ordinal());

    /**
     * Returns the subset of all candidate targets that are checked in the UI.
     */
    List<SiderealTarget> getCheckedCandidates() {
        return getFilteredCandidates(checked);
    }

    /**
     * Returns the subset of all candidate targets that are not checked in the UI.
     */
    List<SiderealTarget> getUncheckedCandidates() {
        return getFilteredCandidates(checked.negate());
    }

    /**
     * Returns all candidate targets.
     */
    List<SiderealTarget> getAllCandidates() {
        return getFilteredCandidates(r -> true);
    }
}
