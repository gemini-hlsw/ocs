package edu.gemini.qpt.ui.find;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.shared.sp.Obs;
import edu.gemini.qpt.ui.util.ScrollPanes;
import edu.gemini.qpt.ui.view.candidate.ClientExclusion;
import edu.gemini.ui.gface.GSelection;
import edu.gemini.ui.gface.GTableViewer;
import edu.gemini.ui.gface.GViewer;
import edu.gemini.ui.workspace.IShell;
import edu.gemini.ui.workspace.util.Factory;

@SuppressWarnings("serial")
public class FindDialog extends JDialog implements PropertyChangeListener {

    static final int OK_OPTION = JOptionPane.OK_OPTION;
    static final int CANCEL_OPTION = JOptionPane.CANCEL_OPTION;
    
    private int result;
    private final GTableViewer<Schedule, FindElement, FindColumns> viewer = new GTableViewer<Schedule, FindElement, FindColumns>(new FindController());
    private final FindFilter filter = new FindFilter();
    private final JScrollPane scroll = Factory.createStrippedScrollPane(viewer.getTable());
    private JButton reveal;
        
    private FindDialog(IShell owner) {
        super(owner.getPeer(), "Find Candidate", true);
        
        // set up the viewer
        viewer.setColumns(FindColumns.TARGET, FindColumns.ERROR);
        viewer.setColumnSize(FindColumns.TARGET, 150);
        viewer.setColumnSize(FindColumns.ERROR, 200, Integer.MAX_VALUE);
        viewer.setModel((Schedule) owner.getModel());
        viewer.setFilter(filter);
        viewer.setDecorator(new FindDecorator());
        viewer.getTable().setTableHeader(null);
        viewer.addPropertyChangeListener(GViewer.PROP_SELECTION, this);
        viewer.getTable().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && reveal.isEnabled()) {
                    result = OK_OPTION;
                    FindDialog.this.setVisible(false);
                }
            }
        });
        
        scroll.setBorder(new JScrollPane().getBorder()); // ?
        ScrollPanes.setViewportHeight(scroll, 10);
        
        setBackground(Color.LIGHT_GRAY);
        setLayout(new BorderLayout());
        
        add(new JPanel(new BorderLayout(8, 8)) {{
            setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            
            add(new JPanel(new BorderLayout(4, 4)) {{
                add(new JLabel("Enter part of an observation ID like S07A-Q-41 [10] below:"), BorderLayout.NORTH);
                add(new JTextField() {{
                    getDocument().addDocumentListener(new DocumentListener() {
                    
                        public void removeUpdate(DocumentEvent e) {
                            changedUpdate(e);
                        }
                    
                        public void insertUpdate(DocumentEvent e) {
                            changedUpdate(e);
                        }
                    
                        public void changedUpdate(DocumentEvent e) {
                            filter.setPattern(getText());
                        }
                    
                    });
                }}, BorderLayout.CENTER);
            }}, BorderLayout.NORTH);

            
            add(scroll, BorderLayout.CENTER);        

            add(new JPanel(new BorderLayout(0, 0)) {{
                
                add(new JPanel(new FlowLayout(FlowLayout.RIGHT)) {{
                    add(reveal = new JButton("Reveal") {{
                        FindDialog.this.getRootPane().setDefaultButton(this);
                        setEnabled(false);
                        addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                result = OK_OPTION;
                                FindDialog.this.setVisible(false);
                            }                    
                        });
                    }});
                    add(new JButton("Cancel") {{
    //                    NewDialog.this.getRootPane().set;
                        addActionListener(new ActionListener() {                    
                            public void actionPerformed(ActionEvent e) {
                                result = CANCEL_OPTION;
                                FindDialog.this.setVisible(false);
                            }                    
                        });
                    }});
                }}, BorderLayout.EAST);
                            
            }}, BorderLayout.SOUTH);
            
        }});
        
        pack();
        setMinimumSize(getPreferredSize());
        
        

    }
    
    @Override
    public void setVisible(boolean b) {
        setLocationRelativeTo(getParent());
        super.setVisible(b);
    }

    public static FindElement showFind(IShell parent) {
        FindDialog d = new FindDialog(parent);
        d.setVisible(true);
        d.dispose();
        GSelection<FindElement> sel = d.viewer.getSelection();
        return (d.result == CANCEL_OPTION || sel.isEmpty() || !(sel.first().getTarget() instanceof Obs)) ? null : sel.first();
    }

    @SuppressWarnings("unchecked")
    public void propertyChange(PropertyChangeEvent pce) {
        GSelection<FindElement> sel = (GSelection<FindElement>) pce.getNewValue();
        boolean enable = false;
        if (!sel.isEmpty()) {
            FindElement e = sel.first();
            enable = e.getError() == null || e.getError() instanceof ClientExclusion;
        }
        reveal.setEnabled(enable);
    }
    
    protected JRootPane createRootPane() {
        KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        JRootPane rootPane = new JRootPane();
        rootPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                FindDialog.this.setVisible(false);
            }
        }, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
        return rootPane;
    }

}



