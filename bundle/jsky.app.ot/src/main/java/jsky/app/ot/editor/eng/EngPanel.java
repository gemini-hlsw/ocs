//
// $
//

package jsky.app.ot.editor.eng;

import edu.gemini.shared.gui.bean.ComboPropertyCtrl;
import edu.gemini.shared.gui.bean.PropertyCtrl;
import edu.gemini.spModel.data.ISPDataObject;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

/**
 * An engineering panel that is created generically from instrument
 * property descriptors.
 *
 * <b>Work in progress</b>
 */
public class EngPanel<B extends ISPDataObject> extends JPanel {

    private final Collection<PropertyCtrl<B, ?>> ctrl;

    public EngPanel(Collection<PropertyDescriptor> props) {
        super(new GridBagLayout());

        // Get the expert properties and create property control objects for
        // each one.
        java.util.List<PropertyDescriptor> propList = new ArrayList<PropertyDescriptor>();
        for (PropertyDescriptor pd : props) {
            if (pd.isExpert()) propList.add(pd);
        }

        // Sort them by name.
        Collections.sort(propList, new Comparator<PropertyDescriptor>() {
            public int compare(PropertyDescriptor pd1, PropertyDescriptor pd2) {
                return pd1.getDisplayName().compareTo(pd2.getDisplayName());
            }
        });

        // Add property control objects for each one.
        int row = 0;
        Collection<PropertyCtrl<B, ?>> ctrl = new ArrayList<PropertyCtrl<B, ?>>();
        for (PropertyDescriptor pd : propList) {
            PropertyCtrl<B, ?> pc = null;
            Class c = pd.getPropertyType();
            if (c.isEnum()) {
                pc = new ComboPropertyCtrl<B, Object>(pd);
            }

            if (pc != null) {
                ctrl.add(pc);
//                ComponentEditor.addCtrl(this, 0, row++, pc);
            }
        }

        // Push everything to the top.
        final int finalRow = row;
        add(new JPanel(), new GridBagConstraints(){{
            gridx = 0; gridy = finalRow; weighty=1.0; fill = VERTICAL;
        }});

        this.ctrl = Collections.unmodifiableCollection(ctrl);
    }

    public void setBean(B bean) {
        for (PropertyCtrl<B, ?> pc : ctrl) {
            pc.setBean(bean);
        }
    }

}
