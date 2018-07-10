package edu.gemini.qpt.ui.action;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import edu.gemini.qpt.ui.util.Platform;
import edu.gemini.qpt.ui.util.PreferenceManager;
import edu.gemini.ui.workspace.BooleanStateAction;

@SuppressWarnings("serial")
public class BooleanPreferenceAction extends AbstractAction implements PropertyChangeListener, BooleanStateAction {

    private final PreferenceManager.Key<Boolean> pref;
    private final PreferenceManager.Key<Boolean> disabler;

    public BooleanPreferenceAction(PreferenceManager.Key<Boolean> pref, PreferenceManager.Key<Boolean> disabler, String text) {
        this(pref, disabler, text, 0, 0);
    }

    public BooleanPreferenceAction(PreferenceManager.Key<Boolean> pref, PreferenceManager.Key<Boolean> disabler, String text, int vkey) {
        this(pref, disabler, text, vkey, Platform.MENU_ACTION_MASK);
    }
    
    public BooleanPreferenceAction(PreferenceManager.Key<Boolean> pref, PreferenceManager.Key<Boolean> disabler, String text, int vkey, int modifiers) {
        super(text);
        this.pref = pref;
        this.disabler = disabler;
        setEnabled(disabler == null || !PreferenceManager.get(disabler));
        putValue(BOOLEAN_STATE, PreferenceManager.get(pref));
        if (vkey != 0)
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(vkey, modifiers));
        PreferenceManager.addPropertyChangeListener(pref.name(), this);
        if (disabler != null)
            PreferenceManager.addPropertyChangeListener(disabler.name(), this);
    }

    public void actionPerformed(ActionEvent e) {
        PreferenceManager.set(pref, !PreferenceManager.get(pref));
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (disabler != null) setEnabled(!PreferenceManager.get(disabler));
        putValue(BOOLEAN_STATE,  PreferenceManager.get(pref));
    }

}
