package jsky.app.ot.gemini.editor.targetComponent.detail;

import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.system.ConicTarget;
import edu.gemini.spModel.target.system.CoordinateParam;
import edu.gemini.spModel.target.system.ITarget;
import jsky.app.ot.gemini.editor.targetComponent.MagnitudeEditor;
import jsky.util.gui.NumberBoxWidget;
import jsky.util.gui.TextBoxWidget;
import jsky.util.gui.TextBoxWidgetWatcher;

import javax.swing.*;
import java.awt.*;

final class JplMinorBodyDetailEditor extends TargetDetailEditor {

    private final ParamPanel.ParamInfo param_epoch = new ParamPanel.ParamInfo("EPOCH", "Orbital Element Epoch", "JD");
    private final ParamPanel.ParamInfo param_in    = new ParamPanel.ParamInfo("IN",    "Inclination", "deg");
    private final ParamPanel.ParamInfo param_om    = new ParamPanel.ParamInfo("OM",    "Longitude of Ascending Node", "deg");
    private final ParamPanel.ParamInfo param_w     = new ParamPanel.ParamInfo("W",     "Argument of Perihelion", "deg");
    private final ParamPanel.ParamInfo param_qr    = new ParamPanel.ParamInfo("QR",    "Perihelion Distance", "AU");
    private final ParamPanel.ParamInfo param_ec    = new ParamPanel.ParamInfo("EC",    "Eccentricity", null);
    private final ParamPanel.ParamInfo param_tp    = new ParamPanel.ParamInfo("TP",    "Time of Perihelion Passage", "JD");


    private final MagnitudeEditor magnitudeEditor = new MagnitudeEditor();
    {
        ((JComponent) magnitudeEditor.getComponent()).setBorder(titleBorder("Magnitudes"));
    }

    final ParamPanel pp = new ParamPanel(new ParamPanel.ParamInfo[] {
        param_epoch, param_in, param_om, param_w, param_qr, param_ec, param_tp
    });
    {
        pp.setBorder(titleBorder("Orbital Elements"));
    }

    private boolean updating = false;
    private SPTarget target = null;

    JplMinorBodyDetailEditor() {
        super(ITarget.Tag.JPL_MINOR_BODY);
        setLayout(new GridBagLayout());

        // Add the magnitude editor
        final GridBagConstraints mec = new GridBagConstraints();
        mec.gridx = 0;
        mec.gridy = 0;
        mec.fill = GridBagConstraints.BOTH;
        add(magnitudeEditor.getComponent(), mec);

        // Add the param panel
        final GridBagConstraints pc = new GridBagConstraints();
        pc.gridx = 1;
        pc.gridy = 0;
        pc.fill = GridBagConstraints.HORIZONTAL;

        add(pp, pc);

        // Valid at panel

        // Add the listeners
        param_epoch.widget.addWatcher(new LocalPropSetter() {
            CoordinateParam getParam(ConicTarget target) {
                return target.getEpoch();
            }
        });
        param_in.widget.addWatcher(new LocalPropSetter() {
            CoordinateParam getParam(ConicTarget target) {
                return target.getInclination();
            }
        });
        param_om.widget.addWatcher(new LocalPropSetter() {
            CoordinateParam getParam(ConicTarget target) {
                return target.getANode();
            }
        });
        param_w.widget.addWatcher(new LocalPropSetter() {
            CoordinateParam getParam(ConicTarget target) {
                return target.getPerihelion();
            }
        });
        param_qr.widget.addWatcher(new LocalPropSetter() {
            CoordinateParam getParam(ConicTarget target) {
                return target.getAQ();
            }
        });
        param_tp.widget.addWatcher(new LocalPropSetter() {
            CoordinateParam getParam(ConicTarget target) {
                return target.getEpochOfPeri();
            }
        });

        // N.B. there is no CoordinateParam for eccentricity
        param_ec.widget.addWatcher(new TextBoxWidgetWatcher() {
            public final void textBoxKeyPress(TextBoxWidget tbwe) {
                textBoxAction(tbwe);
            }
            public final void textBoxAction(TextBoxWidget tbwe) {
                if (target != null) {
                    try {
                        updating = true;
                        ConicTarget ct = ((ConicTarget) target.getTarget());
                        if (ct != null) {
                            ct.setE(Double.parseDouble(tbwe.getValue()));
                            target.notifyOfGenericUpdate();
                        }
                    } catch (NumberFormatException nfe) {
                        // ignore
                    } finally {
                        updating = false;
                    }
                }
            }
        });

    }

    public void edit(final Option<ObsContext> obsContext, final SPTarget spTarget) {
        super.edit(obsContext, spTarget);
        magnitudeEditor.edit(obsContext, spTarget);

        // Local updates
        this.target = spTarget;
        if (!updating) {
            final ConicTarget ct = (ConicTarget) spTarget.getTarget();
            param_epoch.widget.setValue(ct.getEpoch().getValue());
            param_in.widget.setValue(ct.getInclination().getValue());
            param_om.widget.setValue(ct.getANode().getValue());
            param_w.widget.setValue(ct.getPerihelion().getValue());
            param_qr.widget.setValue(ct.getAQ().getValue());
            param_ec.widget.setValue(ct.getE());
            param_tp.widget.setValue(ct.getEpochOfPeri().getValue());
        }

    }

    abstract class LocalPropSetter implements TextBoxWidgetWatcher {
        abstract CoordinateParam getParam(ConicTarget target);
        public final void textBoxKeyPress(TextBoxWidget tbwe) {
            textBoxAction(tbwe);
        }
        public final void textBoxAction(TextBoxWidget tbwe) {
            if (target != null) {
                try {
                    updating = true;
                    final CoordinateParam cp = getParam((ConicTarget) target.getTarget());
                    if (cp != null) {
                        cp.setValue(Double.parseDouble(tbwe.getValue()));
                        target.notifyOfGenericUpdate();
                    }
                } catch (NumberFormatException nfe) {
                    // ignore
                } finally {
                    updating = false;
                }
            }
        }
    }



}






final class ParamPanel extends JPanel {

    static final class ParamInfo {

        public final NumberBoxWidget widget = new NumberBoxWidget() {{
            setColumns(10);
            setMinimumSize(getPreferredSize());
        }};

        public final JLabel shortLabel = new JLabel();
        public final JLabel longLabel  = new JLabel();

        public ParamInfo(String shortLabel, String longLabel, String units) {
            this.shortLabel.setText(shortLabel);
            this.longLabel.setText(units != null ? (longLabel + " (" + units + ")") : longLabel);
        }
    }

    private static final Insets ins = new Insets(0, 2, 0, 2);

    private static GridBagConstraints slc(int row) {
        final GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = ins;
        return c;
    }

    private static GridBagConstraints nbc(int row) {
        final GridBagConstraints c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = row;
        c.insets = ins;
        return c;
    }

    private static GridBagConstraints llc(int row) {
        final GridBagConstraints c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = row;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 2;
        c.insets = ins;
        return c;
    }

    public ParamPanel(ParamInfo[] nbs) {
        setLayout(new GridBagLayout());
        for (int i = 0; i < nbs.length; i++) {
            add(nbs[i].shortLabel, slc(i));
            add(nbs[i].widget, nbc(i));
            add(nbs[i].longLabel, llc(i));
        }
    }

};