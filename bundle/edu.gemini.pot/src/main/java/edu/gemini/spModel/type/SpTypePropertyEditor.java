//
// $Id: SpTypePropertyEditor.java 7030 2006-05-11 17:55:34Z shane $
//

package edu.gemini.spModel.type;

import java.beans.PropertyEditorSupport;

/**
 *
 */
public class SpTypePropertyEditor extends PropertyEditorSupport {
    private Class<? extends Enum>  _enumClass;
    private String[] _tags;

    protected SpTypePropertyEditor(Class<? extends Enum> enumClass) {
        _enumClass = enumClass;
        Enum[] constants = _enumClass.getEnumConstants();
        _tags = new String[constants.length];
        int i = 0;
        for (Enum constant : constants) {
            _tags[i++] = constant.name();
        }
    }

    public String getAsText() {
        return ((Enum) getValue()).name();
    }

    public void setAsText(String string) throws IllegalArgumentException {
        setValue(Enum.valueOf(_enumClass, string));
    }

    public String[] getTags() {
        String[] res = new String[_tags.length];
        System.arraycopy(_tags, 0, res, 0, res.length);
        return res;
    }
}
