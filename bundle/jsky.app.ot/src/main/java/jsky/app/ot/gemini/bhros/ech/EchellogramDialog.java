package jsky.app.ot.gemini.bhros.ech;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import edu.gemini.spModel.gemini.bhros.BHROSParams;
import edu.gemini.spModel.gemini.bhros.BHROSParams.EntranceFibre;
import edu.gemini.spModel.gemini.bhros.ech.HROSHardwareConstants;

//import jsky.app.ot.OT;

/**
 * Popup dialog to configure the bHROS Echelle settings. Here is how to use it:
 * <ol>
 *  <li>Construct the dialog, passing the parent window and a modal value. Because live property changes
 *      are not published, modal should probably be true.
 *  <li>If you know the starting wavelength and centreX/Y positions, set them. Otherwise it will use
 *      default values which will work ok for testing out the controls.
 *  <li>Show the dialog.
 *  <li>When it returns, you can get the new versions of the target wavelength and centreX/Y. You can also
 *      get the echelle/goni settings, but these are calculated automatically on the instrument model
 *      object so you probably only need them for testing. Note that if the user hits cancel, these values
 *      will be unchanged.
 * </ol>
 */
public class EchellogramDialog extends JDialog {

	private final EchellogramControl ech;

	// Keep copies of this information, in case the user hits cancel
	// instead of Ok. 
	private double wavelength;
	private double echAz;
	private double echAlt;
	private double goniAng;
	private double centreXpos;
	private double centreYpos;
		
	public EchellogramDialog(Frame parent, boolean modal, double wavelengthMicrons, BHROSParams.EntranceFibre fibre) throws HeadlessException {
		super(parent, "Echelle Spectrograph Configuration", modal);
		ech = new EchellogramControl(wavelengthMicrons, fibre);
		sync();
		getContentPane().add(ech, BorderLayout.CENTER);
		getContentPane().add(new JPanel() {{
			add(new JButton("Cancel") {{
				addActionListener(new ActionListener() {				
					public void actionPerformed(ActionEvent ae) {
						setWavelength(wavelength);
						sync();
						EchellogramDialog.this.hide();
					}
				});
			}});
			add(new JButton("Ok") {{
				addActionListener(new ActionListener() {				
					public void actionPerformed(ActionEvent ae) {
						sync();
						EchellogramDialog.this.hide();
					}
				});
			}});
		}}, BorderLayout.SOUTH);
		pack();
	}	

	/**
	 * Commits the data from the widget into the dialog members. This is called
	 * if you click Ok or set a field on the dialog via a setter. If you click
	 * cancel, these fields are not modified.
	 */
	private void sync() {
		wavelength = ech.getWavelength();
		echAz = ech.getEchAz();
		echAlt = ech.getEchAlt();
		goniAng = ech.getGoniAng();
		centreXpos = ech.getCentreXPos();
		centreYpos = ech.getCentreYPos();
	}

	/**
	 * Returns the selected wavelength in microns.
	 */
	public double getWavelength() {
		return wavelength;
	}

	/**
	 * Sets the wavelength (in microns).
	 */
	public void setWavelength(double wavelength) {
		ech.setWavelength(wavelength);
		sync();
	}
	
	public double getEchAlt() {
		return echAlt;
	}
	
	public double getEchAz() {
		return echAz;
	}

	public double getGoniAng() {
		return goniAng;
	}
	
	public void setCentreXPos(double xpos) {
		ech.setCentreXPos(xpos);
		sync();
	}
	
	public void setCentreYPos(double ypos) {
		ech.setCentreYPos(ypos);
		sync();
	}
	
	public double getCentreXPos() {
		return centreXpos;
	}	

	public double getCentreYPos() {
		return centreYpos;
	}	

	public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {		
		UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
		EchellogramDialog ed = new EchellogramDialog(null, true, 0.4334, BHROSParams.EntranceFibre.OBJECT_ONLY);

		String help = "Usage: echb [option value ...]" +
			"\n   -microns\ttarget wavelength in microns, default is " + ed.getWavelength() + 
			"\n   -" + EchellogramDisplayUnits.UNIT_NAME_PLURAL + "\ttarget wavelength in " + EchellogramDisplayUnits.UNIT_NAME_PLURAL + ", default is " + EchellogramDisplayUnits.formatMicrons(ed.getWavelength()) +
			"\n   -x\t\ttarget x position in mm, default is " + ed.getCentreXPos() +
			"\n   -y\t\ttarget y position in mm, default is " + ed.getCentreYPos() + 
			"\n   -f\t\tentrance fibre; ObjectOnly (default) or ObjectSky";
		
		try {
			for (int i = 0; i < args.length; i++) {
				String arg = args[i];
				if (arg.equals("-" + EchellogramDisplayUnits.UNIT_NAME_PLURAL)) {
					ed.setWavelength(EchellogramDisplayUnits.toMicrons(Double.parseDouble(args[++i])));
				} else if (arg.equals("-microns")) {
					ed.setWavelength(Double.parseDouble(args[++i]));					
				} else if (arg.equals("-x")) {
					ed.setCentreXPos(Double.parseDouble(args[++i]));					
				} else if (arg.equals("-y")) {
					ed.setCentreYPos(Double.parseDouble(args[++i]));
				} else if (arg.equals("-f")) {
					String fibre = args[++i];
					if ("ObjectOnly".equalsIgnoreCase(fibre)) {
						ed.setEntranceFibre(BHROSParams.EntranceFibre.OBJECT_ONLY);
					} else if ("ObjectSky".equalsIgnoreCase(fibre)) {
						ed.setEntranceFibre(BHROSParams.EntranceFibre.OBJECT_SKY);
					} else {
						throw new IllegalArgumentException("Unknown entrance fibre: " + fibre);
					}
				} else {
					throw new IllegalArgumentException();
				}
			}	
			ed.show();
		} catch (Exception e) {
			System.out.println(help);
		}
		System.exit(0);
	}

	public void setEntranceFibre(EntranceFibre fibre) {
		ech.setEntranceFibre(fibre);
		sync();
	}
	
}


