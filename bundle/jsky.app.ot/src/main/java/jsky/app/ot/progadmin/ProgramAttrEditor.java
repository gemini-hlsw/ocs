//
// $
//

package jsky.app.ot.progadmin;

import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.core.Affiliate;
import edu.gemini.spModel.core.ProgramType;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
import edu.gemini.spModel.too.TooType;

import javax.mail.internet.InternetAddress;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.List;
import java.util.Vector;

/**
 * An editor for program attributes.
 */
final class ProgramAttrEditor {
    private class ProgramIdListener implements FocusListener {
        private final ProgramTypeModel pkm;

        private ProgramIdListener(final ProgramTypeModel pkm) {
            this.pkm = pkm;
        }

        public void focusGained(final FocusEvent e) {
        }

        public void focusLost(final FocusEvent e) {
            SPProgramID progId = getProgramId();

            // You can't wipe out the Program Id.
            if ((progId == null) && (model != null)) {
                progId = model.getProgramId();
                if (progId != null) {
                    final JTextField tf;
                    tf = ui.getProgramIdField();
                    tf.setText(progId.toString());
                }
            }

            // Update the program type.
            ProgramTypeInfo type = pkm.getProgramType();

            Option<ProgramType> progTypeOpt = ProgramTypeModel.getProgramType(progId);
            type = type.withProgramType(progTypeOpt);

            if (progTypeOpt.isEmpty() || progTypeOpt.getValue() != ProgramType.Classical$.MODULE$) {
                type = type.withMode(SPProgram.ProgramMode.QUEUE);
                if (type.getQueueBand() <= 0) {
                    type = type.withQueueBand(1);
                }
            } else {
                type = type.withMode(SPProgram.ProgramMode.CLASSICAL);
                type = type.withQueueBand(0);
            }

            pkm.setProgramType(type);
        }
    }

    private class TypeTracker implements ProgramTypeListener {
        public void programTypeChanged(final ProgramTypeEvent event) {
            final ProgramTypeInfo oldType = event.getOldType();
            final ProgramTypeInfo newType = event.getNewType();

            if (oldType.getMode() != newType.getMode()) {
                boolean bandEnabled = true;
                switch (event.getNewType().getMode()) {
                    case CLASSICAL:
                        ui.getClassicalModeButton().setSelected(true);
                        bandEnabled = false;
                        break;
                    case QUEUE:
                        ui.getQueueModeButton().setSelected(true);
                        break;
                }

                Color bg = Color.white;
                if (!bandEnabled) bg = new Color(225, 225, 225);
                ui.getQueueBandField().setEnabled(bandEnabled);
                ui.getQueueBandField().setBackground(bg);
            }

            // band setting only allowed for queue programs
            if (oldType.getQueueBand() != newType.getQueueBand()) {
                String bandText = "";
                final int band = newType.getQueueBand();
                if (band > 0) bandText = String.valueOf(band);
                ui.getQueueBandField().setText(bandText);
            }

            // rollover only allowed for band 1 queue programs
            final boolean enableRollover = (newType.getQueueBand() == 1) && newType.getMode().isQueue();
            if (!enableRollover) ui.getRolloverBox().setSelected(false);
            ui.getRolloverBox().setEnabled(enableRollover);
        }
    }

    private final ProgramAttrUI ui;
    private ProgramAttrModel model;

    public ProgramAttrEditor(final ProgramAttrUI ui, final ProgramTypeModel pkm) {
        this.ui = ui;

        final ProgramIdListener updater = new ProgramIdListener(pkm);
        ui.getProgramIdField().addFocusListener(updater);

        ui.getClassicalModeButton().setFocusable(false);
        ui.getQueueModeButton().setFocusable(false);

        final TypeTracker typeTracker = new TypeTracker();
        pkm.addProgramTypeListener(typeTracker);

        ui.getQueueBandField().addFocusListener(new FocusListener() {
            public void focusGained(final FocusEvent e) {
            }

            public void focusLost(final FocusEvent e) {
                Integer queueBand = getQueueBand();

                final JTextField field;
                field = (ProgramAttrEditor.this).ui.getQueueBandField();
                if (queueBand == null) {
                    queueBand = 0;
                    field.setText("");
                }

                pkm.setProgramType(pkm.getProgramType().withQueueBand(queueBand));
            }
        });

        // Watch the keystrokes in order to enable the band-3 min time box
        // in the time accounting model.
        ui.getQueueBandField().getDocument().addDocumentListener(new DocumentListener() {
            public void handleChange() {
                final Integer queueBand = getQueueBand();
                if (queueBand == null) return;

                // Don't call our program type listener until the focus is
                // lost. Otherwise, it tries to modify the queue band field
                // that is being edited.
                pkm.removeProgramTypeListener(typeTracker);
                pkm.setProgramType(pkm.getProgramType().withQueueBand(queueBand));
                pkm.addProgramTypeListener(typeTracker);
            }

            public void insertUpdate(final DocumentEvent e) { handleChange(); }
            public void removeUpdate(final DocumentEvent e) { handleChange(); }
            public void changedUpdate(final DocumentEvent e) { handleChange(); }
        });

        ui.getClassicalModeButton().addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                ProgramTypeInfo type = pkm.getProgramType();
                type = type.withMode(SPProgram.ProgramMode.CLASSICAL);
                pkm.setProgramType(type.withQueueBand(0));
            }
        });

        ui.getQueueModeButton().addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                ProgramTypeInfo type = pkm.getProgramType();
                type = type.withMode(SPProgram.ProgramMode.QUEUE);
                Integer band = model.getQueueBand();
                if (band == null) band = 1;
                pkm.setProgramType(type.withQueueBand(band));
            }
        });

        ui.getGeminiContactEmailField().addFocusListener(new FocusListener() {
            public void focusGained(final FocusEvent e) {
            }

            public void focusLost(final FocusEvent e) {
                final List<InternetAddress> addrList = getGeminiContactEmails();

                final JTextField field;
                field = (ProgramAttrEditor.this).ui.getGeminiContactEmailField();
                field.setText(ProgramAttrModel.emailsToString(addrList));
            }
        });

        final JComboBox cb = ui.getAffiliateCombo();
        cb.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList jList, final Object o, final int i, final boolean b, final boolean b2) {
                return super.getListCellRendererComponent(jList, o == null ? "None" : ((Affiliate)o).displayValue, i, b, b2);
            }
        });

    }

    protected SPProgramID getProgramId() {
        final String progIdStr = ui.getProgramIdField().getText();
        if ("".equals(progIdStr)) return null;

        SPProgramID progId = null;
        try {
            progId = SPProgramID.toProgramID(progIdStr);
        } catch (Exception ex) {
            // ignore
        }
        return progId;
    }

    protected SPProgram.ProgramMode getProgramMode() {
        SPProgram.ProgramMode mode = SPProgram.ProgramMode.QUEUE;
        if (ui.getClassicalModeButton().isSelected()) {
            mode = SPProgram.ProgramMode.CLASSICAL;
        }
        return mode;
    }

    protected Integer getQueueBand() {
        final JTextField queueBandField = ui.getQueueBandField();
        final String text = queueBandField.getText();

        Integer queueBand = null;
        try {
            queueBand = Integer.parseInt(text);
            if (queueBand <= 0) queueBand = null;
        } catch (Exception ex) {
            // ignore
        }

        return queueBand;
    }

    protected boolean isRollover() {
        return ui.getRolloverBox().isSelected();
    }

    protected TooType getTooType() {
        if (!ui.getTooNoneButton().isEnabled()) return null;

        if (ui.getTooStandardButton().isSelected()) return TooType.standard;
        if (ui.getTooRapidButton().isSelected()) return TooType.rapid;
        return TooType.none;
    }

    protected boolean isThesis() {
        return ui.getThesisBox().isSelected();
    }

    protected boolean isLibrary() {
        return ui.getLibraryBox().isSelected();
    }

    protected List<InternetAddress> getGeminiContactEmails() {
        final JTextField tf = ui.getGeminiContactEmailField();
        final String text = tf.getText();
        return ProgramAttrModel.parseAddresses(text);
    }

    protected Affiliate getAffiliate() {
        return (Affiliate) ui.getAffiliateCombo().getSelectedItem();
    }

    public void setModel(final ProgramAttrModel model) {
        this.model = model;

        String progIdStr = "";
        final SPProgramID progId = model.getProgramId();
        if (progId != null) progIdStr = progId.toString();
        ui.getProgramIdField().setText(progIdStr);

        switch (model.getProgramMode()) {
            case CLASSICAL:
                ui.getClassicalModeButton().setSelected(true);
                break;
            case QUEUE:
                ui.getQueueModeButton().setSelected(true);
                break;
        }

        final Integer queueBand = model.getQueueBand();
        String queueBandText = "";
        if (queueBand != null) queueBandText = queueBand.toString();
        ui.getQueueBandField().setText(queueBandText);

        ui.getRolloverBox().setSelected(model.isRollover());

        TooType type = model.getTooType();
        final boolean enabled = (type != null);
        if (type == null) type = TooType.none;
        switch (type) {
            case none:
                ui.getTooNoneButton().setSelected(true);
                break;
            case standard:
                ui.getTooStandardButton().setSelected(true);
                break;
            case rapid:
                ui.getTooRapidButton().setSelected(true);
                break;
        }
        ui.getTooNoneButton().setEnabled(enabled);
        ui.getTooStandardButton().setEnabled(enabled);
        ui.getTooRapidButton().setEnabled(enabled);

        ui.getThesisBox().setSelected(model.isThesis());
        ui.getLibraryBox().setSelected(model.isLibrary());


        // We need to fill the affiliate list here because we only want to show inactive affiliates for old
        // programs that are associated with inactive affiliate.
        final JComboBox cb = ui.getAffiliateCombo();
        final Vector<Affiliate> affs = new Vector<Affiliate>();
        affs.add(null); // first choice: no affiliate
        for (final Affiliate a: Affiliate.values()) {
            if (a.isActive || model.getAffiliate() == a)
                affs.add(a);
        }
        final DefaultComboBoxModel cbModel = new DefaultComboBoxModel(affs);
        cb.setModel(cbModel);
        cb.setMaximumRowCount(cbModel.getSize());
        cb.setSelectedItem(model.getAffiliate());

        final String emailStr;
        emailStr = ProgramAttrModel.emailsToString(model.getGeminiContactEmails());
        ui.getGeminiContactEmailField().setText(emailStr);
    }

    public ProgramAttrModel getModel() {
        ProgramAttrModel.Builder b = new ProgramAttrModel.Builder();
        b = b.programId(getProgramId()).programMode(getProgramMode());
        b = b.queueBand(getQueueBand());
        b = b.rollover(isRollover());
        b = b.geminiContactEmails(getGeminiContactEmails());
        b = b.affiliate(getAffiliate()).tooType(getTooType()).isThesis(isThesis()).isLibrary(isLibrary());
        return b.build();
    }
}
