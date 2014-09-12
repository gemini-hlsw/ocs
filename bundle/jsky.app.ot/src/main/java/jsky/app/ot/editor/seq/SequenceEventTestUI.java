package jsky.app.ot.editor.seq;

import edu.gemini.pot.client.SPDB;
import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.spModel.dataset.Dataset;
import edu.gemini.spModel.dataset.DatasetLabel;
import edu.gemini.spModel.event.*;
import edu.gemini.spModel.obs.ObsExecEventHandler;
import edu.gemini.spModel.obsrecord.ObsExecRecord;
import edu.gemini.spModel.util.SPTreeUtil;
import jsky.app.ot.editor.OtItemEditor;
import jsky.app.ot.gemini.obslog.test.TestEventGenerator;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * A UI to facilitate monkey testing of the sequence events.
 */
public final class SequenceEventTestUI extends JPanel {

    private interface EventFactory {
        ObsExecEvent create(long time, SPObservationID obsId, ObsExecRecord rec);
    }

    private static final EventFactory START_SEQUENCE_FACTORY = new EventFactory() {
        @Override public ObsExecEvent create(long time, SPObservationID obsId, ObsExecRecord rec) {
            return new StartSequenceEvent(time, obsId);
        }
    };

    private static final EventFactory END_SEQUENCE_FACTORY = new EventFactory() {
        @Override public ObsExecEvent create(long time, SPObservationID obsId, ObsExecRecord rec) {
            return new EndSequenceEvent(time, obsId);
        }
    };

    private static final EventFactory START_DATASET_FACTORY = new EventFactory() {
        @Override public ObsExecEvent create(long time, SPObservationID obsId, ObsExecRecord rec) {
            int index = rec.getDatasetCount() + 1;
            DatasetLabel label = new DatasetLabel(obsId, index);
            Dataset dataset    = new Dataset(label, "/filename" + label.toString(), time);
            return new StartDatasetEvent(time, dataset);
        }
    };

    private static final EventFactory END_DATASET_FACTORY = new EventFactory() {
        @Override public ObsExecEvent create(long time, SPObservationID obsId, ObsExecRecord rec) {
            int index = rec.getDatasetCount();
            DatasetLabel label = new DatasetLabel(obsId, index);
            return new EndDatasetEvent(time, label);
        }
    };

    private static void addEvent(ObsExecEvent evt) {
        ObsExecEventHandler.handle(evt, SPDB.get());
    }

    private OtItemEditor owner;

    private final ActionListener AUTO_FACTORY = new ActionListener() {
        @Override public void actionPerformed(ActionEvent evt) {
            if (owner == null) return;
            TestEventGenerator.test(SPDB.get(), owner.getContextObservation().getObservationID());
        }
    };

    public SequenceEventTestUI() {
        add(new JButton("Auto") {{ addActionListener(AUTO_FACTORY);}});
        add(new JLabel("Seq"));
        add(new JButton("Start") {{ addActionListener(mkListener(START_SEQUENCE_FACTORY)); }});
        add(new JButton("End") {{ addActionListener(mkListener(END_SEQUENCE_FACTORY)); }});
        add(new JLabel("Dataset"));
        add(new JButton("Start") {{ addActionListener(mkListener(START_DATASET_FACTORY)); }});
        add(new JButton("End") {{ addActionListener(mkListener(END_DATASET_FACTORY)); }});
    }

    public void init(OtItemEditor progData) {
        this.owner = progData;
    }

    private ActionListener mkListener(final EventFactory factory) {
        return new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                if (owner == null) return;
                long time = System.currentTimeMillis();
                SPObservationID obsId = owner.getContextObservation().getObservationID();
                ObsExecRecord rec = SPTreeUtil.getObsRecord(owner.getContextObservation());
                addEvent(factory.create(time, obsId, rec));
            }
        };
    }
}
