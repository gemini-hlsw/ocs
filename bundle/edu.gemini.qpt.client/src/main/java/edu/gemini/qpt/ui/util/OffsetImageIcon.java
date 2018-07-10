package edu.gemini.qpt.ui.util;

import java.awt.Component;
import java.awt.Graphics;
import java.net.URL;

import javax.swing.ImageIcon;

/**
 * An Icon that draws itself at a position offset from the passed coords. Useful for pushing
 * around icons that don't like up the way you want, without having to mess with the image
 * file. Yes, it's lazy.
 * @author rnorris
 */
@SuppressWarnings("serial")
public class OffsetImageIcon extends ImageIcon {

    private final int xoffset, yoffset;
    
    public OffsetImageIcon(URL url, int xoffset, int yoffset) {
        super(url);
        this.xoffset = xoffset;
        this.yoffset = yoffset;
    }
    
    @Override
    public synchronized void paintIcon(Component arg0, Graphics arg1, int arg2, int arg3) {
        super.paintIcon(arg0, arg1, arg2 + xoffset, arg3 + yoffset);
    }
    
}
