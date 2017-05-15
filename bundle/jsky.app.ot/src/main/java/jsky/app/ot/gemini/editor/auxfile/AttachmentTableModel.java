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
            @Override public String getValue(final AuxFile file) {
                return file.getName();
            }
        },
        SIZE("Size", Integer.class) {
            @Override public Object getValue(final AuxFile file) {
                return file.getSize();
            }
        },
        LAST_MOD("Last Modified (UTC)", Date.class) {
            @Override public Object getValue(final AuxFile file) {
                final long l = file.getLastModified();
                return new Date(l);
            }
        },
        DESCRIPTION("Description", String.class) {
            @Override public Object getValue(final AuxFile file) {
                return file.getDescription();
            }
        },
        CHECKED("Check?", Boolean.class) {
            @Override public Object getValue(final AuxFile file) {
                return file.isChecked();
            }
        }

        ;

        private final String displayValue;
        private final Class type;

        Col(final String displayValue, final Class columnClass) {
            this.displayValue = displayValue;
            type = columnClass;
        }

        public String displayValue() { return displayValue; }
        public Class getColumnClass() { return type; }
        public abstract Object getValue(final AuxFile file);
    }

    private final List<AuxFile> fileList;

    public AttachmentTableModel(final List<AuxFile> fileList) {
        this.fileList = Collections.unmodifiableList(new ArrayList<>(fileList));
    }

    public int getRowCount() {
        return fileList.size();
    }

    public int getColumnCount() {
        return Col.values().length;
    }

    public Object getValueAt(final int row, final int col) {
        final AuxFile file = fileList.get(row);
        return Col.values()[col].getValue(file);
    }

    public String getColumnName(final int col) {
        return Col.values()[col].displayValue();
    }

    public Class getColumnClass(final int col) {
        return Col.values()[col].getColumnClass();
    }
}
