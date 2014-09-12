package edu.gemini.ui.workspace.impl;


import java.awt.Font;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.PopupFactory;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.metal.MetalLookAndFeel;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import com.jgoodies.looks.plastic.PlasticLookAndFeel;

import edu.gemini.ui.workspace.IShell;
import edu.gemini.ui.workspace.IShellAdvisor;
import edu.gemini.ui.workspace.IWorkspace;

public class Workspace extends WindowAdapter implements IWorkspace {

	private static final Logger LOGGER = Logger.getLogger(Workspace.class.getName());
	private final Map<JFrame, Shell> shells = new HashMap<JFrame, Shell>();
	private final BundleContext bc;
	private final Clipboard clipboard = new Clipboard("Workspace") {
		public synchronized void setContents(Transferable contents, ClipboardOwner owner) {
			super.setContents(contents, owner);
//			LOGGER.info("Clipboard contents set to " + (contents == null ? "null" : Arrays.toString(contents.getTransferDataFlavors())));
		};
	};
	
	public Workspace(BundleContext bc) {
		this.bc = bc;
	}
	
	public Clipboard getClipboard() {
		return clipboard;
	}
	
	public void open() {
		try {

			System.setProperty("apple.awt.showGrowBox", "false");		
			System.setProperty("apple.awt.antialiasing", "on");
			System.setProperty("apple.awt.textantialiasing", "on");
			
			UIManager.put("ClassLoader", Workspace.class.getClassLoader());
			
			// This is required for Java 1.5 on OS X.
			if (PopupFactory.getSharedInstance().getClass().getName().equals("apple.laf.ScreenPopupFactory"))
				PopupFactory.setSharedInstance(new PopupFactory());

//            PlasticLookAndFeel.setTabStyle(PlasticLookAndFeel.TAB_STYLE_METAL_VALUE);
			UIManager.setLookAndFeel(new PlasticLookAndFeel());


			// Make all the fonts smaller. You have to get the enumeration here .. the keyset 
			// is empty at this point for some reason. Only do this once per VM life
			if (System.getProperty(Workspace.class.getName() + ".fonts.shrunk") == null) {
				UIDefaults defaults = UIManager.getDefaults();
				Enumeration<Object> e = defaults.keys();
				while (e.hasMoreElements()) {
					Object k = e.nextElement();
					Object v = defaults.get(k);
					if (v instanceof Font) {
						Font f = (Font) v;
						f = f.deriveFont(f.getSize2D() - 1.0f);
						defaults.put(k, f);					
					}
				}
				System.setProperty(Workspace.class.getName() + ".fonts.shrunk", "true");
			}
						
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public void close() {		
		for (IShell s: shells.values())
			s.close();		
	}
	
	public Shell createShell(IShellAdvisor advisor) {			
		Shell shell = new Shell(this, advisor);
		shells.put(shell.getPeer(), shell);
		shell.getPeer().addWindowListener(this);
		return shell;		
	}

	public Collection<? extends IShell> getShells() {
		return Collections.unmodifiableCollection(shells.values());
	}

	@Override
	public void windowClosed(WindowEvent we) {			
		JFrame frame = (JFrame) we.getSource();			
		Shell shell = shells.remove(frame);			
		
		if (shells.isEmpty()) {
			if (bc == null) {
				System.exit(0);
//			} if (bc.getBundle().getState() != Bundle.ACTIVE || shell.getOwningBundle().getState() != Bundle.ACTIVE) {
//				LOGGER.info("Bundle states are changing. Not shutting down.");
			} else {		 
				try {
					LOGGER.info("All shells have closed. Shutting down...");
					bc.getBundle(0).stop();
				} catch (BundleException e) {
					LOGGER.log(Level.WARNING, "Trouble stopping system bundle.", e);
					LOGGER.warning("Shutting down violently.");
					System.exit(-1);
				}
			}
		}
		
	}		

}
 