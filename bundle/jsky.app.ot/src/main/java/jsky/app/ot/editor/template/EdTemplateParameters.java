package jsky.app.ot.editor.template;

import edu.gemini.pot.sp.ISPTemplateParameters;
import edu.gemini.spModel.template.TemplateParameters;
import jsky.app.ot.editor.OtItemEditor;

import javax.swing.JPanel;


/**
 *
 */
public class EdTemplateParameters extends OtItemEditor<ISPTemplateParameters, TemplateParameters> {

    private final JPanel panel = new JPanel();

    public JPanel getWindow() {
        return panel;
    }

    public void init() {
    }

}
