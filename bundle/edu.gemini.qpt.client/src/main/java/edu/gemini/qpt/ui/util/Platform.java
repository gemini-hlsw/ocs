package edu.gemini.qpt.ui.util;

import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.InputMap;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.text.JTextComponent;

public class Platform {

    private static final Logger LOGGER = Logger.getLogger(Platform.class.getName());
    
    public static final boolean IS_MAC = System.getProperty("os.name").contains("Mac");
    public static final boolean IS_WINDOWS = System.getProperty("os.name").contains("Windows");
    
    public static final int MENU_ACTION_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(); 
    
    public static final int TOGGLE_ACTION_MASK = 
        IS_MAC ? InputEvent.META_DOWN_MASK : InputEvent.CTRL_DOWN_MASK;
    
    static {

        // Make sure all the editor keys are mapped to ACTION_MASK instead of just using
        // CTRL. This only changes things on the Mac.
        
        if (IS_MAC) {
            
            LOGGER.fine("Repairing edit component keymap for Mac OS X.");
            JTextComponent[] jtcs = new JTextComponent[] {
                new JTextArea(),
                new JTextField(),
            };
            
            int CTRL = InputEvent.CTRL_MASK | InputEvent.CTRL_DOWN_MASK;
            for (JTextComponent jtc: jtcs) {
                for (InputMap map = jtc.getInputMap(); map != null; map = map.getParent()) {
                    if (map.keys() != null) {
                        for (KeyStroke ks: map.keys()) {
                            int mod = ks.getModifiers();
                            if ((mod & CTRL) != 0) {
                                int newMod = (mod & ~CTRL) | MENU_ACTION_MASK;
                                KeyStroke newKs = KeyStroke.getKeyStroke(ks.getKeyCode(), newMod, ks.isOnKeyRelease());
                                Object action = map.get(ks);
                                map.remove(ks);
                                map.put(newKs, action);
                            }
                        }
                    }
                }
            }
            
        }            
        
    }
    
    /*
     * Derived from http://www.javaworld.com/javaworld/javatips/jw-javatip66.html
     */
    public static boolean displayURL(String url) throws IOException {
        try {
            if (IS_WINDOWS) {
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
            } else if (IS_MAC) {
                Runtime.getRuntime().exec("/usr/bin/open " + url);                
            } else {
                Process p = Runtime.getRuntime().exec("firefox -remote openURL(" + url + ")");
                try {
                    int exitCode = p.waitFor();
                    if (exitCode != 0)
                        Runtime.getRuntime().exec("firefox " + url);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Operation was interrupted.");
                }                
            }
            return false;
        } catch (IOException ioe) {
            LOGGER.log(Level.WARNING, "Trouble opening " + url, ioe);
            throw ioe;
        }
    }
    
    public static boolean displayURL(URL url) throws IOException {
        return displayURL(url.toString());
    }

    
//    public static void main(String[] args) {
//        
//        int mod = InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK;
//        
//        System.out.println(Integer.toString(mod, 2));
//        
//        mod = (mod & ~InputEvent.CTRL_DOWN_MASK) | ACTION_MASK;
//        
//        System.out.println(Integer.toString(mod, 2));
//        
//    }

}
