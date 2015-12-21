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
        Iterator<?> iter;
    }

    private transient ISysConfig _sysConfig;

    private transient String _systemName;
    private transient List<IParameter> _normalParamList;  // List of IParameter
    private transient List<SeqData> _seqDataList;      // List of SeqData

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

    @Override
    protected void thisReset(Map<String, Object> options) {
        TestDataObject dataObj = (TestDataObject) getDataObject();
        _sysConfig = dataObj.getSysConfig();

        if (_sysConfig == null) {
            return;
        }

        _systemName = _sysConfig.getSystemName();
        _normalParamList = new LinkedList<>();
        _seqDataList = new LinkedList<>();

        for (IParameter param : _sysConfig.getParameters()) {
            Object value = param.getValue();
            if (value instanceof Collection) {
                Iterator<?> valueIt = ((Collection<?>) value).iterator();
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
            for (IParameter param : _normalParamList) {
                IParameter param2 = DefaultParameter.getInstance(param.getName(), param.getValue());
                config.putParameter(_systemName, param2);
            }
            _normalParamList.clear();
        }

        if (_seqDataList.size() > 0) {
            ListIterator<SeqData> lit = _seqDataList.listIterator();

            while (lit.hasNext()) {
                SeqData sd = lit.next();
                IParameter param2 = DefaultParameter.getInstance(sd.name, sd.iter.next());
                config.putParameter(_systemName, param2);
                if (!sd.iter.hasNext()) {
                    lit.remove();
                }
            }
        }
    }

}
