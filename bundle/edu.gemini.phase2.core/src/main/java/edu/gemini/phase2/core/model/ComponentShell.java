package edu.gemini.phase2.core.model;

import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;

import java.io.Serializable;

public abstract class ComponentShell implements Serializable {
    public static final String DATA_OBJ_PARAM_SET = "dataObject";
    public static final String CLASS_NAME_PARAM   = "class";

    protected abstract String paramSetName();

    public final ISPDataObject dataObject;

    protected ComponentShell(ISPDataObject dataObject) {
        if (dataObject == null) throw new IllegalArgumentException("dataObject is null");
        this.dataObject = dataObject;
    }

    protected ComponentShell(ParamSet pset) {
        ParamSet dpset   = pset.getParamSet(DATA_OBJ_PARAM_SET);

        String className = Pio.getValue(dpset, CLASS_NAME_PARAM);
        try {
            Class c = Class.forName(className, true, this.getClass().getClassLoader());
            dataObject = (ISPDataObject) c.newInstance();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        ParamSet dobj = dpset.getParamSets().get(0);
        dataObject.setParamSet(dobj);
    }

    public ParamSet toParamSet(PioFactory factory) {
        ParamSet pset = factory.createParamSet(paramSetName());

        ParamSet dpset = factory.createParamSet(DATA_OBJ_PARAM_SET);
        Pio.addParam(factory, dpset, CLASS_NAME_PARAM, dataObject.getClass().getName());
        dpset.addParamSet(dataObject.getParamSet(factory));
        pset.addParamSet(dpset);

        return pset;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ComponentShell that = (ComponentShell) o;

        if (!dataObject.equals(that.dataObject)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return dataObject.hashCode();
    }
}
