package jsky.app.ot.editor.template;

import edu.gemini.pot.sp.ISPTemplateFolder;
import edu.gemini.spModel.template.TemplateFolder;
import jsky.app.ot.editor.OtItemEditor;
import jsky.util.gui.DialogUtil;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;


public class EdTemplateFolder extends OtItemEditor<ISPTemplateFolder, TemplateFolder> {

    // Immutable members
    private final TemplateFolderPanel panel = new TemplateFolderPanel();

    public JPanel getWindow() {
        return panel;
    }

    public void init() {
        // NOP
    }

    class TemplateFolderPanel extends JPanel {

        private static final String APPLY_DESCRIPTION =
                "<html>Create new observations from a template group and add targets<br>" +
                        "and conditions to the science observations.</html>";

        private static final String REAPPLY_DESCRIPTION =
                "<html>Reapply a template to an existing observation or group.  Target<br> " +
                        "details and position angles are not modified.</html>";

        private static final String REGENERATE_DESCRIPTION =
                "<html>Create new or replace existing template groups and baseline<br>" +
                        "calibrations with the latest definitions from Gemini.</html>";

        // Action to instantiate templates
        private final AbstractAction applyAction = new AbstractAction("Apply Templates...") {
            {
                putValue(SHORT_DESCRIPTION, APPLY_DESCRIPTION);
            }

            public void actionPerformed(ActionEvent evt) {
                try {
                    InstantiationDialog.open(getParent(), getProgram(), getNode());
                } catch (Exception e) {
                    DialogUtil.error(e);
                }
            }
        };

        // Action to instantiate templates
        private final AbstractAction reapplyAction = new AbstractAction("Reapply Templates...") {
            {
                putValue(SHORT_DESCRIPTION, REAPPLY_DESCRIPTION);
            }

            public void actionPerformed(ActionEvent evt) {
                try {
                    ReapplicationDialog.open(getParent(), getProgram(), getNode());
                } catch (Exception e) {
                    DialogUtil.error(e);
                }
            }
        };

        // Action to re-create templates
        private final AbstractAction regenAction = new AbstractAction("Regenerate Templates...") {
            {
                putValue(SHORT_DESCRIPTION, REGENERATE_DESCRIPTION);
            }

            public void actionPerformed(ActionEvent evt) {
                try {
                    RegenerationDialog.open(getParent(), getProgram());
                } catch (Exception e) {
                    DialogUtil.error(e);
                }
            }
        };

        public TemplateFolderPanel() {
            setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
            setLayout(new GridBagLayout());
            add(new JButton(applyAction), new GBC(0, 1));
            add(new JButton(reapplyAction), new GBC(0, 2));
            add(new JButton(regenAction), new GBC(0, 3));
        }

    }

}

