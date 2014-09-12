package jsky.app.ot.gemini.bhros.ech;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.text.MessageFormat;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JViewport;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.gemini.spModel.gemini.bhros.BHROSParams;
import edu.gemini.spModel.gemini.bhros.BHROSParams.EntranceFibre;
import edu.gemini.spModel.gemini.bhros.ech.HROSHardwareConstants;

/**
 * A control that displays an Echellogram and allows the user to interact with it.
 * This control mostly just wraps an EchellogramCanvas, adding a zoom control and some
 * readouts.
 */
public class EchellogramControl extends JPanel implements HROSHardwareConstants {

	private static final Color COLOR_BORDER = Color.GRAY;

	public static final int ZOOM_MIN = 100;
	public static final int ZOOM_MAX = 1000;

	private static final String TEXT_BORDER_VIEW_CONFIG = "Echelle View & Config";
	private static final String TEXT_BORDER_TARGET_POSITION = "Target Position";

	private static final String TEXT_LABEL_ZOOM = "Zoom:";
	private static final String TEXT_LABEL_REFERENCE = "Reference Lines:";
	private static final String TEXT_LABEL_LIMITS = "Detected Range:";
	private static final String TEXT_LABEL_ECHELLE = "Echelle Settings:";
	private static final String TEXT_LABEL_WARNINGS = "Warnings:";

	private static final String TEXT_WARNINGS_NONE = "(none)";

	private static final String HTML_LABEL_TARGET = "<html><font color=\"blue\"><u>Tracking Wavelength</u></font>:</html>";

	private static final String TEXT_ITEM_REFERENCE_NONE = "None";
	private static final String TEXT_ITEM_REFERENCE_ASTRO = "Astronomical";
	private static final String TEXT_ITEM_REFERENCE_CALIB = "Calibration";
	private static final String TEXT_ITEM_REFERENCE_USER = "User File...";

	public static final ReferenceLine[] REFLINES_ASTRO = ReferenceLine.readArray(EchellogramControl.class.getResourceAsStream("/resources/conf/astroLines.dat"), true);
	public static final ReferenceLine[] REFLINES_CALIB = ReferenceLine.readArray(EchellogramControl.class.getResourceAsStream("/resources/conf/calibLines.dat"), false);


	private static final String[] TEXT_ITEMS_REFERENCE = {
		TEXT_ITEM_REFERENCE_NONE,
		TEXT_ITEM_REFERENCE_ASTRO,
		TEXT_ITEM_REFERENCE_CALIB,
		TEXT_ITEM_REFERENCE_USER,
	};

	private static final String FORMAT_ECHELLE_AZ_ALT_ANG = "alt: {1,number,0.0000}, az: {0,number,0.0000}, ang: {2,number,0.0000}";

	private static final String FORMAT_TARGET_ORD_WAVEL =
		"{1,number," + EchellogramDisplayUnits.FORMAT_STRING + "}" + EchellogramDisplayUnits.UNIT_ABBREV + " ({0,number}), x = {2,number,0.00}, y = {3,number,0.00}, {4}";
//		"{1,number," + EchellogramDisplayUnits.FORMAT_STRING + "}" + EchellogramDisplayUnits.UNIT_ABBREV + " (order {0,number})";

	private static final String FORMAT_LIMITS_HO_LWL_LO_HWL =
  		   "low: {1,number," + EchellogramDisplayUnits.FORMAT_STRING + "}" + EchellogramDisplayUnits.UNIT_ABBREV + " ({0,number})" +
		", high: {3,number," + EchellogramDisplayUnits.FORMAT_STRING + "}" + EchellogramDisplayUnits.UNIT_ABBREV + " ({2,number})";


	// We actually need a reference here because several controls communicate
	// with it. Other than this, everything is anonymous.
	private EchellogramCanvas echCanvas; // this is final, but it no longer compiles in eclipse as final. so who cares.
	private JViewport viewport;

	public EchellogramControl(double wavelength, BHROSParams.EntranceFibre fibre) {
		echCanvas = new EchellogramCanvas(0.4334, fibre);
		
		echCanvas.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent arg0) {
				center();
			}
		});

		addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent arg0) {
				center();
			}
		});

		// Border and layout. It's a border layout with two components.
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		setLayout(new BorderLayout(0, 10));

		// Component in the top (center) is the echellogram canvas, in a viewport.
		add(new JPanel() {{
			setLayout(new BorderLayout());
			Border outer = BorderFactory.createEmptyBorder(0, 1, 0, 1);
			Border inner = BorderFactory.createLineBorder(COLOR_BORDER);
			setBorder(new CompoundBorder(outer, inner));
			add(viewport = new JViewport() {{
				setView(echCanvas);
			}}, BorderLayout.CENTER);
		}}, BorderLayout.CENTER);

		// Component in the bottom is itself a JPanel with some other things in it.
		add(new JPanel() {{

			// Another border layout with two children.
			setLayout(new BorderLayout(10, 0));

			// Child on the left (center) contains the echellogram view/config. It
			// is a JPanel with a bunch of children.
			add(new JPanel() {{

				// Titled border.
				Border title = BorderFactory.createLineBorder(COLOR_BORDER);
				Border outer = new TitledBorder(title, TEXT_BORDER_VIEW_CONFIG);
				Border inner = BorderFactory.createEmptyBorder(5, 5, 5, 5);
				setBorder(new CompoundBorder(outer, inner));

				// Layout is a gridbag that lays out labels on the left and controls on the right.
				setLayout(new GridBagLayout());
				GridBagConstraints gbc = new GridBagConstraints(
						0, -1, 1, 1, 10, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(1, 4, 1, 1), 0, 0);

				// Labels down the left side in column 0. These are trivial.
				gbc.gridy = 0; add(new JLabel(TEXT_LABEL_ZOOM, SwingConstants.RIGHT), gbc);
				gbc.gridy = 1; add(new JLabel(TEXT_LABEL_REFERENCE, SwingConstants.RIGHT), gbc);
				gbc.gridy = 3; add(new JLabel(TEXT_LABEL_LIMITS, SwingConstants.RIGHT), gbc);
				gbc.gridy = 4; add(new JLabel(TEXT_LABEL_ECHELLE, SwingConstants.RIGHT), gbc);

// RCN: Warnings are turned off for the moment (see below).
//
// 				gbc.gridy = 5; add(new JLabel(TEXT_LABEL_WARNINGS, SwingConstants.RIGHT), gbc);

				// The target lable looks and acts like a hyperlink. When the link is clicked,
				// the user can select a new wavelength. A side effect of using HTML text in
				// the label is that the geometry changes and we need different insets.
				Insets in = gbc.insets;
				gbc.insets = new Insets(in.top + 3, 4, in.bottom + 3, in.right + 1);
				gbc.gridy = 2; add(new JLabel() {{
					setText(HTML_LABEL_TARGET);
					setCursor(new Cursor(Cursor.HAND_CURSOR));
					addMouseListener(new MouseAdapter() {
						public void mouseClicked(MouseEvent me) {
							try {
								Object obj = JOptionPane.showInputDialog(
									EchellogramControl.this,
									"Enter a tracking wavelength in " + EchellogramDisplayUnits.UNIT_NAME_PLURAL + ".",
									"Tracking Wavelength",
									JOptionPane.QUESTION_MESSAGE,
									null, null,
									EchellogramDisplayUnits.formatMicrons(echCanvas.getWavelength())
								);
								if (obj != null) {
									double wavel = Double.parseDouble(obj.toString());
									echCanvas.setWavelength(EchellogramDisplayUnits.toMicrons(wavel));
								}
							} catch (NumberFormatException nfe) {
								JOptionPane.showMessageDialog(
										EchellogramControl.this,
										"Sorry, that wasn't a valid number.",
										"Error",
										JOptionPane.ERROR_MESSAGE
								);
							} catch (IllegalArgumentException iae) {
								JOptionPane.showMessageDialog(
										EchellogramControl.this,
										"The value you entered is out of range.\nYou might try adjusting the target position.",
										"Error",
										JOptionPane.ERROR_MESSAGE
								);
							}
						}
					});
				}}, gbc);
				gbc.insets = in;

				// Controls are in column 1 and need different constraints.
				gbc.gridx = 1;
				gbc.anchor = GridBagConstraints.WEST;
				gbc.weightx = 100;

				// First item is a slider that simply sets the scale on the echellogram canvas.
				// Let's go ahead and let this one stretch.
				gbc.fill = GridBagConstraints.HORIZONTAL;
				gbc.gridy = 0; add(new JSlider() {{
					setOrientation(JSlider.HORIZONTAL);
					setMinimum(ZOOM_MIN);
					setMaximum(ZOOM_MAX);
					setValue(echCanvas.getScale());
					addChangeListener(new ChangeListener() {
						public void stateChanged(ChangeEvent ce) {
							echCanvas.setScale(getValue());
						}
					});
				}}, gbc);
				gbc.fill = GridBagConstraints.NONE;

				// Second item is a combo box that determines which reference labels to show.
				gbc.gridy = 1; add(new JComboBox(TEXT_ITEMS_REFERENCE) {{
                    addActionListener(new ActionListener() {
						JFileChooser chooser = new JFileChooser();
						public void actionPerformed(ActionEvent ie) {
							String item = (String) getSelectedItem();
							if (item.equals(TEXT_ITEM_REFERENCE_ASTRO)) {
								echCanvas.setReferenceLines(REFLINES_ASTRO);
							} else if (item.equals(TEXT_ITEM_REFERENCE_CALIB)) {
								echCanvas.setReferenceLines(REFLINES_CALIB);
							} else if (item.equals(TEXT_ITEM_REFERENCE_USER)) {

								// Try to load a user file.
								if (chooser.showOpenDialog(echCanvas) == JFileChooser.APPROVE_OPTION) {
									try {
										File file = chooser.getSelectedFile();
										ReferenceLine[] userLines = ReferenceLine.readArray(new FileInputStream(file), true);
										echCanvas.setReferenceLines(userLines);
										return; // success!
									} catch (Exception e) {
										JOptionPane.showMessageDialog(null, "Could not load the selected user line file:\n" + e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
									}
								}

								// Clean up if user file load failed or was cancelled.
								setSelectedItem(TEXT_ITEM_REFERENCE_NONE);
								echCanvas.setReferenceLines(null);


							} else {
								echCanvas.setReferenceLines(null);
							}
						}
					});
				}}, gbc);

				// Third is tha target wavelength
				gbc.gridy = 2; add(new JLabel() {
					final MessageFormat mf = new MessageFormat(FORMAT_TARGET_ORD_WAVEL);
					{
						setForeground(Color.BLACK);
						setText();
						echCanvas.addPropertyChangeListener(new PropertyChangeListener() {
							public void propertyChange(PropertyChangeEvent pce) {
								String prop = pce.getPropertyName();
								if (prop.equals(EchellogramCanvas.PROP_WAVELENGTH) ||
									prop.equals(EchellogramCanvas.PROP_CENTRE_X_POS) ||
									prop.equals(EchellogramCanvas.PROP_CENTRE_Y_POS)) {
									setText();
								}
							}
						});
					}
					void setText() {
						setText(mf.format(new Object[] {
							new Integer(echCanvas.getOrder()),
							new Double(EchellogramDisplayUnits.fromMicrons(echCanvas.getWavelength())),
							new Double(echCanvas.getCentreXpos()),
							new Double(echCanvas.getCentreYpos()),
							echCanvas.getEntranceFibre().name(),
						}));
					}
				}, gbc);

				// Third is tha target wavelength
				gbc.gridy = 3; add(new JLabel() {
					final MessageFormat mf = new MessageFormat(FORMAT_LIMITS_HO_LWL_LO_HWL);
					{
						setForeground(Color.BLACK);
						setText();
						echCanvas.addPropertyChangeListener(new PropertyChangeListener() {
							public void propertyChange(PropertyChangeEvent pce) {
								String prop = pce.getPropertyName();
								if (prop.equals(EchellogramCanvas.PROP_WAVELENGTH) ||
									prop.equals(EchellogramCanvas.PROP_CENTRE_X_POS) ||
									prop.equals(EchellogramCanvas.PROP_CENTRE_Y_POS)) {
									setText();
								}
							}
						});
					}
					void setText() {
						setText(mf.format(new Object[] {
							new Integer(echCanvas.getMaxOrder()),
							new Double(EchellogramDisplayUnits.fromMicrons(echCanvas.getMinWavelength())),
							new Integer(echCanvas.getMinOrder()),
							new Double(EchellogramDisplayUnits.fromMicrons(echCanvas.getMaxWavelength())),
						}));
					}
				}, gbc);

				// Fourth is the Echelle configuration, which are updated when they change in the
				// echellogram canvas.
				gbc.gridy = 4; add(new JLabel() {
					final MessageFormat mf = new MessageFormat(FORMAT_ECHELLE_AZ_ALT_ANG);
					{
						setForeground(Color.BLACK);
						setText();
						echCanvas.addPropertyChangeListener(new PropertyChangeListener() {
							public void propertyChange(PropertyChangeEvent pce) {
								String prop = pce.getPropertyName();
								if (prop.equals(EchellogramCanvas.PROP_ECH_ALT) ||
									prop.equals(EchellogramCanvas.PROP_ECH_AZ) ||
									prop.equals(EchellogramCanvas.PROP_GONI_ANG)) {
									setText();
								}
							}
						});
					}
					void setText() {
						setText(mf.format(new Object[] {
								new Double(echCanvas.getEchAz()),
								new Double(echCanvas.getEchAlt()),
								new Double(echCanvas.getGoniAng())
							}));
					}
				}, gbc);

// RCN: warnings are not relevant at this time because only one chip is working; the inter-chip gap
// is not a factor. If they fix the red chip, this code needs to come back. Also need to turn on the
// label above.
//
//				// Fifth is the Warning line.
//				gbc.gridy = 5; add(new JLabel() {
//					{
//						setForeground(Color.BLACK);
//						setText();
//						echCanvas.addPropertyChangeListener(new PropertyChangeListener() {
//							public void propertyChange(PropertyChangeEvent pce) {
//								String prop = pce.getPropertyName();
//								if (prop.equals(EchellogramCanvas.PROP_ECH_ALT) ||
//									prop.equals(EchellogramCanvas.PROP_ECH_AZ) ||
//									prop.equals(EchellogramCanvas.PROP_GONI_ANG)) {
//									setText();
//								}
//							}
//						});
//					}
//					void setText() {
//						Set warnings = echCanvas.getGapOrders();
//						if (warnings.size() == 0) {
//							setText(TEXT_WARNINGS_NONE);
//						} else if (warnings.size() == 1) {
//							setText("Order " + warnings.iterator().next() + " crosses the inter-chip gap.");
//						} else {
//							StringBuffer buf = new StringBuffer("Orders crossing the inter-chip gap: ");
//							for (Iterator it = warnings.iterator(); it.hasNext(); ) {
//								buf.append(it.next());
//								if (it.hasNext())
//									buf.append(", ");
//							}
//							setText(buf.toString());
//						}
//					}
//				}, gbc);

			}}, BorderLayout.CENTER);

			// Second child in the lower panel is another panel that contains arrow controls
			// for manipulating the on-chip target position.
			add(new JPanel() {{

				// Border
				Border title = BorderFactory.createLineBorder(COLOR_BORDER);
				Border outer = new TitledBorder(title, TEXT_BORDER_TARGET_POSITION);
				Border inner = BorderFactory.createEmptyBorder(5, 5, 5, 5);
				setBorder(new CompoundBorder(outer, inner));

				// Layout right now is a GridLayout. This will probably change.
				setLayout(new GridLayout(3, 3));

				final double step = PIXELMM * 10; // 10 pixels

				// There are four buttons.
				final JButton posU = new ArrowButton(SwingConstants.NORTH) {{
					addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							echCanvas.setCentreYpos(echCanvas.getCentreYpos() + step);
						}
					});
				}};
				final JButton posD = new ArrowButton(SwingConstants.SOUTH) {{
					addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							echCanvas.setCentreYpos(echCanvas.getCentreYpos() - step);
						}
					});
				}};
				final JButton posL = new ArrowButton(SwingConstants.WEST) {{
					addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							echCanvas.setCentreXpos(echCanvas.getCentreXpos() - step);
						}
					});
				}};
				final JButton posR = new ArrowButton(SwingConstants.EAST) {{
					addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							echCanvas.setCentreXpos(echCanvas.getCentreXpos() + step);
						}
					});
				}};

				// And a new fifth button to center the display
				final JButton posCentre = new JButton() {
					{
						addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								echCanvas.setCentreXpos(0);
								echCanvas.setCentreYpos(HROSHardwareConstants.BLUE_YCENTRE);
							}
						});
						setToolTipText("Reset Position");
					}
					protected void paintComponent(Graphics g) {
						super.paintComponent(g);
						Dimension d = getSize();
						Graphics2D g2d = (Graphics2D) g;
						g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
						g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
						AffineTransform t = AffineTransform.getTranslateInstance(d.getWidth() / 2, d.getHeight() / 2);
						AffineTransform old = g2d.getTransform();
						g2d.transform(t);
						g2d.draw(new Arc2D.Double(-6, -6, 12, 12, 0, 360, Arc2D.OPEN));
						g2d.drawLine(-3, -3, 3, 3);
						g2d.drawLine(-3, 3, 3, -3);
						g2d.setTransform(old);
					}
				};

				add(new JPanel() {{
					setPreferredSize(new Dimension(30, 30));
				}});
				add(posU);
				add(new JLabel());
				add(posL);
				add(posCentre);
				add(posR);
				add(new JLabel());
				add(posD);
				add(new JButton("!") {
					{
						addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent arg0) {
								Object obj = JOptionPane.showInputDialog(
										EchellogramControl.this,
										"Enter x and y positions separated by a space.",
										"Target Position",
										JOptionPane.QUESTION_MESSAGE,
										null, null,
										echCanvas.getCentreXpos() + " " + echCanvas.getCentreYpos()
									);
									if (obj != null) {
										StringTokenizer tok = new StringTokenizer(obj.toString());
										echCanvas.setCentreXpos(Double.parseDouble(tok.nextToken()));
										echCanvas.setCentreYpos(Double.parseDouble(tok.nextToken()));
									}

							}
						});
						setToolTipText("Set x and y coordinates.");
					}
				});
			}}, BorderLayout.EAST);

		}}, BorderLayout.SOUTH);

	}

	private void center() {

		// Find the centerpoint of the viewport
		Dimension viewportSize = viewport.getSize();
		double viewportCenterX = viewportSize.getWidth() / 2;
		double viewportCenterY = viewportSize.getHeight() / 2;

		// Get the target position in pixels (on the canvas)
		Point2D centre = new Point2D.Double(echCanvas.getCentreXpos(), echCanvas.getCentreYpos());
		echCanvas.getTransform().transform(centre, centre);

		// And the centerpoint of the canvas
		double canvasCenterX = (int) centre.getX(); // echCanvas.getPreferredSize().getWidth() / 2;
		double canvasCenterY = (int) centre.getY(); // echCanvas.getPreferredSize().getHeight() / 2;

		// Find the offsets to center the target. Viewports don't like negative coordinates.
		int posX = Math.max(0, (int) (canvasCenterX - viewportCenterX));
		int posY = Math.max(0, (int) (canvasCenterY - viewportCenterY));

		// Do it. Need to revalidate the canvas because its container changed size.
		viewport.setViewPosition(new Point(posX, posY));
		echCanvas.revalidate();

	}

	public double getWavelength() {
		return echCanvas.getWavelength();
	}

	public void setWavelength(double wavelength) {
		echCanvas.setWavelength(wavelength);
	}

	public double getEchAz() {
		return echCanvas.getEchAz();
	}

	public double getEchAlt() {
		return echCanvas.getEchAlt();
	}

	public double getGoniAng() {
		return echCanvas.getGoniAng();
	}

	public void setCentreXPos(double xpos) {
		echCanvas.setCentreXpos(xpos);
	}

	public void setCentreYPos(double ypos) {
		echCanvas.setCentreYpos(ypos);
	}

	public double getCentreXPos() {
		return echCanvas.getCentreXpos();
	}

	public double getCentreYPos() {
		return echCanvas.getCentreYpos();
	}

	public void setEntranceFibre(EntranceFibre fibre) {
		echCanvas.setEntranceFibre(fibre);
	}

}

/**
 * This looks better than BasicArrowButton, I think.
 * @author rnorris
 */
class ArrowButton extends JButton {

	final GeneralPath path = new GeneralPath();
	{
		path.moveTo(-2.5f, 0);
		path.lineTo(-2.5f, -5);
		path.lineTo(2.5f, 0);
		path.lineTo(-2.5f, 5);
		path.closePath();
	}

	final int angle;

	ArrowButton(int direction) {
		switch (direction) {
			case SwingConstants.NORTH: angle = 270; break;
			case SwingConstants.EAST: angle = 0; break;
			case SwingConstants.SOUTH: angle = 90; break;
			case SwingConstants.WEST: angle = 180; break;
			default:
				throw new IllegalArgumentException("Direction unknown: " + direction);
		}

	}

	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		Dimension size = getSize();
		AffineTransform t = AffineTransform.getTranslateInstance(size.getWidth() / 2, size.getHeight() / 2);
		t.rotate(angle * (Math.PI * 2 / 360));
		g2d.fill(t.createTransformedShape(path));
	}

}




