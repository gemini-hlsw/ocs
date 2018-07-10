package edu.gemini.qpt.ui.view.property;

import java.awt.Color;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JLabel;

import edu.gemini.ui.gface.GSelection;
import edu.gemini.ui.gface.GSubElementDecorator;
import edu.gemini.ui.gface.GViewer;

public class PropertyDecorator implements GSubElementDecorator<GSelection<?>, Map.Entry<String, Object>, PropertyAttribute> {

    public void decorate(JLabel label, Entry<String, Object> element, PropertyAttribute subElement, Object value) {
        label.setForeground(element.getValue() == null ? Color.LIGHT_GRAY : Color.BLACK);
        if (element.getValue() == null && subElement == PropertyAttribute.Value) label.setText("\u00ABMultiple Values\u00BB");
    }

    public void modelChanged(GViewer<GSelection<?>, Entry<String, Object>> viewer, GSelection<?> oldModel, GSelection<?> newModel) {
    }
    

}
