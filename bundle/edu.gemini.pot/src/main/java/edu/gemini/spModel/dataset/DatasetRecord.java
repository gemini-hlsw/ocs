package edu.gemini.spModel.dataset;

/**
 * Combination of DatasetQaRecord and DatasetExecRecord.
 */
public final class DatasetRecord {
    public final DatasetQaRecord qa;
    public final DatasetExecRecord exec;

    public DatasetRecord(DatasetQaRecord qa, DatasetExecRecord exec) {
        if (qa == null) throw new NullPointerException();
        if (exec == null) throw new NullPointerException();
        if (!qa.label.equals(exec.getLabel())) {
            final String msg = String.format("QA and Exec records for different datasets: %s vs %s", qa.label, exec.getLabel());
            throw new IllegalArgumentException(msg);
        }
        this.qa   = qa;
        this.exec = exec;
    }

    public DatasetLabel getLabel() { return qa.label; }

    public DatasetRecord withQa(DatasetQaRecord qa) { return new DatasetRecord(qa, exec); }
    public DatasetRecord withExec(DatasetExecRecord exec) { return new DatasetRecord(qa, exec); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final DatasetRecord that = (DatasetRecord) o;

        if (!exec.equals(that.exec)) return false;
        if (!qa.equals(that.qa)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = qa.hashCode();
        result = 31 * result + exec.hashCode();
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DatasetRecord{");
        sb.append("qa=").append(qa);
        sb.append(", exec=").append(exec);
        sb.append('}');
        return sb.toString();
    }
}
