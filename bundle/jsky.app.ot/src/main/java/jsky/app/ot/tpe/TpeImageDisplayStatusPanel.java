package jsky.app.ot.tpe;

import jsky.image.fits.codec.FITSImage;
import jsky.image.gui.ImageDisplayStatusPanel;
import jsky.image.ImageChangeEvent;

import javax.swing.BorderFactory;
import javax.swing.JTextField;
import java.awt.Color;

/**
 * Adds image origin and bandpass to the display.
 */
public class TpeImageDisplayStatusPanel extends ImageDisplayStatusPanel {

    // From http://gsss.stsci.edu/Catalogs/GSC/GSC2/GSC2codes.htm
    private static final String[] BANDPASS = new String[55];
    static {
        for(int i = 0; i < BANDPASS.length; i++) BANDPASS[i] = "";
        BANDPASS[0]="Jpg";
        BANDPASS[1]="V";
        BANDPASS[2]="-";
        BANDPASS[3]="B";
        BANDPASS[4]="V";
        BANDPASS[5]="Fpg";
        BANDPASS[6]="V495";
        BANDPASS[7]="O";
        BANDPASS[8]="E";
        BANDPASS[9]="Fpg";
        BANDPASS[10]="-";
        BANDPASS[11]="-";
        BANDPASS[12]="-";
        BANDPASS[13]="-";
        BANDPASS[14]="-";
        BANDPASS[16]="Vpg";
        BANDPASS[18]="Jpg";
        BANDPASS[19]="U";
        BANDPASS[20]="R";
        BANDPASS[21]="I";
        BANDPASS[22]="U";
        BANDPASS[23]="R";
        BANDPASS[24]="I";
        BANDPASS[35]="Fpg";
        BANDPASS[36]="Fpg";
        BANDPASS[37]="Npg";
        BANDPASS[38]="Npg";
        BANDPASS[39]="Jpg";
        BANDPASS[41]="BT";
        BANDPASS[42]="VT";
        BANDPASS[43]="-";
        BANDPASS[44]="-";
        BANDPASS[45]="-";
        BANDPASS[46]="-";
        BANDPASS[47]="J";
        BANDPASS[48]="H";
        BANDPASS[49]="K";
        BANDPASS[50]="u'";
        BANDPASS[51]="g'";
        BANDPASS[52]="r'";
        BANDPASS[53]="i'";
        BANDPASS[54]="z'";
    }


    // Text field used to display the origin of the image
    private JTextField originTextField;

    // Text field used to display the bandpass of the image
    private JTextField bandpassTextField;

    public TpeImageDisplayStatusPanel() {
        final Color bgColor = getBackground();
        originTextField = new JTextField(0);
        originTextField.setEditable(false);
        originTextField.setToolTipText("Image origin");
        originTextField.setHorizontalAlignment(JTextField.CENTER);
        originTextField.setBackground(bgColor);
        originTextField.setBorder(BorderFactory.createLoweredBevelBorder());
        add(originTextField);

        bandpassTextField = new JTextField(0);
        bandpassTextField.setEditable(false);
        bandpassTextField.setToolTipText("Image bandpass");
        bandpassTextField.setHorizontalAlignment(JTextField.CENTER);
        bandpassTextField.setBackground(bgColor);
        bandpassTextField.setBorder(BorderFactory.createLoweredBevelBorder());
        add(bandpassTextField);
    }


    protected void imageStateChanged(ImageChangeEvent e) {
        super.imageStateChanged(e);
        if (e.isNewImage()) {
            String origin = "";
            String bandpass = "";
            FITSImage fitsImage = imageDisplay.getFitsImage();
            if (fitsImage != null) {
                origin = fitsImage.getKeywordValue("ORIGIN", "");
                if ("2MASS".equals(origin)) {
                    bandpass = fitsImage.getKeywordValue("FILTER", "");
                    if (bandpass != null) {
                        bandpass = bandpass.toUpperCase();
                    }
                } else {
                    bandpass = lookupBandpass(fitsImage.getKeywordValue("BANDPASS", -1));
                }
            }
            originTextField.setText(origin);
            bandpassTextField.setText(bandpass);
        }
    }

    private String lookupBandpass(int band) {
        if (band >= 0 && band < BANDPASS.length) {
            return BANDPASS[band];
        }
        return "";
    }
}
