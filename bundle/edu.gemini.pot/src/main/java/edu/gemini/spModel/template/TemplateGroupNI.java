package edu.gemini.spModel.template;

import edu.gemini.pot.sp.ISPFactory;
import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPNode;


//import java.util.logging.Logger;

public final class TemplateGroupNI implements ISPNodeInitializer {
//    private static final Logger LOG = Logger.getLogger(TemplateFolderNI.class.getName());

    public void initNode(ISPFactory factory, ISPNode node)  {
        TemplateGroup obj = new TemplateGroup();
        node.setDataObject(obj);
    }

    public void updateNode(ISPNode node)  {
        // do nothing for now
    }
}
