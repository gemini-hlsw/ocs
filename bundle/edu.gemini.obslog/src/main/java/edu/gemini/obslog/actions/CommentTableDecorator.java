package edu.gemini.obslog.actions;

import edu.gemini.obslog.obslog.ConfigMapUtil;
import org.displaytag.decorator.TableDecorator;

import java.util.Map;

//
// Gemini Observatory/AURA
// $Id: CommentTableDecorator.java,v 1.3 2005/12/11 15:54:15 gillies Exp $
//

public class CommentTableDecorator extends TableDecorator {

    public String getComment() {
        Map m = (Map) getCurrentRowObject();

        String svalue = (String) m.get(ConfigMapUtil.OBSLOG_COMMENT_ITEM_NAME);

        String obsID = (String) m.get("observationID");
        String configID = (String) m.get(ConfigMapUtil.OBSLOG_UNIQUECONFIG_ID);

        String name = obsID + '-' + configID;

        return "<div style='carea'><textarea id=\"" + name + "\" rows=2 cols=25>" + svalue + "</textarea></div>";
    }

}
