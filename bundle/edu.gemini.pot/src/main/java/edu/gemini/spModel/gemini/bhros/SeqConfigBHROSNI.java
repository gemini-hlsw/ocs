package edu.gemini.spModel.gemini.bhros;

import edu.gemini.pot.sp.ISPFactory;
import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.spModel.config.IConfigBuilder;



public class SeqConfigBHROSNI implements ISPNodeInitializer {

	public void initNode(ISPFactory factory, ISPNode node)  {

		ISPSeqComponent castNode = (ISPSeqComponent) node;
		if (!castNode.getType().equals(SeqConfigBHROS.SP_TYPE)) {
			throw new InternalError();
		}

		node.setDataObject(new SeqConfigBHROS());
		updateNode(node);

	}

	public void updateNode(ISPNode node)  {
		node.putClientData(IConfigBuilder.USER_OBJ_KEY, new SeqConfigBHROSCB((ISPSeqComponent) node));
	}
}
