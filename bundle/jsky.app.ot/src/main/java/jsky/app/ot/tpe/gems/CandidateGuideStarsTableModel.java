package jsky.app.ot.tpe.gems;

import edu.gemini.ags.gems.GemsUtils4Java;
import edu.gemini.spModel.core.Target;
import jsky.catalog.Catalog;
import jsky.catalog.FieldDesc;
import jsky.catalog.FieldDescAdapter;
import jsky.catalog.TableQueryResult;
import jsky.catalog.skycat.SkycatCatalog;
import jsky.catalog.skycat.SkycatConfigEntry;
import jsky.catalog.skycat.SkycatConfigFile;
import jsky.catalog.skycat.SkycatTable;

import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

/**
 * OT-111: Model for {@link CandidateGuideStarsTable}
 */
class CandidateGuideStarsTableModel extends DefaultTableModel {

    // The NIR band is selected in the UI, the others are listed afterwards (UNUSED_BAND*).
    // Always including them in the table makes the SkyObjectFactory code easier later on.
    enum Cols {
        CHECK, ID, R, NIR_BAND, RA, DEC, UNUSED_BAND1, UNUSED_BAND2
    }

    // User interface model
    private GemsGuideStarSearchModel _model;

    // The selected NIR band
    private String _nirBand;

    // The unselected bands (displayed at end)
    private String[] _unusedBands;

    // Table column names
    private Vector<String> _columnNames;

    // SkyObjects corresponding to the table rows
    private List<Target.SiderealTarget> _siderealTargets;

    public CandidateGuideStarsTableModel(GemsGuideStarSearchModel model) {
        _model = model;
        _nirBand = _model.getBand().name();
        _unusedBands = getOtherNirBands(_nirBand);
        _columnNames = makeColumnNames();
        setDataVector(makeDataVector(), _columnNames);
    }

    private Vector<String> makeColumnNames() {
        Vector<String> columnNames = new Vector<>();
        columnNames.add(""); // checkbox column
        columnNames.add("Id");
        columnNames.add("R");
        columnNames.add(_nirBand);
        columnNames.add("RA");
        columnNames.add("Dec");
        columnNames.add(_unusedBands[0]);
        columnNames.add(_unusedBands[1]);
        return columnNames;
    }

    private String[] getOtherNirBands(String band) {
        String[] bands = new String[2];
        if ("J".equals(band)) {
            bands[0] = "H";
            bands[1] = "K";
        } else if ("H".equals(band)) {
            bands[0] = "J";
            bands[1] = "K";
        } else if ("K".equals(band)) {
            bands[0] = "J";
            bands[1] = "H";
        }
        return bands;
    }

    private Vector<Vector<Object>> makeDataVector() {
        _siderealTargets = GemsUtils4Java.uniqueTargets(_model.getGemsCatalogSearchResults());
        Vector<Vector<Object>> rows = new Vector<>();
        for (Target.SiderealTarget siderealTarget : _siderealTargets) {
            rows.add(CatalogUtils4Java.makeRow(siderealTarget, _nirBand, _unusedBands));
        }
        return rows;
    }


    @Override
    public boolean isCellEditable(int row, int column) {
        return column == 0;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == Cols.CHECK.ordinal())
            return Boolean.class;
        if (columnIndex == Cols.ID.ordinal())
            return Object.class;
        if (columnIndex == Cols.R.ordinal() || columnIndex == Cols.NIR_BAND.ordinal()
                || columnIndex == Cols.UNUSED_BAND1.ordinal() || columnIndex == Cols.UNUSED_BAND2.ordinal())
            return Double.class;
        return String.class;
    }

    public FieldDesc[] getFields() {
        FieldDescAdapter[] ar = new FieldDescAdapter[_columnNames.size()];
        for(int i = 0; i < ar.length; i++) {
            ar[i] = new FieldDescAdapter(_columnNames.get(i));
        }
        ar[Cols.ID.ordinal()].setIsId(true);
        ar[Cols.RA.ordinal()].setIsRA(true);
        ar[Cols.DEC.ordinal()].setIsDec(true);
        return ar;
    }

    public TableQueryResult getTableQueryResult(Catalog cat) {
        final SkycatConfigEntry configEntry;
        if (cat instanceof SkycatCatalog) {
            configEntry = ((SkycatCatalog) cat).getConfigEntry();
        } else if (cat instanceof SkycatTable) {
            configEntry = ((SkycatTable) cat).getConfigEntry();
        } else {
            throw new RuntimeException("Expected a skycat catalog or table");
        }

        Properties props = (Properties)configEntry.getProperties().clone();
        props.setProperty(SkycatConfigFile.RA_COL, String.valueOf(Cols.RA.ordinal()));
        props.setProperty(SkycatConfigFile.DEC_COL, String.valueOf(Cols.DEC.ordinal()));
        SkycatConfigEntry entry = new SkycatConfigEntry(props);
        SkycatTable skycatTable = new SkycatTable(entry, getDataVector(), getFields()) {
            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return columnIndex == Cols.CHECK.ordinal();
            }

            @Override
            public String getId() {
                return "GEMS"; // see entry in ot.skycat.cfg - needed for GemsSkyObjectFactory
            }
        };
        skycatTable.setCatalog(new SkycatCatalog(entry));
        return skycatTable;
    }

    /**
     * Returns a list of SkyObjects corresponding to the checked (or unchecked) rows in the table
     * @param checked if true, return the checked rows (candidates), otherwise the unchecked (non-candidates)
     */
    public List<Target.SiderealTarget> getCandidates(boolean checked) {
        List<Target.SiderealTarget> result = new ArrayList<>();
        int numRows = getRowCount();
        int col = Cols.CHECK.ordinal();
        for(int row = 0; row < numRows; row++) {
            if ((Boolean)getValueAt(row, col) == checked) {
                result.add(_siderealTargets.get(row));
            }
        }
        return result;
    }
}
