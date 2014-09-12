// Copyright 2000
// Association for Universities for Research in Astronomy, Inc.
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: TestDataObject.java 46768 2012-07-16 18:58:53Z rnorris $
//

package edu.gemini.spModel.config.test;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.data.config.IConfigProvider;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;

import java.beans.PropertyChangeListener;
import java.io.Serializable;



public class TestDataObject implements Serializable, IConfigProvider, ISPDataObject {

    private ISysConfig _sysConfig;


    public TestDataObject() {
    }

    public ISysConfig getSysConfig() {
        return (ISysConfig) _sysConfig.clone();
    }

    public void setSysConfig(ISysConfig sysConfig) {
        _sysConfig = (ISysConfig) sysConfig.clone();
    }

    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        throw new Error("unimplemented");
    }

    public void addPropertyChangeListener(String propName, PropertyChangeListener pcl) {
        throw new Error("unimplemented");
    }

    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        throw new Error("unimplemented");
    }

    public void removePropertyChangeListener(String propName, PropertyChangeListener rel) {
        throw new Error("unimplemented");
    }

    public String getTitle() {
        throw new Error("unimplemented");
    }

    public void setTitle(String title) {
        throw new Error("unimplemented");
    }

    public String getEditableTitle() {
        throw new Error("unimplemented");
    }

    public String getVersion() {
        throw new Error("unimplemented");
    }

    public void setParamSet(ParamSet paramSet) {
        throw new Error("unimplemented");
    }

    public ParamSet getParamSet(PioFactory factory) {
        throw new Error("unimplemented");
    }

    public SPComponentType getType() {
        throw new Error("unimplemented");
    }


}



