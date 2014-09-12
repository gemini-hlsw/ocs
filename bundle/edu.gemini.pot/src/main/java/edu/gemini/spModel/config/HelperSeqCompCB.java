// Copyright 2000
// Association for Universities for Research in Astronomy, Inc.
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: HelperSeqCompCB.java 39256 2011-11-22 17:42:49Z swalker $
//

package edu.gemini.spModel.config;

import edu.gemini.pot.sp.ISPSeqComponent;

import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.data.config.IConfigProvider;
import edu.gemini.spModel.data.config.IParameter;
import edu.gemini.spModel.data.config.ISysConfig;

import java.util.*;


public class HelperSeqCompCB extends AbstractSeqComponentCB {

    private class SeqData {

        String name;
        Iterator iter;
    }

    private transient ISysConfig _sysConfig;

    private transient String _systemName;
    private transient List<IParameter> _normalParamList;  // List of IParameter
    private transient List<SeqData> _seqDataList;      // List of SeqData

    public HelperSeqCompCB(ISPSeqComponent seqComp) {
        super(seqComp);
    }

    public Object clone() {
        HelperSeqCompCB result = (HelperSeqCompCB) super.clone();
        result._sysConfig       = null;
        result._systemName      = null;
        result._normalParamList = null;
        result._seqDataList     = null;
        return result;
    }

    protected void thisReset(Map options) {
        IConfigProvider dataObj = (IConfigProvider) getDataObject();
        _sysConfig = dataObj.getSysConfig();
        if (_sysConfig == null) return;

        _systemName = _sysConfig.getSystemName();
        // Normal parameters are parameters that are a single value
        _normalParamList = new LinkedList<IParameter>();
        _seqDataList = new LinkedList<SeqData>();

        Iterator it = _sysConfig.getParameters().iterator();
        while (it.hasNext()) {
            IParameter param = (IParameter) it.next();
            Object value = param.getValue();
            if (value instanceof Collection) {
                Iterator valueIt = ((Collection) value).iterator();
                if (valueIt.hasNext()) {
                    SeqData sd = new SeqData();
                    sd.name = param.getName();
                    sd.iter = valueIt;
                    _seqDataList.add(sd);
                }
            } else {
                _normalParamList.add(param);
            }
        }
    }

    protected boolean thisHasNext() {
        if (_sysConfig == null)
            return false;

        if (_normalParamList.size() > 0) {
            return true;
        }

        if (_seqDataList.size() > 0) {
            return true;
        }

        return false;
    }

    protected void thisApplyNext(IConfig config, IConfig prevFull) {
        //
        // REL-226: null values can happen for nested instrument iterators and
        // even for top level instrument iterators that aren't first.  When the
        // user adds an item, no value is added in any rows that exist.
        //
        // In this case, we don't inject a null into the sequence but rather
        // allow the value already in "config" to remain (if any).
        //

        if (_normalParamList.size() > 0) {
            Iterator<IParameter> it = _normalParamList.iterator();
            while (it.hasNext()) {
                IParameter param = it.next();
                IParameter param2 = DefaultParameter.getInstance(param.getName(), param.getValue());
                if (param.getValue() != null) config.putParameter(_systemName, param2);
            }
            _normalParamList.clear();
        }

        if (_seqDataList.size() > 0) {
            ListIterator<SeqData> lit = _seqDataList.listIterator();

            while (lit.hasNext()) {
                SeqData sd = lit.next();
                Object val = sd.iter.next();
                if (val != null) {
                    IParameter param2 = DefaultParameter.getInstance(sd.name, val);
                    config.putParameter(_systemName, param2);
                }
                if (!sd.iter.hasNext()) {
                    lit.remove();
                }
            }
        }
    }

}
