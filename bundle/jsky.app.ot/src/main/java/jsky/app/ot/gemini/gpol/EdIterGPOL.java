package jsky.app.ot.gemini.gpol;

import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.data.config.IConfigProvider;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.gemini.gpol.GPOLConstants;
import edu.gemini.spModel.gemini.gpol.GPOLParams.Angle;
import edu.gemini.spModel.gemini.gpol.GPOLParams.Calibrator;
import edu.gemini.spModel.gemini.gpol.GPOLParams.Modulator;
import jsky.app.ot.editor.EdIterGenericConfig;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;


/**
 * The iterator component is a modified generic iterator component with
 * fixed, default columns and standard default values.
 */
public class EdIterGPOL extends EdIterGenericConfig {

    public EdIterGPOL() {
        super();

        // Rather than deleting one button and creating another one,
        // redefine the meaning of the "Delete Item" button to "Create"
        // a default sequence
        JButton button = getDeleteItem();
        button.setText("Create Default");
        button.removeActionListener(this);
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _createDefault();
            }
        });
    }


    // Create a standard, default GPOL sequence.
    private void _createDefault() {
        IConfigProvider iIterConfigProvider = (IConfigProvider) getDataObject();
        ISysConfig currentSysConfig = iIterConfigProvider.getSysConfig();

        //currentSysConfig.removeParameters();

        List<Object> modulatorList = new ArrayList<Object>();
        modulatorList.add(Modulator.DEFAULT);

        List<Object> angleList = new ArrayList<Object>();
        angleList.add(Angle.ANGLE1);
        angleList.add(Angle.ANGLE2);
        angleList.add(Angle.ANGLE3);
        angleList.add(Angle.ANGLE4);

        List<Object> calibratorList = new ArrayList<Object>();
        calibratorList.add(Calibrator.DEFAULT);

        currentSysConfig.putParameter(DefaultParameter.getInstance(GPOLConstants.MODULATOR_PROP, modulatorList));
        currentSysConfig.putParameter(DefaultParameter.getInstance(GPOLConstants.ANGLE_PROP, angleList));
        currentSysConfig.putParameter(DefaultParameter.getInstance(GPOLConstants.CALIBRATOR_PROP, calibratorList));

        iIterConfigProvider.setSysConfig(currentSysConfig);

// RCN: is this necessary? may need to fix
//        setDataObject((ISPDataObject) iIterConfigProvider);
    }
}


