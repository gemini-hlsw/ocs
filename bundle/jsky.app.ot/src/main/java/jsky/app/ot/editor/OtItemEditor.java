package jsky.app.ot.editor;

import edu.gemini.pot.sp.*;
import edu.gemini.shared.gui.calendar.JCalendarPopup;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.telescope.IssPort;
import edu.gemini.spModel.telescope.IssPortProvider;
import jsky.app.ot.OTOptions;
import jsky.app.ot.viewer.SPViewer;
import org.jdesktop.swingx.JXTreeTable;

import javax.swing.*;
import javax.swing.plaf.basic.BasicArrowButton;
import java.awt.Component;
import java.awt.Container;
import java.util.*;
import java.util.function.Predicate;


/**
 * Abstract base class for all OT item editors.
 */
public abstract class OtItemEditor<N extends ISPNode, T extends ISPDataObject> {

    private boolean _enabled = true;
    private N _node;
    private T _dataObject;

    // Holds expected event ids generated from editing in this component.
    // Generally when the node being edited is changed (or the node is replaced
    // with a new one), the editor should reinitialize itself to show the
    // current values.  However, events that the editor itself creates should
    // not cause the editor to reinitialize.  We record event ids in the apply()
    // method for this purpose.  When init() is called with the event that this
    // generates, we can see if it was caused by the apply() and if so ignore
    // it.  We use a Set because the property change events are sent later and
    // so multiple edits can be performed before the first one generates a
    // property change.
    private final Set<PropagationId> _propIdSet = new HashSet<>();

    // used to save and restore previous enabled states of widgets
    private final Hashtable<Component, Boolean> _enabledTab = new Hashtable<>();

    /**
     * Constructor
     */
    public OtItemEditor() {
    }

    /**
     * Update the enabled (editable) state of this editor.
     * The default implementation just enables or disables all components in
     * the editor window.
     */
    protected void updateEnabledState(final boolean enabled) {
        if (enabled != isEnabled()) {
            setEnabled(enabled);
            updateEnabledState(getWindow().getComponents(), enabled);
        }
    }

    /**
     * Update the enabled (editable) state of the given components.
     */
    protected void updateEnabledState(final Component[] ar, final boolean enabled) {
        for (final Component component : ar) {
            if (!((component instanceof JTabbedPane)
                    || (component instanceof JSplitPane)
                    || (component instanceof JLabel)
                    || (component instanceof JXTreeTable)
                    || (component instanceof BasicArrowButton)
                    || (component instanceof JScrollBar)
                    || (component instanceof JCalendarPopup))) {

                // If the enabled state is different from the previous state, change.
                boolean prevEnabled = Optional.ofNullable(getEnabledTable().get(component)).orElse(true);
                if (prevEnabled != enabled) {
                    component.setEnabled(enabled);
                    getEnabledTable().put(component, component.isEnabled());
                }
            }

            if (component instanceof Container && !(component instanceof JComboBox) && !(component instanceof JSpinner)) {
                updateEnabledState(((Container) component).getComponents(), enabled);
            }
        }
    }


    /**
     * Initialize the OT item editor with the given science program and
     * and viewable.
     *
     * @param node the item being edited
     */
    @SuppressWarnings("unchecked")
    public final void init(PropagationId propId, N node) {
        // If we generated this update as a result of a call to apply(), then
        // skip the reinitialization.
        // TODO: okay maybe we shouldn't just skip it all together (updateEnabledSate regardless?)
        if ((_node != null) && _propIdSet.remove(propId))
            return;

        _propIdSet.clear();
        if (_node != null)
            cleanup();

        this._node = node;
        this._dataObject = (_node != null) ? (T) _node.getDataObject() : null;
        final boolean editable = OTOptions.areRootAndCurrentObsIfAnyEditable(getProgram(), getContextObservation());
        init();
        updateEnabledState(editable);
    }


    /**
     * Subclasses should use this to perform initialization. The viewable and data object will have
     * been set, but enabled state calculations have not yet happened.
     */
    protected abstract void init();

    /**
     * Subclasses can override to remove listeners and clean up resources associated with the
     * current viewable and data object. This method will only be called if the current viewable is
     * non-null.
     * The default implementation does nothing.
     */
    protected void cleanup() {
    }

    /**
     * Subclasses can override to provide additional processing after apply() is called.
     * Default implementation does nothing.
     */
    protected void afterApply() {
    }

    protected void beforeApply() {
    }

    /**
     * Re-initialize this editor, discarding any in-progess edits.
     */
    public final void reinitialize(final PropagationId propId) {
        init(propId, _node);
    }

    /**
     * Apply any changes made in this editor.
     */
    public final void apply() {
        beforeApply();
        _propIdSet.add(getNode().setDataObject(_dataObject));
        afterApply();
    }

    /**
     * Return the window containing the editor.
     */
    public abstract JPanel getWindow();

    /**
     * Returns the viewer that contains this editor (by searching up the parent
     * hierarchy).
     */
    public SPViewer getViewer() { return getViewer(getWindow().getParent()); }
    private SPViewer getViewer(final Container c) {
        return (c == null) ? null : ((c instanceof SPViewer) ? (SPViewer) c : getViewer(c.getParent()));
    }

    public final T getDataObject() {
        return _dataObject;
    }

    public final N getNode() {
        return _node;
    }

    /**
     * The current science program, if any.
     */
    public ISPProgram getProgram() {
        return _node.getProgram();
    }

    /**
     * The current science program, if any.
     */
    public SPProgram getProgramDataObject() {
        final ISPProgram root = getProgram();
        return (root != null) ? (SPProgram) root.getDataObject() : null;
    }

    /**
     * Is this a library program?.
     */
    protected boolean isLibraryProgram() {
        final ISPProgram p = getProgram();
        return (p != null) && ((SPProgram) p.getDataObject()).isLibrary();
    }

    /**
     * The current enabled state
     */
    protected boolean isEnabled() {
        return _enabled;
    }

    protected void setEnabled(final boolean enabled) {
        this._enabled = enabled;
    }

    protected Hashtable<Component, Boolean> getEnabledTable() {
        return _enabledTab;
    }

    public ISPObservation getContextObservation() {
        final ISPNode n = getNode();
        return (n != null) ? n.getContextObservation() : null;
    }

    private Optional<ISPObsComponent> findObsComponent(final Predicate<ISPObsComponent> p) {
        return Optional.ofNullable(getContextObservation())
                .map(ISPObservation::getObsComponents).orElse(Collections.emptyList())
                .stream().filter(p).findFirst();
    }

    public ISPObsComponent getContextInstrument() {
        return findObsComponent(c -> c.getType().broadType == SPComponentBroadType.INSTRUMENT).orElse(null);
    }

    public SPInstObsComp getContextInstrumentDataObject() {
        final ISPObsComponent o = getContextInstrument();
        return (o != null) ? (SPInstObsComp) o.getDataObject() : null;
    }

    public IssPort getContextIssPort() {
        final T o = getDataObject();
        return (o instanceof IssPortProvider) ? ((IssPortProvider) o).getIssPort() : IssPort.DEFAULT;
    }

    public ISPObsComponent getContextTargetObsComp() {
        return findObsComponent(c -> c.getType() == SPComponentType.TELESCOPE_TARGETENV).orElse(null);
    }

    public SPSiteQuality getContextSiteQuality() {
        return findObsComponent(c -> c.getType() == SPComponentType.SCHEDULING_CONDITIONS)
                .map(s -> (SPSiteQuality) s.getDataObject()).orElse(null);
    }

    public TargetObsComp getContextTargetObsCompDataObject() {
        final ISPObsComponent o = getContextTargetObsComp();
        return (o != null) ? (TargetObsComp) o.getDataObject() : null;
    }

    public TargetEnvironment getContextTargetEnv() {
        final TargetObsComp o = getContextTargetObsCompDataObject();
        return (o != null) ? o.getTargetEnvironment() : null;
    }

}
