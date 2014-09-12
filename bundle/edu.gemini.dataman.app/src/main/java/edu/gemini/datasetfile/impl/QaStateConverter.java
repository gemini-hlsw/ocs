//
// $Id: QaStateConverter.java 124 2005-09-13 15:52:56Z shane $
//

package edu.gemini.datasetfile.impl;

import edu.gemini.spModel.dataset.DatasetQaState;
import edu.gemini.fits.HeaderItem;
import edu.gemini.fits.Header;
import edu.gemini.fits.DefaultHeaderItem;

import java.util.*;

/**
 *
 */
final class QaStateConverter {

    static final String RAWGEMQA = "RAWGEMQA";
    static final String RAWPIREQ = "RAWPIREQ";

    static final Set<String> KEYWORDS;

    static {
        Set<String> tmp = new TreeSet<String>();
        tmp.add(RAWGEMQA);
        tmp.add(RAWPIREQ);
        KEYWORDS = Collections.unmodifiableSet(tmp);
    }

    private static final Map<String, DatasetQaState> RAW_TO_QA;
    private static final Map<DatasetQaState, String> QA_TO_RAWGEMQA;
    private static final Map<DatasetQaState, String> QA_TO_RAWPIREQ;

    static {
        RAW_TO_QA = new TreeMap<String, DatasetQaState>();
        RAW_TO_QA.put("CHECK,CHECK", DatasetQaState.CHECK);
        RAW_TO_QA.put("USABLE,YES",  DatasetQaState.PASS);
        RAW_TO_QA.put("USABLE,NO",   DatasetQaState.USABLE);
        RAW_TO_QA.put("BAD,NO",      DatasetQaState.FAIL);

        QA_TO_RAWGEMQA = new TreeMap<DatasetQaState, String>();
        QA_TO_RAWGEMQA.put(DatasetQaState.UNDEFINED, "UNKNOWN");
        QA_TO_RAWGEMQA.put(DatasetQaState.CHECK,     "CHECK");
        QA_TO_RAWGEMQA.put(DatasetQaState.PASS,      "USABLE");
        QA_TO_RAWGEMQA.put(DatasetQaState.USABLE,    "USABLE");
        QA_TO_RAWGEMQA.put(DatasetQaState.FAIL,      "BAD");

        QA_TO_RAWPIREQ = new TreeMap<DatasetQaState, String>();
        QA_TO_RAWPIREQ.put(DatasetQaState.UNDEFINED, "UNKNOWN");
        QA_TO_RAWPIREQ.put(DatasetQaState.CHECK,     "CHECK");
        QA_TO_RAWPIREQ.put(DatasetQaState.PASS,      "YES");
        QA_TO_RAWPIREQ.put(DatasetQaState.USABLE,    "NO");
        QA_TO_RAWPIREQ.put(DatasetQaState.FAIL,      "NO");
    }

    static DatasetQaState toQaState(String rawGemQa, String rawPiReq) {
        if (rawGemQa == null) {
            return DatasetQaState.UNDEFINED;
        }
        if (rawPiReq == null) {
            return DatasetQaState.UNDEFINED;
        }
        StringBuilder key = new StringBuilder();
        key.append(rawGemQa.toUpperCase()).append(',').append(rawPiReq.toUpperCase());
        DatasetQaState res = RAW_TO_QA.get(key.toString());
        if (res == null) res = DatasetQaState.UNDEFINED;
        return res;
    }

    static DatasetQaState toQaState(HeaderItem rawGemQa, HeaderItem rawPiReq) {
        if (rawGemQa == null) {
            return DatasetQaState.UNDEFINED;
        }
        if (rawPiReq == null) {
            return DatasetQaState.UNDEFINED;
        }
        String rawGemQaStr = rawGemQa.getValue();
        String rawPiReqStr = rawPiReq.getValue();
        return toQaState(rawGemQaStr, rawPiReqStr);
    }

    static DatasetQaState getQaState(Header h) {
        HeaderItem rawGemQa = h.get(RAWGEMQA);
        HeaderItem rawPiReq = h.get(RAWPIREQ);
        return toQaState(rawGemQa, rawPiReq);
    }

    static String toRawGemQaString(DatasetQaState qa) {
        return QA_TO_RAWGEMQA.get(qa);
    }

    static HeaderItem toRawGemQaItem(DatasetQaState qa) {
        String qaStr   = toRawGemQaString(qa);
        String comment = "Gemini Quality Assessment";
        return DefaultHeaderItem.create(RAWGEMQA, qaStr, comment);
    }

    static String toRawPiReqString(DatasetQaState qa) {
        return QA_TO_RAWPIREQ.get(qa);
    }

    static HeaderItem toRawPiReqItem(DatasetQaState qa) {
        String reqStr  = toRawPiReqString(qa);
        String comment = "PI Requirements Met";
        return DefaultHeaderItem.create(RAWPIREQ, reqStr, comment);
    }
}
