package edu.gemini.spModel.template;

import edu.gemini.pot.sp.ISPFactory;
import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPNodeInitializer;


//import java.util.logging.Logger;

public final class TemplateFolderNI implements ISPNodeInitializer {
//    private static final Logger LOG = Logger.getLogger(TemplateFolderNI.class.getName());

    public void initNode(ISPFactory factory, ISPNode node)  {
        TemplateFolder obj = new TemplateFolder();
        node.setDataObject(obj);
    }

    public void updateNode(ISPNode node)  {
        // do nothing for now
    }
}
