/*
 * Copyright 2003 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: HistoryList.java 6000 2005-04-29 19:30:48Z brighton $
 */
package edu.gemini.spModel.gemini.obscomp;

import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.pot.sp.ISPCloneable;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

public class HistoryList implements Serializable, ISPCloneable {

    /** An item in the observation history list */
    public static class HistoryListItem implements Serializable {
        long _time;
        ObsEventMsg _event;
        String _message;

        // paramset tags
        private static final String _TIME = "time";
        private static final String _MESSAGE = "message";

        public HistoryListItem() {
        }

        public HistoryListItem(long time, ObsEventMsg event, String message) {
            _time = time;
            _event = event;
            _message = message;
        }

        public long getTime() {
            return _time;
        }

        public ObsEventMsg getEvent() {
            return _event;
        }

        public String getMessage() {
            return _message;
        }

        public void setMessage(String message) {
            _message = message;
        }

        /**
         * Return a parameter set describing the current state of this object.
         */
        public ParamSet getParamSet(PioFactory factory) {
            ParamSet paramSet = factory.createParamSet(getEvent().getMsgName());
            Pio.addParam(factory, paramSet, _TIME, Long.toString(getTime()), "milliseconds");
            Pio.addParam(factory, paramSet, _MESSAGE, getMessage());

            return paramSet;
        }

        /**
         * Set the state of this object from the given parameter set.
         */
        public void setParamSet(ParamSet paramSet) {
            _event = ObsEventMsg.fromString(paramSet.getName());
            _time = Long.parseLong(Pio.getValue(paramSet, _TIME));
            _message = Pio.getValue(paramSet, _MESSAGE);
        }
    }

    private static final long serialVersionUID = 1L;

    public static final String HISTORY_PROP = "history";

    private List _historyList = new ArrayList();

    public HistoryList() {
    }

    public void add(HistoryListItem item) {
//        ObsEventMsg msg = item.getEvent();
//        if (msg == ObsEventMsg.OBSERVATION_START && _contains(msg)) {
//            // by request: see OCS-41: don't add obs start if already present
//            return;
//        }
        _historyList.add(item);
    }

//    // Return true if the history list contains the given event
//    private boolean _contains(ObsEventMsg msg) {
//        Iterator it = _historyList.iterator();
//        while(it.hasNext()) {
//            HistoryListItem item = (HistoryListItem)it.next();
//            if (item.getEvent() == msg) {
//                return true;
//            }
//        }
//        return false;
//    }

    public HistoryListItem get(int i) {
        return (HistoryListItem)_historyList.get(i);
    }

    public Object clone() {
        try {
            HistoryList historyList = (HistoryList)super.clone();
            historyList._historyList = new ArrayList(_historyList);
            return historyList;
        } catch (CloneNotSupportedException e) {
            throw new UnsupportedOperationException();
        }
    }

    public int size() {
        return _historyList.size();
    }

    public void clear() {
        _historyList.clear();
    }

    public Iterator iterator() {
        return _historyList.iterator();
    }

    /**
     * Remove the given items from the history list.
     *
     * @param rows an array of item indexes in the history list.
     */
    public void removeRows(int[] rows) {
        for (int i = rows.length - 1; i >= 0; i--) {
            _historyList.remove(rows[i]);
        }
    }

    /**
     * Return a parameter set describing the current state of this object.
     */
    public ParamSet getParamSet(PioFactory factory, String name) {
        ParamSet paramSet = factory.createParamSet(name);
        int n = _historyList.size();
        if (n != 0) {
            for (int i = 0; i < n; i++) {
                ParamSet p = get(i).getParamSet(factory);
                p.setSequence(i);
                paramSet.addParamSet(p);
            }
        }
        return paramSet;
    }

    /**
     * Set the state of this object from the given parameter set.
     */
    public void setParamSet(ParamSet paramSet) {
        clear();
        if (paramSet == null) {
            return;
        }
        Iterator it = paramSet.getParamSets().iterator();
        while(it.hasNext()) {
            HistoryListItem item = new HistoryListItem();
            item.setParamSet((ParamSet)it.next());
            add(item);
        }
    }
}
