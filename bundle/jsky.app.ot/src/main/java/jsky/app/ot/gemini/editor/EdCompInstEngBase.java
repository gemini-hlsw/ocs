package jsky.app.ot.gemini.editor;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.spModel.data.ISPDataObject;
import jsky.app.ot.editor.OtItemEditor;


/**
 * The base class for all instrument engineering components.  It handles updating
 * the related instrument iterator first row when needed.
 */
public abstract class EdCompInstEngBase<T extends ISPDataObject> extends OtItemEditor<ISPObsComponent, T> {

//    public void init() {
//        SPTreeEditUtil.checkIteratorFirstRow(getContextSeqComponent(), getNode());
//    }

    /**
     * Apply any changes made in this editor (and update any instrument iterators, if needed).
     * The values in the static instrument and engineering components should propagate to the
     * first items in the first instrument iterator in the sequence corresponding to the instrument.
     */
//    public void afterApply() {
//
//        // update iterator, if found
//        final ISPSeqComponent iterSeq =
//                SPTreeUtil.findSeqComponentByNarrowType(
//                        getContextSeqComponent(),
//                        getContextInstrumentDataObject().getNarrowType(), true);
//        if (iterSeq == null) return;
//
//        final ISPDataObject iterDataObj = iterSeq.getDataObject();
//        if (!(iterDataObj instanceof IConfigProvider)) return;
//
//        final IConfigProvider p = (IConfigProvider) iterDataObj;
//        final ISysConfig sysConfig = p.getSysConfig();
//        if (sysConfig == null) return;
//
//        SPTreeEditUtil.updateSysConfigFromDataObject(sysConfig, getDataObject());
//        p.setSysConfig(sysConfig);
//        iterSeq.setDataObject(iterDataObj);
//    }

}

