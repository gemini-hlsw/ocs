package jsky.app.ot.gemini.editor.offset;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.shared.gui.RotatedButtonUI;
import edu.gemini.shared.gui.text.AbstractDocumentListener;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.data.IOffsetPosListProvider;
import edu.gemini.spModel.gemini.seqcomp.SeqRepeatOffsetBase;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.guide.GuideProbeUtil;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.target.offset.OffsetPosBase;
import edu.gemini.spModel.target.offset.OffsetPosList;
import edu.gemini.spModel.target.offset.OffsetPosSelection;
import edu.gemini.spModel.telescope.IssPort;
import edu.gemini.spModel.telescope.IssPortProvider;
import jsky.app.ot.OTOptions;
import jsky.app.ot.editor.OtItemEditor;
import jsky.app.ot.util.OtColor;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static jsky.app.ot.util.OtColor.DARKER_BG_GREY;
import static jsky.app.ot.util.OtColor.LIGHT_GREY;

/**
 * This is the editor for Offset Iterator component.  It allows a list of
 * offset positions to be entered and ordered.
 *
 * @see edu.gemini.spModel.target.offset.OffsetPos
 */
public abstract class AbstractOffsetPosListEditor<P extends OffsetPosBase> extends OtItemEditor<ISPSeqComponent, SeqRepeatOffsetBase<P>> {

    private OffsetPosListEditorConfig<P> config;
    private AbstractOffsetPosTableEditor<P> posTableCtrl;

    private TargetObsComp oldTargetObsComp;
    private final JToggleButton advancedButton;
    private final AdvancedGuiderSelectionEditor<P> guiderSelectionEditor;

    private JPanel editorPanel;

    private final PropertyChangeListener targetListWatcher = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
            updateReferencedGuiders();
        }
    };

    private final DocumentListener titleTextListener = new AbstractDocumentListener() {
        public void textChanged(DocumentEvent docEvent, String newText) {
            getDataObject().setTitle(newText);
        }
    };

    /**
     * The constructor initializes the user interface.
     */
    protected AbstractOffsetPosListEditor() {
        advancedButton = new JToggleButton("Advanced Guiding Options") {{
            setUI(new RotatedButtonUI(RotatedButtonUI.Orientation.topToBottom));
            setBackground(OtColor.VERY_LIGHT_GREY);
        }};
        guiderSelectionEditor = new AdvancedGuiderSelectionEditor<P>();
    }

    protected void init(final OffsetPosListEditorConfig<P> config) {
        this.config = config;
        trackTitleChanges(true);
        posTableCtrl = config.getTableEditor();

        // The content panel holds the editors and the advanced guiding
        // selection options (when opened).
        final JPanel content = new JPanel(new BorderLayout(10, 0)) {{
            add(config.getPan(), BorderLayout.CENTER);
        }};

        // The main container panel for everything.  Holds the content panel
        // and the advanced guiding button.
        editorPanel = new JPanel(new GridBagLayout()) {{
            setBorder(BorderFactory.createEmptyBorder(10, 5, 5, 5));
            add(content, new GridBagConstraints() {{
                gridx = 0;
                gridy = 0;
                weightx = 1.0;
                weighty = 1.0;
                fill = BOTH;
                anchor = NORTHWEST;
            }});
            add(advancedButton, new GridBagConstraints() {{
                gridx = 1;
                gridy = 0;
                anchor = NORTH;
                insets = new Insets(0, 2, 0, 0);
            }});
        }};


        final JPanel wrap = wrap(guiderSelectionEditor.pan);
        advancedButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final JToggleButton tb = (JToggleButton) e.getSource();
                if (tb.isSelected()) {
                    tb.setBackground(OtColor.LIGHT_ORANGE);
                    content.add(wrap, BorderLayout.EAST);
                } else {
                    tb.setBackground(OtColor.VERY_LIGHT_GREY);
                    content.remove(wrap);
                }
                content.validate();
            }
        });
    }

    private void trackTitleChanges(boolean enabled) {
        final Document doc = config.getPan().getTitleTextField().getDocument();
        doc.removeDocumentListener(titleTextListener);
        if (enabled) {
            doc.addDocumentListener(titleTextListener);
        }
    }

    /**
     * Return the window containing the editor
     */
    public JPanel getWindow() {
        return editorPanel;
    }

    /**
     * Apply any changes made in this editor.
     */
    public void afterApply() {
    }

    @Override
    protected void cleanup() {
        if (oldTargetObsComp != null) {
            oldTargetObsComp.removePropertyChangeListener(TargetObsComp.TARGET_ENV_PROP, targetListWatcher);
        }

        oldTargetObsComp = getContextTargetObsCompDataObject();
        if (oldTargetObsComp != null) {
            oldTargetObsComp.addPropertyChangeListener(TargetObsComp.TARGET_ENV_PROP, targetListWatcher);
        }
    }

    /**
     * Set the data object corresponding to this editor.
     */
    public void init() {
        //noinspection unchecked
        final IOffsetPosListProvider<P> sro = getDataObject();

        // Set the title
        trackTitleChanges(false);
        config.getPan().getTitleTextField().setText(sro.getTitle());
        trackTitleChanges(true);

        // Get the current offset list and fill in the table widget
        final OffsetPosList<P> opl = getDataObject().getPosList();

        guiderSelectionEditor.setPosList(opl);

        // need to know if this pos list is editable but the init() method is
        // called from OtItemEditor before the editable state is set up, so
        // compute it here to pass along to the editors.
        final boolean editable = OTOptions.areRootAndCurrentObsIfAnyEditable(getProgram(), getContextObservation());

        posTableCtrl.setPositionList(this, getIssPort(), editable);
        config.getPosEditor().setPositionList(this, editable);

        // Remember the selection
        final List<P> selList = OffsetPosSelection.apply(getNode()).selectedPositions(opl);

        config.getPosEditor().setIssPort(getIssPort());
        updateReferencedGuiders(); // restructures the table, losing the selection

        // Make sure that something gets selected, if the table isn't empty
        if ((selList.size() == 0) && (opl.size() > 0)) {
            selList.add(opl.getPositionAt(0));
        }

        OffsetPosSelection.select(opl, selList).commit(getNode());
    }

    private IssPort getIssPort() {
        final SPInstObsComp inst = getContextInstrumentDataObject();
        return (inst instanceof IssPortProvider) ? ((IssPortProvider) inst).getIssPort() : IssPort.DEFAULT;
    }

    protected void updateReferencedGuiders() {
        // Figure out which of the possible wfs types are in use and the tags
        // that correspond.
        Option<TargetEnvironment> envOpt = None.instance();
        Set<GuideProbe> referenced = Collections.emptySet();
        final TargetObsComp toc = getContextTargetObsCompDataObject();
        if (toc != null) {
            envOpt = new Some<TargetEnvironment>(toc.getTargetEnvironment());
            referenced = envOpt.getValue().getPrimaryGuideGroup().getReferencedGuiders();
        }

        // Make sure that the position links are in sync with the referenced
        // targets. When editing the offset component and adding targets via
        // the TPE, links will be missing since the target component changes
        // haven't been saved yet.  GuideSync will keep this up-to-date when
        // changes to the target env are saved, but the UI doesn't update if
        // you're editing a position list when the target list is updated via
        // the TPE or some other means.
//        OffsetPosList<P> opl = config.getPosEditor().getPositionList();
//        GuideSync.updatePosList(opl, envOpt, oldEnv);

        final ISPObservation ctxObs = getContextObservation();
        final Set<GuideProbe> available = (ctxObs == null) ?
                    Collections.<GuideProbe>emptySet() :
                    GuideProbeUtil.instance.getAvailableGuiders(getContextObservation());
        final Set<GuideProbe> noPrimary = new HashSet<GuideProbe>();
        if (!envOpt.isEmpty()) {
            for (GuideProbeTargets gt : envOpt.getValue().getPrimaryGuideGroup()) {
                if (gt.getPrimary().isEmpty()) {
                    noPrimary.add(gt.getGuider());
                }
            }
        }

        // Update the table to show columns for each in-use category.
        guiderSelectionEditor.setAvailableGuiders(available);
        config.getTableEditor().syncGuideState(referenced, available, noPrimary);
        config.getPosEditor().syncGuideState(available, noPrimary);
    }


    // Wraps the tracking details editor with a border and a lighter background
    // so that it shows.
    private static JPanel wrap(final JPanel pan) {
        return new JPanel(new BorderLayout()) {{
            setBackground(LIGHT_GREY);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(DARKER_BG_GREY),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)
            ));
            add(pan, BorderLayout.CENTER);
            setMinimumSize(new Dimension(200, 0));
            setPreferredSize(new Dimension(200, 0));
        }};
    }

}
