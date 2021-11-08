package jsky.app.ot.editor.template;

import edu.gemini.phase2.core.odb.TemplateFolderFunctor;
import edu.gemini.phase2.core.odb.TemplateFolderService.BaselineOption;
import edu.gemini.phase2.core.odb.TemplateFolderService.TemplateOption;
import edu.gemini.pot.client.SPDB;
import edu.gemini.pot.sp.*;

import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.core.Peer;
import edu.gemini.spModel.template.Phase1Folder;
import jsky.app.ot.OT;
import jsky.app.ot.editor.template.RegenerationClient.Result;
import jsky.app.ot.userprefs.observer.ObservingPeer;
import jsky.util.gui.Resources;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class RegenerationDialog extends JDialog {
    private static final Logger LOG = Logger.getLogger(RegenerationDialog.class.getName());

    private static final Insets EMPTY_INSETS = new Insets(0,0,0,0);
    private static GridBagConstraints gbc(final int y, final Insets i) {
        return new GridBagConstraints() {{
            gridx=0; gridy=y; anchor=WEST; fill=HORIZONTAL; weightx=1.0; insets=i;
        }};
    }

    private static Font smallerFont(Component c, int reduction) {
        return c.getFont().deriveFont(c.getFont().getSize2D()-reduction);
    }

    // Make a multi-line label by doctoring a JTextArea until it works more or
    // less like a text label.
    private static JTextArea makeNote(final int rows, final int cols) {
        return new JTextArea() {{
                        setColumns(cols);
                        setRows(rows);
                        setEditable(false);
                        setOpaque(false);
                        setLineWrap(true);
                        setWrapStyleWord(true);
                        setHighlighter(null); // turn off selection
                    }};
    }

    private interface OptionGroup {
        String getNoteText();
        String getTitle();
        JPanel getSubOptions();

        TemplateOption getTemplateOption();
        BaselineOption getBaselineOption();
    }

    private static JCheckBox subOption(String title) {
        return new JCheckBox(title) {{
            setFont(smallerFont(this, 2));
            setFocusable(false);
        }};
    }

    private static final class ReplaceOptionGroup implements OptionGroup {
        public String getNoteText() {
            return "Removes all existing templates and replaces them with new copies using the latest observation definitions from Gemini.";
        }

        public String getTitle() {
            return "Replace existing templates";
        }

        private final JCheckBox baselineCheck =
                subOption("Create new baseline calibrations as well");

        private final JPanel subOptions       =
                new JPanel(new BorderLayout()) {{
                    add(baselineCheck, BorderLayout.CENTER);
                    baselineCheck.setSelected(false);
                }};

        public JPanel getSubOptions() { return subOptions; }

        public TemplateOption getTemplateOption() {
            return TemplateOption.REPLACE;
        }

        public BaselineOption getBaselineOption() {
            return baselineCheck.isSelected() ? BaselineOption.ADD : BaselineOption.SKIP;
        }
    }

    public static final class AddOptionGroup implements OptionGroup {
        public String getNoteText() {
            return "Leaves existing templates in place and adds new copies using the latest observation definitions from Gemini.";
        }

        public String getTitle() {
            return "Add a new copy of the templates";
        }

        private final JCheckBox moveCheck     =
                subOption("Move targets and conditions to the new templates");
        private final JCheckBox baselineCheck =
                subOption("Create new baseline calibrations as well");

        private final JPanel subOptions       =
                new JPanel(new BorderLayout()) {{
                    add(moveCheck,     BorderLayout.NORTH);
                    add(baselineCheck, BorderLayout.SOUTH);
                    moveCheck.setSelected(true);
                    baselineCheck.setSelected(false);
                }};

        public JPanel getSubOptions() { return subOptions; }

        public TemplateOption getTemplateOption() {
            return moveCheck.isSelected() ? TemplateOption.ADD_MOVE : TemplateOption.ADD_EMPTY;
        }

        public BaselineOption getBaselineOption() {
            return baselineCheck.isSelected() ? BaselineOption.ADD : BaselineOption.SKIP;
        }
    }

    public static final class BaselineOptionGroup implements OptionGroup {
        public String getNoteText() {
            return "Creates a new copy of your baseline calibrations only, leaving your Templates folder unmodified.";
        }

        public String getTitle() {
            return "Only create new baseline calibrations";
        }

        // no suboptions
        public JPanel getSubOptions() { return new JPanel(); }

        public TemplateOption getTemplateOption() {
            return TemplateOption.SKIP;
        }

        public BaselineOption getBaselineOption() {
            return BaselineOption.ADD;
        }
    }

    private static final class OptionPanel extends JPanel {
        private static final Insets NEW_GROUP  = new Insets(10,0,0,0);
        private static final Insets INDENTOPTS = new Insets(0,19,0,0);

        private final OptionGroup[] optionGroups = new OptionGroup[] {
            new AddOptionGroup(),
            new ReplaceOptionGroup(),
            new BaselineOptionGroup(),
        };

        private OptionGroup selected = optionGroups[0];

        OptionPanel(final JTextArea statusMsg) {
            super(new GridBagLayout());

            final ButtonGroup btnGrp = new ButtonGroup();

            int i = 0;
            for (final OptionGroup grp : optionGroups) {
                final boolean isFirst = i == 0;
                JRadioButton radio = new JRadioButton(grp.getTitle()) {{
                    setFocusable(false);
                    setSelected(isFirst);
                }};
                radio.addActionListener(new ActionListener() {
                    @Override public void actionPerformed(ActionEvent e) {
                        statusMsg.setText(grp.getNoteText());
                        selected = grp;
                    }
                });
                JPanel  subOptions = grp.getSubOptions();
                setChildrenEnabled(subOptions, isFirst);
                enableChildrenWithSelection(radio, subOptions);
                btnGrp.add(radio);

                if (isFirst) statusMsg.setText(grp.getNoteText());

                add(radio,                     gbc(i++, isFirst ? EMPTY_INSETS : NEW_GROUP));
//                add(mkNote(grp.getNoteText()), gbc(i++, INDENT));
                add(subOptions,                gbc(i++, INDENTOPTS));
            }
        }

        private static void enableChildrenWithSelection(final JRadioButton btn, final JComponent children) {
            btn.addItemListener(new ItemListener() {
                @Override public void itemStateChanged(ItemEvent evt) {
                    boolean enabled = evt.getStateChange() == ItemEvent.SELECTED;
                    setChildrenEnabled(children, enabled);
                }
            });
        }

        /*
        private static JTextArea mkNote(final String text) {
            return new JTextArea() {{
                setColumns(45);
                setText(text);
                setEditable(false);
                setOpaque(false);
                setLineWrap(true);
                setWrapStyleWord(true);
                setFont(smallerFont(this, 2));
                setForeground(Color.DARK_GRAY);
            }};
        }
        */

        private static void setChildrenEnabled(JComponent parent, boolean enabled) {
            parent.setEnabled(enabled);
            for (Component child : parent.getComponents()) {
                child.setEnabled(enabled);
            }
        }

        public void disableAll() {
            // set the radio button enabled state
            for (Component child : getComponents()) child.setEnabled(false);

            // set the suboption enabled state
            for (OptionGroup grp : optionGroups) setChildrenEnabled(grp.getSubOptions(), false);
        }

        TemplateOption getTemplateOption() { return selected.getTemplateOption(); }
        BaselineOption getBaselineOption() { return selected.getBaselineOption(); }
    }

    private final OptionPanel options;
    private final JLabel  statusIcon   = new JLabel(Resources.getIcon("eclipse/lightbulb.gif"));
    private final JTextArea statusMsg  = makeNote(0, 35);

    private final JButton actionButton = new JButton("Regenerate");
    private final JButton closeButton  = new JButton("Cancel");

    private final ISPProgram program;
    private final Phase1Folder folder;

    private SwingWorker<Boolean, String> worker;

    private final class Worker extends SwingWorker<Boolean, String> {
        private final TemplateOption templateOption;
        private final BaselineOption baselineOption;

        Worker(TemplateOption templateOption, BaselineOption baselineOption) {
            this.templateOption = templateOption;
            this.baselineOption = baselineOption;
        }

        @Override public Boolean doInBackground() {
            publish("Querying for current template observation information ...");
            final Peer p = ObservingPeer.anyWithSiteOrNull();
            if (p == null) {
                publish("Could not find a peer to contact for template observation expansion.");
                return false;
            }
            Result res = RegenerationClient.expand(folder, program.getProgramID(), p, 10000);
            if (res.isFailure()) {
                publish(res.failure.code.message + " " + res.failure.detail);
                return false;
            }
            publish("Applying the updates to your program ...");

            IDBDatabaseService odb = SPDB.get();
            try {
                TemplateFolderFunctor.store(res.expansion, templateOption, baselineOption, program, odb, OT.getUser());
            } catch (SPException ex) {
                LOG.log(Level.SEVERE, "Unexpected exception working with ODB..", ex);
                throw new RuntimeException(ex);
            }

            // Figure out an appropriate reassuring message to display depending
            // upon what was done.
            final String templateMessage;
            switch (templateOption) {
                case REPLACE: templateMessage = "Replaced templates"; break;
                case SKIP:    templateMessage = ""; break;
                default:      templateMessage = "Added new templates"; break;
            }

            final String baselineMessage;
            switch (baselineOption) {
                case ADD:
                    baselineMessage = "".equals(templateMessage) ?
                            "Regenerated baseline calibrations." :
                            " and regenerated baseline calibrations.";
                    break;
                default:
                    baselineMessage = "".equals(templateMessage) ? "." : ".";
            }

            publish(templateMessage + baselineMessage);
            return true;
        }

        @Override protected void process(List<String> l) {
            // We only care about the last message if multiple ones came in
            // at the same time.
            if (l.size() == 0) return;
            final String message = l.get(l.size() - 1);
            statusIcon.setIcon(Resources.getIcon("spinner16.gif"));
            statusMsg.setText(message);
        }

        @Override
        protected void done() {
            boolean success;
            try {
                success = get();
            } catch (Exception e) {
                success = false;
            }

            String path = success ? "check.png" : "error_tsk.gif";
            statusIcon.setIcon(Resources.getIcon(path));

            closeButton.setEnabled(true);
            closeButton.setText("Dismiss");
        }
    }

    private static JPanel mkVerticalStrut(final int height) {
        return new JPanel() {
            public Dimension getMinimumSize()   { return new Dimension(0, height); }
            public Dimension getMaximumSize()   { return getMinimumSize();         }
            public Dimension getPreferredSize() { return getMinimumSize();         }
        };
    }

    private RegenerationDialog(ISPProgram program, Phase1Folder folder) {
        if (folder == null) throw new IllegalArgumentException("folder is null");
        this.program = program;
        this.folder  = folder;
        this.options = new OptionPanel(statusMsg);

        setTitle("Template and Calibration Regeneration");
        setModal(true);
        setResizable(false);

        final JPanel content = new JPanel(new GridBagLayout()) {{
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 10));
            add(new JLabel("Press 'Regenerate' to"), gbc(0, EMPTY_INSETS));
            add(options,     gbc(1, new Insets(10, 15, 0, 0)));

            JPanel statusPanel = new JPanel(new GridBagLayout()) {{
                add(statusIcon, new GridBagConstraints() {{
                    gridx=0; insets=new Insets(0, 0, 0, 5); anchor=WEST;
                }});
                add(statusMsg,  new GridBagConstraints() {{
                    gridx=1; fill=HORIZONTAL; weightx=1.0; anchor=WEST;
                }});
                // Keep the icon and message from dancing around depending on
                // how many rows it takes.
                add(mkVerticalStrut(32), new GridBagConstraints() {{ gridx=2; }});
            }};
            add(statusPanel, gbc(2, new Insets(10, 0, 5, 5)));

            add(new JPanel(), new GridBagConstraints() {{
                gridy=3; fill=VERTICAL; weighty=1.0;
            }});

            JPanel btnPanel = new JPanel(new GridBagLayout()) {{
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                        BorderFactory.createEmptyBorder(10, 0, 0, 0)
                ));
                add(new JPanel(), gbc(0, EMPTY_INSETS));
                add(actionButton, new GridBagConstraints() {{ gridx = 1; insets=new Insets(0, 0, 0, 5); }});
                add(closeButton,  new GridBagConstraints() {{ gridx = 2; }});
            }};
            add(btnPanel, gbc(4, EMPTY_INSETS));
        }};

        setContentPane(content);

        closeButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { stop(); }
        });
        actionButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { start(); }
        });

        getRootPane().setDefaultButton(closeButton);
    }

    void start() {
        if (worker != null) return;
        options.disableAll();
        actionButton.setEnabled(false);
        worker = new Worker(options.getTemplateOption(), options.getBaselineOption());
        worker.execute();
    }

    void stop() {
        if (worker != null) worker.cancel(true);
        setVisible(false);
        dispose();
    }


    /**
     * Open the template regeneration dialog.
     */
    public static void open(Component parent, ISPProgram program)  {
        final ISPTemplateFolder spFolder = program.getTemplateFolder();
        if (spFolder == null) {
            // Hopefully the UI doesn't even call this method unless there is a
            // Templates folder, but just in case, throw up a warning and quit.
            JOptionPane.showMessageDialog(
                    parent,
                    "This program does not have a Templates folder so its templates cannot be regenerated.",
                    "Cannot Regenerate Templates",
                    JOptionPane.WARNING_MESSAGE,
                    null);
            return;
        }

        final Phase1Folder folder = Phase1Folder.extract(spFolder);
        final RegenerationDialog dialog = new RegenerationDialog(program, folder);

        dialog.pack();
        dialog.setLocationRelativeTo(parent);

        // Inexplicably, we have to pack again or it doesn't take into account
        // the content panel border :-(
        dialog.pack();

        dialog.setVisible(true);
    }
}

