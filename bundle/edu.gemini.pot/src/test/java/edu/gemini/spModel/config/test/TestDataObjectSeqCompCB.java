// Copyright 2000
// Association for Universities for Research in Astronomy, Inc.
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: TestDataObjectSeqCompCB.java 27568 2010-10-25 18:03:42Z swalker $
//

package edu.gemini.spModel.config.test;

import edu.gemini.spModel.config.AbstractSeqComponentCB;

import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.data.config.IParameter;
import edu.gemini.spModel.data.config.ISysConfig;

import edu.gemini.pot.sp.ISPSeqComponent;

import java.util.*;


public class TestDataObjectSeqCompCB extends AbstractSeqComponentCB {

    private class SeqData {

        String name;
        Iterator iter;
    }

    private transient ISysConfig _sysConfig;

    private transient String _systemName;
    private transient List _normalParamList;  // List of IParameter
    private transient List _seqDataList;      // List of SeqData

    public TestDataObjectSeqCompCB(ISPSeqComponent seqComp) {
        super(seqComp);
    }

    public Object clone() {
        TestDataObjectSeqCompCB result = (TestDataObjectSeqCompCB) super.clone();
        result._sysConfig = null;
        result._systemName = null;
        result._normalParamList = null;
        result._seqDataList = null;
        return result;
    }

    protected void thisReset(Map options) {
        TestDataObject dataObj = (TestDataObject) getDataObject();
        _sysConfig = dataObj.getSysConfig();

        if (_sysConfig == null) {
            return;
        }

        _systemName = _sysConfig.getSystemName();
        _normalParamList = new LinkedList();
        _seqDataList = new LinkedList();

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
        if (_normalParamList.size() > 0) {
            Iterator it = _normalParamList.iterator();
            while (it.hasNext()) {
                IParameter param = (IParameter) it.next();
                IParameter param2 = DefaultParameter.getInstance(param.getName(), param.getValue());
                config.putParameter(_systemName, param2);
            }
            _normalParamList.clear();
        }

        if (_seqDataList.size() > 0) {
            ListIterator lit = _seqDataList.listIterator();

            while (lit.hasNext()) {
                SeqData sd = (SeqData) lit.next();
                IParameter param2 = DefaultParameter.getInstance(sd.name, sd.iter.next());
                config.putParameter(_systemName, param2);
                if (!sd.iter.hasNext()) {
                    lit.remove();
                }
            }
        }
    }

}
