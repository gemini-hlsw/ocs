package jsky.app.ot.gemini.bhros;

import edu.gemini.spModel.gemini.bhros.BHROSParams.*;
import edu.gemini.spModel.gemini.bhros.InstBHROS;
import jsky.app.ot.editor.type.SpTypeComboBoxModel;
import jsky.app.ot.editor.type.SpTypeComboBoxRenderer;
import jsky.app.ot.gemini.bhros.ech.EchellogramDialog;
import jsky.app.ot.gemini.bhros.ech.EchellogramDisplayUnits;
import jsky.util.gui.TextBoxWidget;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;

class BhrosForm extends JPanel {

	// These controls need to be available to the editor component due to
	// superclass requirements. They are manipulated by the framework.
	TextBoxWidget posAngleTextBox, exposureTimeTextBox, coaddsTextBox;

	// We need a handle to this guy internally because it's updated non-locally;
	private JLabel echWavelength, echAlt, echAz, echAng;
	private EdCompInstBHROS edComp;


    //This method will initialize the JComboBox with the elements
    //indicated in Enum class, and will configure the widget to show all the
    //available options.
    private <E extends Enum<E>> void initListBox(JComboBox widget, Class<E> c) {
        SpTypeComboBoxModel<E> model = new SpTypeComboBoxModel<E>(c);
        widget.setModel(model);
        widget.setRenderer(new SpTypeComboBoxRenderer());
        widget.setMaximumRowCount(c.getEnumConstants().length);
    }


    // This code style may seem a little odd, but basically we're just using nested scopes
	// to hook the GUI up, rather than having to name every control with an identifier and
	// then string them together somewhere else. Control is localized and nothing leaks
	// out of the scope where it's used. And the shape of the code mirrors the shape of
	// the component tree that it builds. This is my current theory, anyway.
	public BhrosForm(final EdCompInstBHROS edComp) {
		this.edComp = edComp;

		// Trivial subclass for our specific local use.
		class GBC extends GridBagConstraints {
			{
				fill = HORIZONTAL;
				anchor = EAST;
			}
			public GBC(int gridx, int gridy) {
				this(gridx, gridy, false);
			}
			public GBC(int gridx, int gridy, boolean grab) {
				this.gridx = gridx;
				this.gridy = gridy;
				insets = new Insets(0, 3, 1, 3);
				if (grab)
					this.weightx = 100;
			}
			public GBC(int gridx, int gridy, Insets insets) {
				this(gridx, gridy, 1, 1, insets);
			}
			public GBC(int gridx, int gridy, Insets insets, boolean grab) {
				this(gridx, gridy, 1, 1, insets);
				if (grab)
					this.weightx = 100;
			}
			public GBC(int gridx, int gridy, int anchor) {
				this(gridx, gridy);
				this.anchor = anchor;
			}
			public GBC(int gridx, int gridy, int xspan, int yspan, Insets insets) {
				this(gridx, gridy);
				this.gridwidth = xspan;
				this.gridheight = yspan;
				Insets prev = this.insets;
				this.insets = new Insets(
					prev.top + insets.top,
					prev.left + insets.left,
					prev.bottom + insets.bottom,
					prev.right + insets.right);
			}
		}

		// The whole thing is in a JPanel so the controls won't get pushed all over each
		// other if the window is sized down.
		add(new JPanel() {{

			// Create a border to push it down a little.
			setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

			// The layout is a 4x5 grid, with two columns of { label, control } pairs
			// for the first three rows, and grouped sets of controls in the final row
			setLayout(new GridBagLayout());

			// Left column row 0
			add(new JLabel("Position Angle", JLabel.TRAILING), new GBC(0, 0));
			add(posAngleTextBox = new TextBoxWidget(), new GBC(1, 0));

			// Left column row 1
			add(new JLabel("Exposure Time", JLabel.TRAILING), new GBC(0, 1));
			add(exposureTimeTextBox = new TextBoxWidget(), new GBC(1, 1));

			// Left column row 2
			add(new JLabel("Entrance Fibre", JLabel.TRAILING), new GBC(0, 2));
			add(new JComboBox() {{
				initListBox(this, EntranceFibre.class);
				addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						getInstrument().setEntranceFibre((EntranceFibre)getSelectedItem());
						setEchelleConfigLabels(); // changing the entrance fibre also changes echelle config
					}
				});
				edComp.addPropertyChangeListener(new PropertyChangeListener() {
					public void propertyChange(PropertyChangeEvent arg0) {
						setSelectedIndex(getInstrument().getEntranceFibre().ordinal());
					}
				});
			}}, new GBC(1, 2));

			// Left column row 3 is empty.

			// Right column row 0
			add(new JLabel("Post-Slit Filter", JLabel.TRAILING), new GBC(2, 0));
			add(new JComboBox() {{
				initListBox(this, PostSlitFilter.class);
				addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						getInstrument().setPostSlitFilter((PostSlitFilter)getSelectedItem());
					}
				});
				edComp.addPropertyChangeListener(new PropertyChangeListener() {
					public void propertyChange(PropertyChangeEvent pce) {
						setSelectedIndex(getInstrument().getPostSlitFilter().ordinal());
					}
				});
			}}, new GBC(3, 0));

			// Left column row 1
			add(new JLabel("Hartmann Flap", JLabel.TRAILING), new GBC(2, 1));
			add(new JComboBox() {{
				initListBox(this, HartmannFlap.class);
				addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						getInstrument().setHartmannFlap((HartmannFlap)getSelectedItem());
					}
				});
				edComp.addPropertyChangeListener(new PropertyChangeListener() {
					public void propertyChange(PropertyChangeEvent pce) {
						setSelectedIndex(getInstrument().getHartmannFlap().ordinal());
					}
				});
			}}, new GBC(3, 1));

			// Left column row 2
			add(new JLabel("ISS Port", JLabel.TRAILING), new GBC(2, 2));
			add(new JComboBox() {{
				initListBox(this, ISSPort.class);
				addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						getInstrument().setISSPort((ISSPort)getSelectedItem());
					}
				});
				edComp.addPropertyChangeListener(new PropertyChangeListener() {
					public void propertyChange(PropertyChangeEvent pce) {
						setSelectedIndex(getInstrument().getISSPort().ordinal());
					}
				});
			}}, new GBC(3, 2));

			// Right column row 3
			add(new JLabel("Exp. Meter Filter", JLabel.TRAILING), new GBC(2, 3));
			add(new JComboBox() {{
				initListBox(this, ExposureMeterFilter.class);
				addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						getInstrument().setExposureMeterFilter((ExposureMeterFilter)getSelectedItem());
					}
				});
				edComp.addPropertyChangeListener(new PropertyChangeListener() {
					public void propertyChange(PropertyChangeEvent pce) {
						setSelectedIndex(getInstrument().getExposureMeterFilter().ordinal());
					}
				});
			}}, new GBC(3, 3));

			// Echelle Parameters are grouped
			add(new JPanel() {{


				setBorder(new CompoundBorder(
					new TitledBorder("Echelle Parameters"),
					BorderFactory.createEmptyBorder(0, 5, 5, 5)));

				// Layout here is a 2x5 grid with label+control pairs in one column
				setLayout(new GridBagLayout());

				Insets padding = new Insets(3, 0, 3, 0);

				// Echelle row 0
				add(new JLabel("Tracking Wavelength", JLabel.TRAILING), new GBC(0, 0, padding));
				add(echWavelength = new JLabel("", JLabel.TRAILING), new GBC(1, 0, padding));
				add(new JLabel(EchellogramDisplayUnits.UNIT_ABBREV), new GBC(2, 0, padding));

				// Echelle row 1
				add(new JLabel("Echelle Altitude", JLabel.TRAILING), new GBC(0, 1, padding));
				add(echAlt = new JLabel("", JLabel.TRAILING), new GBC(1, 1, padding));

				// Echelle row 2
				add(new JLabel("Echelle Azimuth", JLabel.TRAILING), new GBC(0, 2, padding));
				add(echAz = new JLabel("",JLabel.TRAILING), new GBC(1, 2, padding));

				// Echelle row 3
				add(new JLabel("Goniometer Angle", JLabel.TRAILING), new GBC(0, 3, padding));
				add(echAng = new JLabel("", JLabel.TRAILING), new GBC(1, 3, padding));

				// Echelle row 4
				add(new JButton("Configure Echelle...") {{

					final JButton button = this;
					addMouseListener(new MouseAdapter() {
						EchellogramDialog ech = null;
						public void mouseClicked(MouseEvent arg0) {

							// We need to initialize the dialog here because the parent
							// hierarchy isn't available until after construction.
							InstBHROS inst = getInstrument();
							if (ech == null) {
								Container c = button.getParent();
								while (!(c instanceof Frame)) {
									c = c.getParent();
								}
								ech = new EchellogramDialog((Frame) c, true, inst.getCentralWavelength(), inst.getEntranceFibre());
							}

							// Set up the dialog and show it.
							ech.setWavelength(inst.getCentralWavelength());
							ech.setCentreXPos(inst.getCCDCentreX());
							ech.setCentreYPos(inst.getCCDCentreY());
							ech.setEntranceFibre(inst.getEntranceFibre());
							ech.setVisible(true);
							inst.setCentralWavelength(ech.getWavelength());
							inst.setCCDCentreX(ech.getCentreXPos());
							inst.setCCDCentreY(ech.getCentreYPos());
							setEchelleConfigLabels();

						}
					});

					edComp.addPropertyChangeListener(new PropertyChangeListener() {
						public void propertyChange(PropertyChangeEvent arg0) {
							setEchelleConfigLabels();
						}
					});

				}

				}, new GBC(0, 4, 3, 1, new Insets(-1, 0, -1, 0)));

			}},  new GBC(0, 4, 2, 1, new Insets(10, 0, 0, 0)) {{
				anchor = NORTH;
			}});

			// CCD Params are also grouped
			add(new JPanel() {{

				setBorder(new CompoundBorder(
					new TitledBorder("CCD Parameters"),
					BorderFactory.createEmptyBorder(0, 5, 5, 5)));

				// Layout here is a 2x5 grid with label+control pairs in one column
				setLayout(new GridBagLayout());

				// CCD row 1
				add(new JLabel("Spacial Binning (X)", JLabel.TRAILING), new GBC(0, 0));
				add(new JComboBox() {{
					initListBox(this, CCDXBinning.class);
					addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							getInstrument().setCCDXBinning((CCDXBinning)getSelectedItem());
						}
					});
					edComp.addPropertyChangeListener(new PropertyChangeListener() {
						public void propertyChange(PropertyChangeEvent pce) {
							setSelectedIndex(getInstrument().getCCDXBinning().ordinal());
						}
					});
				}}, new GBC(1, 0, true));

				// CCD row 2
				add(new JLabel("Spectral Binning (Y)", JLabel.TRAILING), new GBC(0, 1));
				add(new JComboBox() {{
				    initListBox(this, CCDYBinning.class);
					addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							getInstrument().setCCDYBinning((CCDYBinning)getSelectedItem());
						}
					});
					edComp.addPropertyChangeListener(new PropertyChangeListener() {
						public void propertyChange(PropertyChangeEvent pce) {
							setSelectedIndex(getInstrument().getCCDYBinning().ordinal());
						}
					});
				}}, new GBC(1, 1));

				// CCD row 3
				add(new JLabel("Readout Ports", JLabel.TRAILING), new GBC(0, 2));
				add(new JComboBox() {{
					{
						// For now only allow the default value.
						setEnabled(false);
					}
					initListBox(this, CCDReadoutPorts.class);
					addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							getInstrument().setCCDAmplifiers((CCDReadoutPorts)getSelectedItem());
						}
					});
					edComp.addPropertyChangeListener(new PropertyChangeListener() {
						public void propertyChange(PropertyChangeEvent pce) {
							setSelectedIndex(getInstrument().getCCDAmplifiers().ordinal());
						}
					});
				}}, new GBC(1, 2));

				// CCD row 4
				add(new JLabel("Speed", JLabel.TRAILING), new GBC(0, 3));
				add(new JComboBox() {{
					initListBox(this, CCDSpeed.class);
					addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							getInstrument().setCCDSpeed((CCDSpeed)getSelectedItem());
						}
					});
					edComp.addPropertyChangeListener(new PropertyChangeListener() {
						public void propertyChange(PropertyChangeEvent pce) {
							setSelectedIndex(getInstrument().getCCDSpeed().ordinal());
						}
					});
				}}, new GBC(1, 3));

				// CCD row 5
				add(new JLabel("Gain", JLabel.TRAILING), new GBC(0, 4));
				add(new JComboBox() {{
					initListBox(this, CCDGain.class);
					addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							getInstrument().setCCDGain((CCDGain)getSelectedItem());
						}
					});
					edComp.addPropertyChangeListener(new PropertyChangeListener() {
						public void propertyChange(PropertyChangeEvent pce) {
							setSelectedIndex(getInstrument().getCCDGain().ordinal());
						}
					});
				}}, new GBC(1, 4));

			}},  new GBC(2, 4, 2, 1, new Insets(10, 10, 0, 0)) {{
				anchor = NORTH;
			}});

		}});

	}


	// When the instrument itself changes (i.e., the editor component gets reused for
	// another instance) or when the user pops up the Echellogram dialog, we need to
	// update the target and ech labels.
	void setEchelleConfigLabels() {

		final MessageFormat mf2 = new MessageFormat("{0,number,0.000}");
		echWavelength.setText(mf2.format(new Object[] { EchellogramDisplayUnits.fromMicrons(getInstrument().getCentralWavelength()) }));

		echAlt.setText(mf2.format(new Object[] { getInstrument().getEchelleALT() }));
		echAz.setText(mf2.format(new Object[] { getInstrument().getEchelleAz() }));
		echAng.setText(mf2.format(new Object[] { getInstrument().getGoniAng() }));

	}

    private InstBHROS getInstrument() {
        return (InstBHROS) edComp.getContextInstrumentDataObject();
    }


}
