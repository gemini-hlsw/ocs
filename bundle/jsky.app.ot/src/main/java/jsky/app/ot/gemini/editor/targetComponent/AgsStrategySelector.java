//
// $
//

package jsky.app.ot.gemini.editor.targetComponent;

import edu.gemini.ags.api.AgsRegistrar;
import edu.gemini.ags.api.AgsStrategy;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.ags.AgsStrategyKey;
import edu.gemini.spModel.obs.SPObservation;
import edu.gemini.spModel.obs.context.ObsContext;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;


/**
 * A simple controller that keeps track of changes to selected guiders in different gui elements and allows to
 * keep all gui elements in sync. This could also be part of either the EdCompTargetList or TelescopePosEditor
 * component but I choose to place the logic here to make it easier to see what's going on.
 */
public final class AgsStrategySelector implements ActionListener {

    private ISPObservation obs;
    private final List<AgsSelectionUi> selectionViews;

    public AgsStrategySelector() {
        selectionViews = new ArrayList<AgsSelectionUi>();
    }

    public void init(ISPObservation obs) {
        this.obs = obs;
        // now notify all views and let them update the available and selected ones
        updateViews(null);
    }

    public ObsContext getContext() {
        return (obs == null) ? null : ObsContext.create(obs).getOrNull();
    }

    public void setVisible(boolean visible) {
        for (AgsSelectionUi view : selectionViews) {
            view.setVisible(visible);
        }
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        if (evt.getSource() instanceof AgsSelectionEditor) {
            AgsSelectionEditor source = (AgsSelectionEditor) evt.getSource();
            updateSelectedAgsStrategy(source.getSelectedAgsStrategy(), source);
        }
    }


    private void addView(AgsSelectionUi view) {
        selectionViews.add(view);
    }

    /**
     * Updates the guide environment of the target obs component of this program.
     * This will make sure that AGS system knows which guiders or ags strategies are selected.
     */
    public void updateSelectedAgsStrategy(Option<AgsStrategy> selectedStrategy) {
        updateSelectedAgsStrategy(selectedStrategy, null);
    }

    private void updateSelectedAgsStrategy(Option<AgsStrategy> selectedStrategy, AgsSelectionUi source) {
        if (obs == null) return;

        final SPObservation spObs = (SPObservation) obs.getDataObject();

        scala.Option<AgsStrategy> def = AgsRegistrar.defaultStrategy(getContext());
        if (selectedStrategy.isEmpty() || def.isEmpty() || selectedStrategy.getValue().equals(def.get())) {
            // Here we store an empty set whenever the default value would be
            // the same as the explicit provided value.  We have to be
            // consistent so that there isn't a spurious difference between
            // a target environment with no "selected guider" and one with the
            // guider that happens to correspond to the default strategy.
            spObs.setSelectedAgsStrategy(None.<AgsStrategyKey>instance());
            obs.setDataObject(spObs);
        } else {
            final Option<AgsStrategyKey> selectedStrategyKey = selectedStrategy.map(new MapOp<AgsStrategy, AgsStrategyKey>() {
                @Override public AgsStrategyKey apply(AgsStrategy agsStrategy) {
                    return agsStrategy.key();
                }
            });

            if (!spObs.getSelectedAgsStrategy().equals(selectedStrategyKey)) {
                spObs.setSelectedAgsStrategy(selectedStrategyKey);
                obs.setDataObject(spObs);
            }
        }
        updateViews(source);
    }

    public static Option<AgsStrategy> getSelectedOrDefault(Option<ObsContext> ctx) {
        return ctx.flatMap(new MapOp<ObsContext, Option<AgsStrategy>>() {
            @Override
            public Option<AgsStrategy> apply(ObsContext obsContext) {
                return getSelectedOrDefault(obsContext);
            }
        });
    }

    private static <T> Option<T> toJavaOption(scala.Option<T> optT) {
        return optT.isDefined() ? new Some<T>(optT.get()) : None.<T>instance();
    }

    public static Option<AgsStrategy> getSelectedOrDefault(ObsContext ctx) {
        final Option<AgsStrategyKey> sel = ctx.getSelectedAgsStrategy();
        if (sel.isEmpty()) {
            return AgsStrategySelector.<AgsStrategy>toJavaOption(AgsRegistrar.defaultStrategy(ctx));
        } else {
            return AgsStrategySelector.<AgsStrategy>toJavaOption(AgsRegistrar.lookup(sel.getValue()));
        }
    }

    private void updateViews(AgsSelectionUi source) {
        final ObsContext ctx = getContext();
        if (ctx == null) {
            for (AgsSelectionUi view : selectionViews) {
                view.setAgsStrategyOptions(null, ImCollections.<AgsStrategy>emptyList(), None.<AgsStrategy>instance());
            }
            return;
        }

        final ImList<AgsStrategy> options  = DefaultImList.create(AgsRegistrar.validStrategiesAsJava(ctx));
        final Option<AgsStrategy> selected = getSelectedOrDefault(ctx);

        // now update all views
        for (AgsSelectionUi view : selectionViews) {
            if (view != source) {
                view.setAgsStrategyOptions(ctx, options, selected);
            }
        }
    }

    /**
     * Gets a tooltip text that displays the cornerstones of the ags strategy: the magnitude and radius
     * limits which will be used for the guide star search.
     *
     * @param strategy
     * @param ctx
     * @return
     */
    private static String createToolTipText(ObsContext ctx, AgsStrategy strategy) {
        if (ctx == null) return "Cannot perform an AGS search for this observation";

        // Get the query constraint from the Ags.Strategy. This returns a list of such constraints: we assume right
        // now that there is exactly one, which contains the magnitude limits and the radius limits.
        /*
        final StringBuilder sb = new StringBuilder();
        QueryConstraint queryConstraint = strategy.queryConstraints(ctx).get(0);
        MagnitudeLimits magnitudeLimits = queryConstraint.magnitudeLimits;
        RadiusLimits radiusLimits = queryConstraint.radiusLimits;

        sb.append(magnitudeLimits.toString());
        sb.append("; ");
        sb.append(String.format("%.2f'", radiusLimits.getMinLimit().toArcmins().getMagnitude()));
        sb.append(" - ");
        sb.append(String.format("%.2f'", radiusLimits.getMaxLimit().toArcmins().getMagnitude()));
        return sb.toString();
        */
        return "Perform AGS search for " + strategy.key().displayName();
    }

    private static final Comparator<AgsStrategy> DISPLAY_NAME_COMPARATOR = new Comparator<AgsStrategy>() {
        @Override
        public int compare(AgsStrategy as1, AgsStrategy as2) {
            return as1.key().displayName().compareTo(as2.key().displayName());
        }
    };

    private static ImList<AgsStrategy> getSortedAgsStrategies(ImList<AgsStrategy> strategies) {
        return strategies.sort(DISPLAY_NAME_COMPARATOR);
    }

    // =======

    interface AgsSelectionUi {
        void setAgsStrategyOptions(ObsContext ctx, ImList<AgsStrategy> options, Option<AgsStrategy> selectedStrategy);
        void setVisible(boolean visible);
    }

    interface AgsSelectionEditor extends AgsSelectionUi {
        Option<AgsStrategy> getSelectedAgsStrategy();
    }

    public final class Panel extends JPanel implements AgsSelectionEditor, ActionListener {
        private final JLabel label;
        private List<Tuple2<JRadioButton, AgsStrategy>> buttons;
        private ButtonGroup buttonGroup;

        public Panel() {
            setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
            this.label = new JLabel("Auto GS Options");
            this.buttonGroup = new ButtonGroup();
            this.buttons = new ArrayList<Tuple2<JRadioButton, AgsStrategy>>();
            AgsStrategySelector.this.addView(this);
        }

        @Override
        public void setAgsStrategyOptions(ObsContext ctx, ImList<AgsStrategy> options, Option<AgsStrategy> selectedStrategy) {
            // clear current strategies from gui
            remove(label);
            for (Tuple2<JRadioButton, AgsStrategy> button : buttons) {
                remove(button._1());
            }

            if ((ctx != null) && (options.size() > 0)) {
                add(label);
                addStrategies(ctx, options, selectedStrategy);
            }

            // update this gui element (including the parent)
            getParent().validate();
            getParent().repaint();
            validate();
            repaint();
        }

        @Override
        public Option<AgsStrategy> getSelectedAgsStrategy() {
            for (Tuple2<JRadioButton, AgsStrategy> button : buttons) {
                if (button._1().isSelected()) {
                    return new Some<AgsStrategy>(button._2());
                }
            }
            return None.instance();
        }

        @Override
        public void setVisible(boolean visible) {
            super.setVisible(visible);
            label.setVisible(visible);
            for (Tuple2<JRadioButton, AgsStrategy> button : buttons) {
                button._1().setVisible(visible);
            }
        }

        private void addStrategies(final ObsContext ctx, final ImList<AgsStrategy> options, final Option<AgsStrategy> selectedStrategy) {
            // add radio buttons to gui
            this.buttonGroup = new ButtonGroup();
            this.buttons = getSortedAgsStrategies(options).map(new MapOp<AgsStrategy, Tuple2<JRadioButton, AgsStrategy>>() {
                @Override
                public Tuple2<JRadioButton, AgsStrategy> apply(AgsStrategy strategy) {
                    final JRadioButton button = new JRadioButton(strategy.key().displayName());
                    button.addActionListener(Panel.this);
                    button.setToolTipText(createToolTipText(ctx, strategy));
                    buttonGroup.add(button);
                    add(button);
                    button.setSelected(
                      !selectedStrategy.isEmpty() && selectedStrategy.getValue().equals(strategy)
                    );

                    return new Pair<JRadioButton, AgsStrategy>(button,strategy);
                }
            }).toList();
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            // a button has been clicked -> inform controller for propagation
            AgsStrategySelector.this.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "changed"));
        }
    }

    final class ComboBox extends JComboBox<String> implements AgsSelectionEditor, ActionListener {
        public ComboBox() {
            AgsStrategySelector.this.addView(this);

            setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    final JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    final AgsStrategy s = (AgsStrategy) value;
                    label.setText(s.key().displayName());
                    return label;
                }
            });
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            // a selection has been made -> inform controller for propagation
            AgsStrategySelector.this.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "changed"));
        }

        @Override
        public void setAgsStrategyOptions(ObsContext ctx, ImList<AgsStrategy> options, Option<AgsStrategy> selectedStrategy) {
            removeActionListener(this);
            setToolTipText("");
            removeAllItems();

            final DefaultComboBoxModel cbm = new DefaultComboBoxModel(new Vector<AgsStrategy>(getSortedAgsStrategies(options).toList()));
            selectedStrategy.foreach(new ApplyOp<AgsStrategy>() {
                @Override public void apply(AgsStrategy strategy) {
                    cbm.setSelectedItem(strategy);
                }
            });
            setModel(cbm);

            addActionListener(this);
        }

        @Override
        public Option<AgsStrategy> getSelectedAgsStrategy() {
            return ImOption.apply((AgsStrategy) getModel().getSelectedItem());
        }
    }
}
