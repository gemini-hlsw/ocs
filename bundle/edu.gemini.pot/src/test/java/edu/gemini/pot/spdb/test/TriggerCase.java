//
// $Id: TriggerCase.java 47163 2012-08-01 23:09:47Z rnorris $
//
package edu.gemini.pot.spdb.test;

import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.IDBTriggerAction;
import edu.gemini.pot.spdb.IDBTriggerCondition;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;
import org.junit.Before;
import org.junit.Test;

import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * A base case for trigger test cases.  Handles creating a program and setting
 * it up to generate and record trigger events.
 */

public class TriggerCase extends SpdbBaseTestCase {
    private static final Logger LOG = Logger.getLogger(TriggerCase.class.getName());

    private static final SPComponentType NON_TRIGGER_TYPE = SPComponentType.AO_GEMS; // SPComponentType.getInstance("non_trigger", "non_trigger", "no trigger");
    private static final SPComponentType TRIGGER_TYPE = SPComponentType.INSTRUMENT_FLAMINGOS2; // SPComponentType.getInstance("trigger", "trigger", "trigger");

    public static final class ProgramDataObject implements ISPDataObject {
        private List<String> _triggerList = new ArrayList<>();

        public Object clone() {
            try {
                return super.clone();
            } catch (CloneNotSupportedException e) {
                throw new InternalError();
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public <A extends ISPDataObject> A clone(A a) {
            return (A) a.clone();
        }


        public String[] getTriggerMessages() {
            return _triggerList.toArray(new String[_triggerList.size()]);
        }

        public void addTriggerMessage(String message) {
            _triggerList.add(message);
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

    public static final class TriggerDataObject implements ISPDataObject, Serializable {
        private String _triggerMessage;

        public Object clone() {
            try {
                return super.clone();
            } catch (CloneNotSupportedException e) {
                throw new InternalError();
            }
        }

        @Override @SuppressWarnings("unchecked")
        public <A extends ISPDataObject> A clone(A a) {
            return (A) a.clone();
        }

        public String getTriggerMessage() {
            return _triggerMessage;
        }

        public void setTriggerMessage(String message) {
            _triggerMessage = message;
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

    /**
     * When an obs component of type TRIGGER_TYPE is stored, and it has a
     * non-null trigger message, generate a trigger.
     */
    public static class TestTriggerCondition implements IDBTriggerCondition {
        public Object matches(SPCompositeChange change) {
//            System.out.println("*** composite change");
            String propName = SPUtil.getDataObjectPropertyName();
            if (!propName.equals(change.getPropertyName())) {
//                System.out.println("\t*** not data object change: " + change.getPropertyName());
                return null;
            }

            Object newValue = change.getNewValue();
            if (newValue == null) return null;
            if (TriggerDataObject.class != newValue.getClass()) {
//                System.out.println("\t*** not a TriggerDataObject");
                return null;
            }

            ISPObsComponent obsComp = (ISPObsComponent) change.getModifiedNode();
            try {
                SPComponentType type = obsComp.getType();
                if (!type.equals(TRIGGER_TYPE)) {
//                    System.out.println("\t*** not a TRIGGER_TYPE");
                    return null;
                }

                TriggerDataObject dobj = (TriggerDataObject) newValue;
//                System.out.println("\t*** message=" + dobj.getTriggerMessage());
                return dobj.getTriggerMessage() == null ? null : obsComp;
            } catch (Exception ex) {
                 LOG.log(Level.WARNING, ex.getMessage(), ex);
            }

            return null;
        }
    }

    public static class TestTriggerAction implements IDBTriggerAction {
        public void doTriggerAction(SPCompositeChange change, Object handback)
                 {

            // Get the trigger message and clear it.
            ISPObsComponent obsComp = (ISPObsComponent) change.getModifiedNode();
            TriggerDataObject tdo = (TriggerDataObject) obsComp.getDataObject();
            String message = tdo.getTriggerMessage();
            tdo.setTriggerMessage(null);
            obsComp.setDataObject(tdo);

            // Add it to the program's record of trigger messages.
            ISPProgram prog = (ISPProgram) change.getModifiedNode().getParent();
            ProgramDataObject pdo = (ProgramDataObject) prog.getDataObject();
            pdo.addTriggerMessage(message);
            prog.setDataObject(pdo);
        }
    }

    private ISPProgram _prog;
    private ISPObsComponent _triggerComp;
    private ISPObsComponent _nonTriggerComp;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        _prog = createProgram();
        _prog.setDataObject(new ProgramDataObject());

        _triggerComp    = _createObsComponent(_prog, TRIGGER_TYPE);
        _nonTriggerComp = _createObsComponent(_prog, NON_TRIGGER_TYPE);

        List<ISPObsComponent> obsCompList = new ArrayList<>();
        obsCompList.add(_triggerComp);
        obsCompList.add(_nonTriggerComp);
        _prog.setObsComponents(obsCompList);
    }

    private ISPObsComponent _createObsComponent(ISPProgram prog, SPComponentType type)
            throws Exception {
        ISPObsComponent obsComp;
        obsComp = getDatabase().getFactory().createObsComponent(prog, type,
                                          new EmptyNodeInitializer<ISPObsComponent, ISPDataObject>(), null);
        obsComp.setDataObject(new TriggerDataObject());
        return obsComp;
    }

    private void _leaseTrigger() throws Exception {
        getDatabase().registerTrigger(new TestTriggerCondition(),
                                      new TestTriggerAction());
    }

    private void _assertMessages(String[] expected) throws Exception {
        ProgramDataObject pdo = (ProgramDataObject) _prog.getDataObject();
        String[] actual = pdo.getTriggerMessages();
        assertEquals(expected.length, actual.length);

        for (int i=0; i<expected.length; ++i) {
            assertEquals(expected[i], actual[i]);
        }
    }

    @Test public void testBasics() throws Exception {
        _leaseTrigger();

        // Start out with no messages.
        _assertMessages(new String[0]);

        // Trigger an event.
        TriggerDataObject tdo = (TriggerDataObject) _triggerComp.getDataObject();
        tdo.setTriggerMessage("message1");
        _triggerComp.setDataObject(tdo);

        // Make sure it happened (after a short delay since it is async ...).
        Thread.sleep(1000);
        _assertMessages(new String[] {"message1"});

        // Do something that shouldn't generate a trigger.
        _nonTriggerComp.setDataObject(tdo);

        // Make sure nothing happened.
        Thread.sleep(1000);
        _assertMessages(new String[] {"message1"});

        // Do a second trigger.
        tdo.setTriggerMessage("message2");
        _triggerComp.setDataObject(tdo);
        Thread.sleep(1000);
        _assertMessages(new String[] {"message1", "message2"});
     }

}
