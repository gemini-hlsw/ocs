package jsky.app.ot.gemini.editor.auxfile;

import edu.gemini.auxfile.api.AuxFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.swing.table.AbstractTableModel;

public final class AttachmentTableModel extends AbstractTableModel {
    public enum Col {
        NAME("Name", String.class) {
            @Override public String getValue(AuxFile file) {
                return file.getName();
            }
        },
        SIZE("Size", Integer.class) {
            @Override public Object getValue(AuxFile file) {
                return file.getSize();
            }
        },
        LAST_MOD("Last Modified (UTC)", Date.class) {
            @Override public Object getValue(AuxFile file) {
                long l = file.getLastModified();
                return new Date(l);
            }
        },
        DESCRIPTION("Description", String.class) {
            @Override public Object getValue(AuxFile file) {
                return file.getDescription();
            }
        },
        CHECKED("NGO Check?", Boolean.class) {
            @Override public Object getValue(AuxFile file) {
                return file.isChecked();
            }
        }

        ;

        private final String displayValue;
        private final Class type;

        Col(String displayValue, Class columnClass) {
            this.displayValue = displayValue;
            type = columnClass;
        }

        public String displayValue() { return displayValue; }
        public Class getColumnClass() { return type; }
        public abstract Object getValue(AuxFile file);
    }

    private final List<AuxFile> fileList;

    public AttachmentTableModel(List<AuxFile> fileList) {
        this.fileList = Collections.unmodifiableList(new ArrayList<AuxFile>(fileList));
    }

    public int getRowCount() {
        return fileList.size();
    }

    public int getColumnCount() {
        return Col.values().length;
    }

    public Object getValueAt(int row, int col) {
        AuxFile file = fileList.get(row);
        return Col.values()[col].getValue(file);
    }

    public String getColumnName(int col) {
        return Col.values()[col].displayValue();
    }

    public Class getColumnClass(int col) {
        return Col.values()[col].getColumnClass();
    }

    @Override public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false; // columnIndex == Col.CHECKED.ordinal();
    }

    @Override public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
//        assert columnIndex == Col.CHECKED.ordinal(); // this is the only editable column
    }
}
