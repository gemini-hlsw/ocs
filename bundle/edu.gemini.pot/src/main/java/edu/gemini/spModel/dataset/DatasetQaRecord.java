package edu.gemini.spModel.dataset;

import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.pio.PioParseException;

import java.io.Serializable;
import java.text.ParseException;

/**
 * This is simply a triple (DatasetLabel, DatasetQaState, String) to hold the QA
 * state and a comment.  Everything else is Java boilerplate and PIO boilerplate
 */
public final class DatasetQaRecord implements Comparable, Serializable {
    public static DatasetQaRecord empty(DatasetLabel label) {
        return new DatasetQaRecord(label, DatasetQaState.UNDEFINED, "");
    }

    public static final String PARAM_SET = "datasetQa";
    public static final String LABEL_PARAM    = "label";
    public static final String QA_STATE_PARAM = "qaState";
    public static final String COMMENT_PARAM  = "comment";

    public final DatasetLabel label;
    public final DatasetQaState qaState;
    public final String comment;

    public DatasetQaRecord(DatasetLabel label, DatasetQaState qaState, String comment) {
        if (label == null) throw new NullPointerException();
        if (qaState == null) throw new NullPointerException();
        if (comment == null) throw new NullPointerException();
        this.label   = label;
        this.qaState = qaState;
        this.comment = comment;
    }

    public DatasetQaRecord(ParamSet paramSet) throws PioParseException {
        final String s0 = Pio.getValue(paramSet, LABEL_PARAM);
        if (s0 == null) throw new PioParseException("Unspecified dataset label");
        try {
            label = new DatasetLabel(s0);
        } catch (ParseException ex) {
            throw new PioParseException("Could not parse dataset label: " + s0, ex);
        }

        final String s1 = Pio.getValue(paramSet, QA_STATE_PARAM, DatasetQaState.UNDEFINED.toString());
        final DatasetQaState q = DatasetQaState.parseType(s1);
        qaState = (q == null) ? DatasetQaState.UNDEFINED : q;
        comment = Pio.getValue(paramSet, COMMENT_PARAM, "");
    }

    public DatasetQaRecord withQaState(DatasetQaState qaState) {
        return (qaState == this.qaState) ? this : new DatasetQaRecord(label, qaState, comment);
    }

    public DatasetQaRecord withComment(String comment) {
        return comment.equals(this.comment) ? this : new DatasetQaRecord(label, this.qaState, comment);
    }

    public synchronized ParamSet toParamSet(PioFactory factory) {
         final ParamSet paramSet = factory.createParamSet(PARAM_SET);
         Pio.addParam(factory, paramSet, LABEL_PARAM, label.toString());
         Pio.addParam(factory, paramSet, QA_STATE_PARAM, qaState.name());
         if (!"".equals(comment)) {
             Pio.addParam(factory, paramSet, COMMENT_PARAM, comment);
         }
         return paramSet;
     }

    @Override
    public int compareTo(Object o) {
        final DatasetQaRecord that = (DatasetQaRecord) o;

        int res;

        res = label.compareTo(that.label);
        if (res != 0) return res;

        res = qaState.compareTo(that.qaState);
        if (res != 0) return res;

        res = comment.compareTo(that.comment);
        if (res != 0) return res;

        return res;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final DatasetQaRecord that = (DatasetQaRecord) o;

        if (!label.equals(that.label)) return false;
        if (!comment.equals(that.comment)) return false;
        return qaState == that.qaState;
    }

    @Override
    public int hashCode() {
        int result = label.hashCode();
        result = 31 * result + qaState.hashCode();
        result = 31 * result + comment.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "DatasetQaRecord{" + "label=" + label + ", qaState=" + qaState + ", comment='" + comment + '\'' + '}';
    }
}
